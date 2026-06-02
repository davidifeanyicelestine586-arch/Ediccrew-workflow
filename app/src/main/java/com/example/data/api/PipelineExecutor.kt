package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.StepExecutionOutput
import com.example.data.model.WorkflowStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class ExecutionState {
    object Idle : ExecutionState()
    data class Running(val currentStepIndex: Int, val totalSteps: Int, val stepName: String) : ExecutionState()
    data class StepFinished(val stepIndex: Int, val stepName: String, val output: String) : ExecutionState()
    data class Success(val finalOutputs: List<StepExecutionOutput>) : ExecutionState()
    data class Error(val message: String) : ExecutionState()
}

object PipelineExecutor {

    private fun replacePlaceholders(template: String, values: Map<String, String>): String {
        var result = template
        for ((key, value) in values) {
            result = result.replace("{$key}", value, ignoreCase = true)
            result = result.replace("{ $key }", value, ignoreCase = true)
        }
        return result
    }

    fun execute(
        steps: List<WorkflowStep>,
        userInputs: Map<String, String>,
        model: String = "gemini-3.5-flash"
    ): Flow<ExecutionState> = flow {
        if (steps.isEmpty()) {
            emit(ExecutionState.Error("No pipeline steps found to execute."))
            return@flow
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            emit(ExecutionState.Error("Gemini API key is unconfigured. Please configure your key in the AI Studio Secrets panel."))
            return@flow
        }

        // Keep a copy of execution variables
        val dynamicVariables = userInputs.toMutableMap()
        val finalOutputs = mutableListOf<StepExecutionOutput>()

        val totalSteps = steps.size
        for (index in 0 until totalSteps) {
            val step = steps[index]
            emit(ExecutionState.Running(index, totalSteps, step.stepName))

            // Replace current templates with variables
            val processedPrompt = replacePlaceholders(step.promptTemplate, dynamicVariables)
            val processedSystemInstruction = replacePlaceholders(step.systemInstruction, dynamicVariables)

            Log.d("PipelineExecutor", "Running Step ${index + 1}: ${step.stepName}")
            Log.d("PipelineExecutor", "Prompt Used: $processedPrompt")

            try {
                val requestModel = GeminiRequest(
                    contents = listOf(
                        Content(parts = listOf(ContentPart(text = processedPrompt)))
                    ),
                    systemInstruction = Content(parts = listOf(ContentPart(text = processedSystemInstruction))),
                    generationConfig = GenerationConfig(temperature = 0.7f)
                )

                val response = RetrofitClient.service.generateContent(
                    model = model,
                    apiKey = apiKey,
                    request = requestModel
                )

                val textOutput = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("Empty response candidate returned from model.")

                // Save variable for subsequent steps
                val variableKey = "step${index + 1}_output"
                dynamicVariables[variableKey] = textOutput

                // Also populate friendly name key (lowercased, spaces replaced by underscores)
                val friendlyKey = step.stepName.lowercase()
                    .replace(Regex("[^a-z0-9_]"), "_") + "_output"
                dynamicVariables[friendlyKey] = textOutput

                val resultOutput = StepExecutionOutput(
                    stepName = step.stepName,
                    promptUsed = processedPrompt,
                    rawOutput = textOutput
                )
                finalOutputs.add(resultOutput)

                emit(ExecutionState.StepFinished(index, step.stepName, textOutput))

            } catch (e: Exception) {
                Log.e("PipelineExecutor", "Execution failed on step ${step.stepName}: ${e.message}", e)
                emit(ExecutionState.Error("Failed step '${step.stepName}': ${e.localizedMessage ?: e.message}"))
                return@flow
            }
        }

        emit(ExecutionState.Success(finalOutputs))
    }
}
