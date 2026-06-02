package com.example.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@JsonClass(generateAdapter = true)
data class WorkflowStep(
    val stepName: String,
    val promptTemplate: String,
    val systemInstruction: String = "You are an expert creative advisor. Help the creator refine their content ideas."
)

@JsonClass(generateAdapter = true)
data class StepExecutionOutput(
    val stepName: String,
    val promptUsed: String,
    val rawOutput: String
)

object JsonUtils {
    val moshi: Moshi = Moshi.Builder().build()

    fun stepsToString(steps: List<WorkflowStep>): String {
        val type = Types.newParameterizedType(List::class.java, WorkflowStep::class.java)
        return moshi.adapter<List<WorkflowStep>>(type).toJson(steps)
    }

    fun stringToSteps(json: String?): List<WorkflowStep> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, WorkflowStep::class.java)
        return moshi.adapter<List<WorkflowStep>>(type).fromJson(json) ?: emptyList()
    }

    fun mapToString(map: Map<String, String>): String {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        return moshi.adapter<Map<String, String>>(type).toJson(map)
    }

    fun stringToMap(json: String?): Map<String, String> {
        if (json.isNullOrEmpty()) return emptyMap()
        val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        return moshi.adapter<Map<String, String>>(type).fromJson(json) ?: emptyMap()
    }

    fun outputsToString(outputs: List<StepExecutionOutput>): String {
        val type = Types.newParameterizedType(List::class.java, StepExecutionOutput::class.java)
        return moshi.adapter<List<StepExecutionOutput>>(type).toJson(outputs)
    }

    fun stringToOutputs(json: String?): List<StepExecutionOutput> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, StepExecutionOutput::class.java)
        return moshi.adapter<List<StepExecutionOutput>>(type).fromJson(json) ?: emptyList()
    }
}
