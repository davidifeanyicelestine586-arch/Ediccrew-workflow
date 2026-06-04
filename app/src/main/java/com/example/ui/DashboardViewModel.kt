package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.ExecutionState
import com.example.data.api.PipelineExecutor
import com.example.data.database.AppDatabase
import com.example.data.database.ExecutionHistoryEntity
import com.example.data.database.WorkflowEntity
import com.example.data.database.WorkflowSnapshotEntity
import com.example.data.model.JsonUtils
import com.example.data.model.StepExecutionOutput
import com.example.data.model.WorkflowStep
import com.example.data.repository.WorkflowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val repository: WorkflowRepository) : ViewModel() {

    // All workflows available in the database
    val workflows: StateFlow<List<WorkflowEntity>> = repository.allWorkflows
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selected workflow ID
    private val _selectedWorkflowId = MutableStateFlow<Int?>(null)
    val selectedWorkflowId: StateFlow<Int?> = _selectedWorkflowId.asStateFlow()

    // Loaded workflow (source of truth from db)
    private val _activeWorkflowDb = MutableStateFlow<WorkflowEntity?>(null)
    val activeWorkflowDb: StateFlow<WorkflowEntity?> = _activeWorkflowDb.asStateFlow()

    // Mutable fields for editing
    private val _activeName = MutableStateFlow("")
    val activeName: StateFlow<String> = _activeName.asStateFlow()

    private val _activeDescription = MutableStateFlow("")
    val activeDescription: StateFlow<String> = _activeDescription.asStateFlow()

    private val _activeCategory = MutableStateFlow("Video Content")
    val activeCategory: StateFlow<String> = _activeCategory.asStateFlow()

    private val _activeSteps = MutableStateFlow<List<WorkflowStep>>(emptyList())
    val activeSteps: StateFlow<List<WorkflowStep>> = _activeSteps.asStateFlow()

    // Check if local fields differ from DB
    val hasUnsavedChanges: StateFlow<Boolean> = combine(
        _activeWorkflowDb,
        _activeName,
        _activeDescription,
        _activeCategory,
        _activeSteps
    ) { db, name, desc, cat, steps ->
        if (db == null) {
            // New unsaved workflow, only unsaved if we modified from empty defaults
            name.isNotEmpty() || desc.isNotEmpty() || steps.isNotEmpty()
        } else {
            val dbSteps = JsonUtils.stringToSteps(db.stepsListJson)
            db.name != name || db.description != desc || db.category != cat || dbSteps != steps
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Snapshot checklist of active workflow
    val snapshots: StateFlow<List<WorkflowSnapshotEntity>> = _selectedWorkflowId
        .flatMapLatest { id ->
            if (id != null) repository.getSnapshotsForWorkflow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Direct histories of selected workflow
    val executionHistories: StateFlow<List<ExecutionHistoryEntity>> = _selectedWorkflowId
        .flatMapLatest { id ->
            if (id != null) repository.getExecutionHistoriesForWorkflow(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All global inputs fields matching active prompt configuration
    private val _runtimeInputs = MutableStateFlow<Map<String, String>>(emptyMap())
    val runtimeInputs: StateFlow<Map<String, String>> = _runtimeInputs.asStateFlow()

    // Auto-extracted placeholders
    val extractedVariables: StateFlow<List<String>> = _activeSteps
        .combine(_runtimeInputs) { steps, currentInputs ->
            val regex = Regex("\\{\\{([A-Za-z0-9_\\s]+)\\}\\}|\\{([A-Za-z0-9_\\s]+)\\}")
            val vars = mutableSetOf<String>()
            for (step in steps) {
                regex.findAll(step.promptTemplate).forEach { result ->
                    val rawKey = if (result.groupValues[1].isNotEmpty()) result.groupValues[1] else result.groupValues[2]
                    val key = rawKey.trim().lowercase()
                    // Filter out outputs from prior steps: e.g. "step1_output", or "hook_discovery_engine_output"
                    // Ignore elements starting with 'step' or ending with '_output'
                    if (!key.startsWith("step") && !key.endsWith("_output") && key.isNotEmpty()) {
                        vars.add(key)
                    }
                }
            }
            vars.toList().sorted()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Model selection state ("gemini-3.5-flash" vs "gemini-3.1-pro-preview")
    private val _selectedModel = MutableStateFlow("gemini-3.5-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    // Execution progression state
    private val _executionState = MutableStateFlow<ExecutionState>(ExecutionState.Idle)
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    // Dialog & overlay details state
    private val _selectedHistoryItem = MutableStateFlow<ExecutionHistoryEntity?>(null)
    val selectedHistoryItem: StateFlow<ExecutionHistoryEntity?> = _selectedHistoryItem.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfDbEmpty()
            // Auto select the first workflow if available
            val list = repository.allWorkflows.firstOrNull()
            if (!list.isNullOrEmpty()) {
                selectWorkflow(list.first().id)
            }
        }
    }

    fun selectModel(model: String) {
        _selectedModel.value = model
    }

    fun updateActiveName(name: String) {
        _activeName.value = name
    }

    fun updateActiveDescription(desc: String) {
        _activeDescription.value = desc
    }

    fun updateActiveCategory(category: String) {
        _activeCategory.value = category
    }

    fun selectWorkflow(id: Int) {
        viewModelScope.launch {
            val workflow = repository.getWorkflowById(id)
            if (workflow != null) {
                _selectedWorkflowId.value = id
                _activeWorkflowDb.value = workflow
                _activeName.value = workflow.name
                _activeDescription.value = workflow.description
                _activeCategory.value = workflow.category
                _activeSteps.value = JsonUtils.stringToSteps(workflow.stepsListJson)
                _executionState.value = ExecutionState.Idle
            }
        }
    }

    fun createNewWorkflow() {
        _selectedWorkflowId.value = null
        _activeWorkflowDb.value = null
        _activeName.value = "New Custom Workflow"
        _activeDescription.value = "A custom-built sequential prompt sequence."
        _activeCategory.value = "Marketing"
        _activeSteps.value = listOf(
            WorkflowStep("Step 1: Ideation", "Brainstorm three angles for our {topic} on medium {platform}.", "Keep instructions minimal."),
            WorkflowStep("Step 2: Production", "Refine the best angle from Step 1: {step1_output} into a high-converting tagline.", "Aim for a punchy output.")
        )
        _executionState.value = ExecutionState.Idle
    }

    fun saveCurrentWorkflow() {
        viewModelScope.launch {
            val name = _activeName.value.ifBlank { "Untitled Workflow" }
            val desc = _activeDescription.value
            val category = _activeCategory.value
            val stepsJson = JsonUtils.stepsToString(_activeSteps.value)

            val currentId = _selectedWorkflowId.value
            if (currentId == null) {
                // Insert new
                val newEntity = WorkflowEntity(
                    name = name,
                    description = desc,
                    category = category,
                    stepsListJson = stepsJson
                )
                val newId = repository.insertWorkflow(newEntity)
                selectWorkflow(newId.toInt())
            } else {
                // Update existing
                val updated = WorkflowEntity(
                    id = currentId,
                    name = name,
                    description = desc,
                    category = category,
                    createdAt = _activeWorkflowDb.value?.createdAt ?: System.currentTimeMillis(),
                    stepsListJson = stepsJson
                )
                repository.updateWorkflow(updated)
                _activeWorkflowDb.value = updated
            }
        }
    }

    fun deleteCurrentWorkflow() {
        val id = _selectedWorkflowId.value ?: return
        viewModelScope.launch {
            repository.deleteWorkflow(id)
            _selectedWorkflowId.value = null
            _activeWorkflowDb.value = null
            val list = workflows.value
            if (list.isNotEmpty()) {
                val nextToSelect = list.firstOrNull { it.id != id } ?: list.first()
                selectWorkflow(nextToSelect.id)
            } else {
                createNewWorkflow()
            }
        }
    }

    // Step modification utils
    fun updateStep(index: Int, updated: WorkflowStep) {
        val current = _activeSteps.value.toMutableList()
        if (index in current.indices) {
            current[index] = updated
            _activeSteps.value = current
        }
    }

    fun addStep() {
        val current = _activeSteps.value.toMutableList()
        val nextIdx = current.size + 1
        current.add(WorkflowStep("Step $nextIdx: New Operation", "Use context step ${nextIdx - 1}: {step${nextIdx - 1}_output} to generate additional material.", "Be creative."))
        _activeSteps.value = current
    }

    fun removeStep(index: Int) {
        val current = _activeSteps.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _activeSteps.value = current
        }
    }

    fun updateRuntimeInput(key: String, value: String) {
        val current = _runtimeInputs.value.toMutableMap()
        current[key] = value
        _runtimeInputs.value = current
    }

    fun resetExecutionState() {
        _executionState.value = ExecutionState.Idle
    }

    // Trigger sequential execution engine
    fun runPipeline() {
        val steps = _activeSteps.value
        val inputs = _runtimeInputs.value
        val model = _selectedModel.value

        viewModelScope.launch {
            _executionState.value = ExecutionState.Idle
            PipelineExecutor.execute(steps, inputs, model).collect { state ->
                _executionState.value = state

                // If success or error, write history run inside database
                if (state is ExecutionState.Success) {
                    val wId = _selectedWorkflowId.value ?: 0
                    val wName = _activeName.value
                    val historyRecord = ExecutionHistoryEntity(
                        workflowId = wId,
                        workflowName = wName,
                        inputsJson = JsonUtils.mapToString(inputs),
                        outputsJson = JsonUtils.outputsToString(state.finalOutputs),
                        success = true
                    )
                    repository.insertExecutionHistory(historyRecord)
                } else if (state is ExecutionState.Error) {
                    val wId = _selectedWorkflowId.value ?: 0
                    val wName = _activeName.value

                    // Map empty list or partial runs
                    val historyRecord = ExecutionHistoryEntity(
                        workflowId = wId,
                        workflowName = wName,
                        inputsJson = JsonUtils.mapToString(inputs),
                        outputsJson = JsonUtils.outputsToString(emptyList()),
                        success = false,
                        errorMessage = state.message
                    )
                    repository.insertExecutionHistory(historyRecord)
                }
            }
        }
    }

    // Version snapshots
    fun createSnapshot(label: String) {
        val wId = _selectedWorkflowId.value ?: return
        val currentSteps = _activeSteps.value
        val cleanLabel = label.ifBlank { "Snapshot ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())}" }

        viewModelScope.launch {
            repository.createSnapshot(wId, cleanLabel, currentSteps)
        }
    }

    // Rollback active pipeline config to restore from snapshot
    fun restoreSnapshot(snapshot: WorkflowSnapshotEntity) {
        val steps = JsonUtils.stringToSteps(snapshot.stepsListJson)
        _activeSteps.value = steps
        saveCurrentWorkflow()
    }

    fun deleteSnapshot(snapshotId: Int) {
        viewModelScope.launch {
            repository.deleteSnapshot(snapshotId)
        }
    }

    fun showHistoryDetails(item: ExecutionHistoryEntity?) {
        _selectedHistoryItem.value = item
    }

    fun deleteHistoryRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteExecutionHistory(id)
        }
    }
}

class DashboardViewModelFactory(private val repository: WorkflowRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
