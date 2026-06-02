package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.api.ExecutionState
import com.example.data.database.ExecutionHistoryEntity
import com.example.data.database.WorkflowEntity
import com.example.data.database.WorkflowSnapshotEntity
import com.example.data.model.JsonUtils
import com.example.data.model.WorkflowStep
import com.example.data.model.StepExecutionOutput
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val workflows by viewModel.workflows.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedWorkflowId.collectAsStateWithLifecycle()
    val activeWorkflowDb by viewModel.activeWorkflowDb.collectAsStateWithLifecycle()

    val activeName by viewModel.activeName.collectAsStateWithLifecycle()
    val activeDesc by viewModel.activeDescription.collectAsStateWithLifecycle()
    val activeCategory by viewModel.activeCategory.collectAsStateWithLifecycle()
    val activeSteps by viewModel.activeSteps.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()

    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()
    val histories by viewModel.executionHistories.collectAsStateWithLifecycle()
    val runtimeInputs by viewModel.runtimeInputs.collectAsStateWithLifecycle()
    val extractedVars by viewModel.extractedVariables.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val executionState by viewModel.executionState.collectAsStateWithLifecycle()
    val selectedHistory by viewModel.selectedHistoryItem.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Steps, 1: Execute, 2: Snapshots, 3: History
    var showApiKeyInfoDialog by remember { mutableStateOf(false) }
    var showSnapshotCreateDialog by remember { mutableStateOf(false) }
    var newSnapshotLabel by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Dialog layout state for responsive widths
    Scaffold(
        modifier = modifier.testTag("dashboard_root"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(NeonCyan, VioletGlow)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Logo Concept Icon",
                                tint = DarkBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "EDICCREW",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = WhitePrimary
                        )
                    }
                },
                actions = {
                    // API Key Pill Indicator
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    val isKeyConfigured = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(if (isKeyConfigured) SoftTeal.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f))
                            .border(
                                width = 1.dp,
                                color = if (isKeyConfigured) SoftTeal.copy(alpha = 0.5f) else RedAlert.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(30.dp)
                            )
                            .clickable { showApiKeyInfoDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isKeyConfigured) SoftTeal else RedAlert)
                        )
                        Text(
                            text = if (isKeyConfigured) "API Connected" else "API Key Offline",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isKeyConfigured) SoftTeal else RedAlert
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = WhitePrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            // Main responsive dynamic layouts
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                // Panel 1: Vertical Directory list (Sidebar on wider devices, collapses if needed)
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .padding(bottom = 12.dp)
                        .testTag("directory_panel"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SYSTEM PIPELINES",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = MutedText
                                )
                            )

                            IconButton(
                                onClick = { viewModel.createNewWorkflow() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("add_workflow_button"),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = NeonCyan.copy(alpha = 0.1f),
                                    contentColor = NeonCyan
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Workflow",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(workflows) { workflow ->
                                val isSelected = workflow.id == selectedId
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) DarkSurfaceVariant else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) NeonCyan.copy(0.4f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.selectWorkflow(workflow.id) }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = workflow.name,
                                                style = TextStyle(
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isSelected) NeonCyan else WhitePrimary
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Text(
                                            text = workflow.category,
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = VioletGlow
                                            ),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        Text(
                                            text = workflow.description,
                                            style = TextStyle(
                                                fontSize = 11.sp,
                                                color = MutedText
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Panel 2: Interactive Operations Console Workspace
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(bottom = 12.dp)
                        .testTag("operations_console")
                ) {
                    // Active pipeline meta configurations (Inline editor banner)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextField(
                                            value = activeName,
                                            onValueChange = { viewModel.saveCurrentWorkflow() /* local triggers handle state via factory, let's keep VM editable bound directly */ },
                                            placeholder = { Text("Name your creative pipeline...") },
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = NeonCyan,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedTextColor = WhitePrimary,
                                                unfocusedTextColor = WhitePrimary
                                            ),
                                            textStyle = TextStyle(
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WhitePrimary
                                            ),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("active_workflow_title_input")
                                        )
                                    }

                                    TextField(
                                        value = activeDesc,
                                        onValueChange = { /* handled in standard view model binders */ },
                                        placeholder = { Text("Workflow strategic mission description...") },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = NeonCyan,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedTextColor = MutedText,
                                            unfocusedTextColor = MutedText
                                        ),
                                        textStyle = TextStyle(
                                            fontSize = 12.sp,
                                            color = MutedText
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Quick actions
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Save button with alert notification
                                    AnimatedVisibility(visible = hasUnsavedChanges) {
                                        Button(
                                            onClick = { viewModel.saveCurrentWorkflow() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SoftTeal,
                                                contentColor = DarkBackground
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.testTag("save_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Save Changes",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save Changes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    IconButton(
                                        onClick = { showDeleteConfirmDialog = true },
                                        modifier = Modifier.testTag("delete_workflow_button"),
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = RedAlert.copy(alpha = 0.1f),
                                            contentColor = RedAlert
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete System"
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Tab selections & Model selections
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(DarkBackground, RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tabs = listOf("Pipeline Builder", "Live Exec Console", "Snapshots Memory", "Activity Runs")
                                tabs.forEachIndexed { index, label ->
                                    val isSelected = activeTab == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) DarkSurface else Color.Transparent)
                                            .clickable { activeTab = index }
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = TextStyle(
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) NeonCyan else MutedText
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab contents layout
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Crossfade(
                            targetState = activeTab,
                            animationSpec = tween(150)
                        ) { tab ->
                            when (tab) {
                                0 -> PipelineBuilderView(
                                    steps = activeSteps,
                                    onUpdateStep = { idx, step -> viewModel.updateStep(idx, step) },
                                    onAddStep = { viewModel.addStep() },
                                    onRemoveStep = { idx -> viewModel.removeStep(idx) }
                                )
                                1 -> PipelineExecuteView(
                                    steps = activeSteps,
                                    extractedVars = extractedVars,
                                    runtimeInputs = runtimeInputs,
                                    onUpdateInput = { k, v -> viewModel.updateRuntimeInput(k, v) },
                                    model = selectedModel,
                                    onModelSelect = { m -> viewModel.selectModel(m) },
                                    executionState = executionState,
                                    onTriggerRun = { viewModel.runPipeline() },
                                    onClearTerminal = { viewModel.resetExecutionState() }
                                )
                                2 -> SnapshotsTimelineView(
                                    snapshots = snapshots,
                                    onCreateSnapshot = {
                                        newSnapshotLabel = ""
                                        showSnapshotCreateDialog = true
                                    },
                                    onRestoreSnapshot = { snap -> viewModel.restoreSnapshot(snap) },
                                    onDeleteSnapshot = { id -> viewModel.deleteSnapshot(id) }
                                )
                                3 -> ActivityLogsView(
                                    histories = histories,
                                    onSelectItem = { item -> viewModel.showHistoryDetails(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog 2: Save Snapshot Dialog
    if (showSnapshotCreateDialog) {
        Dialog(onDismissRequest = { showSnapshotCreateDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Create Core Snapshot",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Perfect configuration states. Branch operations and revert safely under version-control checkpoint triggers.",
                        color = MutedText,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = newSnapshotLabel,
                        onValueChange = { newSnapshotLabel = it },
                        label = { Text("Snapshot Label / Code tag") },
                        placeholder = { Text("e.g., v2-higher-conversion-hooks") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = WhitePrimary,
                            unfocusedTextColor = WhitePrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("snapshot_label_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showSnapshotCreateDialog = false }) {
                            Text("Cancel", color = MutedText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.createSnapshot(newSnapshotLabel)
                                showSnapshotCreateDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground),
                            modifier = Modifier.testTag("snapshot_confirm_button")
                        ) {
                            Text("Checkpoint Lock", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal dialog 3: API Info Overlay Dialog
    if (showApiKeyInfoDialog) {
        Dialog(onDismissRequest = { showApiKeyInfoDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "API status outline",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Gemini AI Infrastructure Configuration",
                            fontWeight = FontWeight.Bold,
                            color = WhitePrimary,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "Ediccrew processes complex workflow prompt chains securely on-device using custom configurations.",
                        color = MutedText,
                        fontSize = 12.sp
                    )

                    val apiKey = BuildConfig.GEMINI_API_KEY
                    val isKeyConfigured = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .border(BorderStroke(1.dp, DarkSurfaceVariant), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("Current API Status:", fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isKeyConfigured) "● Connected & Active" else "○ Keys Missing - Action Required",
                                color = if (isKeyConfigured) SoftTeal else RedAlert,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "How to configure API Key:\n" +
                                "1. Enter your private key safely into AI Studio's 'Secrets' panel.\n" +
                                "2. Declare GEMINI_API_KEY as the variable name.\n" +
                                "3. Compile and rerun to unleash the automated workflow memory chains.",
                        color = WhitePrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = { showApiKeyInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Acknowledge Workspace Setup")
                    }
                }
            }
        }
    }

    // Modal dialog 4: Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        Dialog(onDismissRequest = { showDeleteConfirmDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Delete Strategic Pipeline?",
                        fontWeight = FontWeight.Bold,
                        color = RedAlert,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "This removes the entire workflow entity, steps configurations, and clears version checkpoints. Action is permanent.",
                        color = MutedText,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Keep Assets", color = MutedText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.deleteCurrentWorkflow()
                                showDeleteConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = WhitePrimary),
                            modifier = Modifier.testTag("delete_confirm_confirmation")
                        ) {
                            Text("Purge Workspace", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal dialog 5: History Result Viewer Dialog
    if (selectedHistory != null) {
        val history = selectedHistory!!
        Dialog(onDismissRequest = { viewModel.showHistoryDetails(null) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RUN TRANSCRIPT",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = VioletGlow
                                )
                            )
                            Text(
                                text = history.workflowName,
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhitePrimary
                                )
                            )
                        }

                        IconButton(
                            onClick = { viewModel.showHistoryDetails(null) },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MutedText)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close View")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            Text(
                                text = "Variables Input:",
                                style = MaterialTheme.typography.titleSmall,
                                color = NeonCyan
                            )
                            val inputs = JsonUtils.stringToMap(history.inputsJson)
                            if (inputs.isEmpty()) {
                                Text("No custom parameters passed.", color = MutedText, fontSize = 12.sp)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    inputs.forEach { (k, v) ->
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "$k: ",
                                                fontWeight = FontWeight.Bold,
                                                color = WhitePrimary,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.width(80.dp)
                                            )
                                            Text(text = v, color = MutedText, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = DarkSurfaceVariant)
                        }

                        if (!history.success) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(RedAlert.copy(alpha = 0.15f))
                                        .border(BorderStroke(1.dp, RedAlert.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Error Message Log:\n${history.errorMessage ?: "Unknown core execution fault."}",
                                        color = RedAlert,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        val outputs = JsonUtils.stringToOutputs(history.outputsJson)
                        itemsIndexed(outputs) { idx, out ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                border = BorderStroke(1.dp, DarkSurfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Step ${idx + 1}: ${out.stepName}",
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Prompt Used:",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MutedText,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = out.promptUsed,
                                        color = MutedText,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkSurface, RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Model Response:",
                                            fontWeight = FontWeight.SemiBold,
                                            color = SoftTeal,
                                            fontSize = 10.sp
                                        )

                                        val cm = LocalClipboardManager.current
                                        IconButton(
                                            onClick = { cm.setText(AnnotatedString(out.rawOutput)) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Copy Output to Clipboard",
                                                tint = SoftTeal,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = out.rawOutput,
                                        color = WhitePrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                viewModel.deleteHistoryRecord(history.id)
                                viewModel.showHistoryDetails(null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedAlert.copy(alpha = 0.15f), contentColor = RedAlert),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Discard Record")
                        }

                        Button(
                            onClick = { viewModel.showHistoryDetails(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = WhitePrimary)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PipelineBuilderView(
    steps: List<WorkflowStep>,
    onUpdateStep: (Int, WorkflowStep) -> Unit,
    onAddStep: () -> Unit,
    onRemoveStep: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize().testTag("builder_tab"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CHOREOGRAPH PIPELINE",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Build context-aware logic chains. Reference former step outputs with {step1_output}.",
                        color = MutedText,
                        fontSize = 10.sp
                    )
                }

                Button(
                    onClick = onAddStep,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_step_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Setup Step", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Stage", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (steps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Empty", tint = MutedText, modifier = Modifier.size(48.dp))
                        Text("No logical stages defined in pipeline.", color = MutedText, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(steps) { index, step ->
                        var isExpanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("step_card_$index"),
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                            border = BorderStroke(1.dp, DarkSurfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(VioletGlow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                color = WhitePrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        TextField(
                                            value = step.stepName,
                                            onValueChange = { onUpdateStep(index, step.copy(stepName = it)) },
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedTextColor = WhitePrimary,
                                                unfocusedTextColor = WhitePrimary
                                            ),
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.width(180.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { isExpanded = !isExpanded }) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Expand expert fields",
                                                tint = MutedText
                                            )
                                        }

                                        IconButton(onClick = { onRemoveStep(index) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove core step",
                                                tint = RedAlert.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text("PROMPT COMPOSITION FORMULA:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                OutlinedTextField(
                                    value = step.promptTemplate,
                                    onValueChange = { onUpdateStep(index, step.copy(promptTemplate = it)) },
                                    placeholder = { Text("Compile custom instruction utilizing brackets e.g., Generate hook about {topic}...") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = DarkSurfaceVariant,
                                        focusedTextColor = WhitePrimary,
                                        unfocusedTextColor = WhitePrimary
                                    ),
                                    textStyle = TextStyle(fontSize = 12.sp),
                                    modifier = Modifier.fillMaxWidth().testTag("prompt_input_$index")
                                )

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        Text("EXPERT SYSTEM INSTRUCTION:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VioletGlow)
                                        OutlinedTextField(
                                            value = step.systemInstruction,
                                            onValueChange = { onUpdateStep(index, step.copy(systemInstruction = it)) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = VioletGlow,
                                                unfocusedBorderColor = DarkSurfaceVariant,
                                                focusedTextColor = WhitePrimary,
                                                unfocusedTextColor = WhitePrimary
                                            ),
                                            textStyle = TextStyle(fontSize = 12.sp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PipelineExecuteView(
    steps: List<WorkflowStep>,
    extractedVars: List<String>,
    runtimeInputs: Map<String, String>,
    onUpdateInput: (String, String) -> Unit,
    model: String,
    onModelSelect: (String) -> Unit,
    executionState: ExecutionState,
    onTriggerRun: () -> Unit,
    onClearTerminal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize().testTag("execute_tab"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Execution control head
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EXECUTION TERMINAL",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Fill variables, select intelligence model, and compile pipelines.",
                        color = MutedText,
                        fontSize = 10.sp
                    )
                }

                // AI Model select trigger
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .padding(2.dp)
                ) {
                    val models = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview")
                    models.forEach { m ->
                        val isSelected = model == m
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) DarkSurfaceVariant else Color.Transparent)
                                .clickable { onModelSelect(m) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (m.contains("pro")) "PRO PROMPT" else "LIGHT SPEED",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) NeonCyan else MutedText
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body layouts
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Variable Form Pane
                Card(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    border = BorderStroke(1.dp, DarkSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "VARIABLES MATRIX",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = VioletGlow
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (extractedVars.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No dynamic placeholders found. (E.g., utilize brackets {topic} inside templates).",
                                    color = MutedText,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(extractedVars) { variable ->
                                    val currentVal = runtimeInputs[variable] ?: ""
                                    Column {
                                        Text(
                                            text = variable.replace("_", " ").uppercase(),
                                            style = TextStyle(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WhitePrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        OutlinedTextField(
                                            value = currentVal,
                                            onValueChange = { onUpdateInput(variable, it) },
                                            placeholder = { Text("Specify core parameter...") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonCyan,
                                                unfocusedBorderColor = DarkSurfaceVariant,
                                                focusedTextColor = WhitePrimary,
                                                unfocusedTextColor = WhitePrimary
                                            ),
                                            textStyle = TextStyle(fontSize = 12.sp),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("variable_input_$variable")
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = onTriggerRun,
                            enabled = executionState !is ExecutionState.Running,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("run_pipeline_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Trigger Run Sequence")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RUN CHAIN SEQUENCE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Live Stream Logs Monitor Console (Terminal Screen representation)
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    border = BorderStroke(1.dp, DarkSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OUTPUT PROGRESS HUB",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = SoftTeal
                                )
                            )

                            if (executionState !is ExecutionState.Idle) {
                                TextButton(onClick = onClearTerminal) {
                                    Text("Reset Console", color = MutedText, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Output console status monitor states
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            when (executionState) {
                                is ExecutionState.Idle -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(imageVector = Icons.Default.Star, contentDescription = "Idle", tint = DarkSurfaceVariant, modifier = Modifier.size(54.dp))
                                            Text("Console ready. Enter variables and fire pipeline.", color = MutedText, fontSize = 12.sp)
                                        }
                                    }
                                }
                                is ExecutionState.Running -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            CircularProgressIndicator(color = NeonCyan)
                                            Text(
                                                text = "COMPILING STAGE ${executionState.currentStepIndex + 1}/${executionState.totalSteps}",
                                                color = WhitePrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = executionState.stepName,
                                                color = NeonCyan,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                                is ExecutionState.Error -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Warning, contentDescription = "Critical Error", tint = RedAlert, modifier = Modifier.size(48.dp))
                                            Text(
                                                text = "EXECUTION DISRUPTED",
                                                fontWeight = FontWeight.Bold,
                                                color = RedAlert,
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = executionState.message,
                                                color = MutedText,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.background(DarkSurface, RoundedCornerShape(8.dp)).padding(10.dp)
                                            )
                                        }
                                    }
                                }
                                is ExecutionState.StepFinished, is ExecutionState.Success -> {
                                    val outputs = if (executionState is ExecutionState.Success) {
                                        executionState.finalOutputs
                                    } else {
                                        val finishedState = executionState as ExecutionState.StepFinished
                                        listOf(
                                            StepExecutionOutput(
                                                stepName = finishedState.stepName,
                                                promptUsed = "Running...",
                                                rawOutput = finishedState.output
                                            )
                                        )
                                    }

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (executionState is ExecutionState.Success) {
                                            item {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(SoftTeal.copy(alpha = 0.15f))
                                                        .border(BorderStroke(1.dp, SoftTeal.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Done", tint = SoftTeal)
                                                    Column {
                                                        Text("CHANNELS COMPILED SUCCESSFULLY", fontWeight = FontWeight.Bold, color = SoftTeal, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                                        Text("Pipeline fully populated in workspace active memory. Check results below.", color = MutedText, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }

                                        itemsIndexed(outputs) { index, out ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().testTag("output_card_$index"),
                                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                                border = BorderStroke(1.dp, DarkSurfaceVariant)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Stage ${index + 1}: ${out.stepName}",
                                                            fontWeight = FontWeight.Bold,
                                                            color = NeonCyan,
                                                            fontSize = 12.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )

                                                        val cm = LocalClipboardManager.current
                                                        IconButton(
                                                            onClick = { cm.setText(AnnotatedString(out.rawOutput)) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Share,
                                                                contentDescription = "Copy text asset",
                                                                tint = NeonCyan,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = out.rawOutput,
                                                        color = WhitePrimary,
                                                        fontSize = 12.sp,
                                                        lineHeight = 18.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SnapshotsTimelineView(
    snapshots: List<WorkflowSnapshotEntity>,
    onCreateSnapshot: () -> Unit,
    onRestoreSnapshot: (WorkflowSnapshotEntity) -> Unit,
    onDeleteSnapshot: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize().testTag("snapshots_tab"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VERSION HISTORY SNAPSHOTS",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Roll back configuration template matrices, capture snapshots checkpoints programmatically.",
                        color = MutedText,
                        fontSize = 10.sp
                    )
                }

                Button(
                    onClick = onCreateSnapshot,
                    colors = ButtonDefaults.buttonColors(containerColor = VioletGlow, contentColor = WhitePrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("create_snapshot_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Add Setup Snapshot", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Take Snapshot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (snapshots.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Empty", tint = DarkSurfaceVariant, modifier = Modifier.size(48.dp))
                        Text("No snapshot checkpoints recorded for this workflow.", color = MutedText, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(snapshots) { snap ->
                        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(snap.timestamp))
                        val stepCount = JsonUtils.stringToSteps(snap.stepsListJson).size

                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("snapshot_card_${snap.id}"),
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                            border = BorderStroke(1.dp, DarkSurfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(VioletGlow.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(snap.label, color = VioletGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Text(
                                            text = "$stepCount stages",
                                            color = MutedText,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Saved on: $dateString",
                                        color = MutedText,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = { onRestoreSnapshot(snap) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.1f), contentColor = NeonCyan),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("restore_snapshot_${snap.id}")
                                    ) {
                                        Text("Restore", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(onClick = { onDeleteSnapshot(snap.id) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete snapshot", tint = RedAlert.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityLogsView(
    histories: List<ExecutionHistoryEntity>,
    onSelectItem: (ExecutionHistoryEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize().testTag("history_tab"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "EXECUTION HISTORIC LOGS",
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Audits and review pipelines, verify previous generations and clone output assets.",
                color = MutedText,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (histories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Empty log", tint = DarkSurfaceVariant, modifier = Modifier.size(48.dp))
                        Text("No pipeline run telemetry records yet.", color = MutedText, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(histories) { log ->
                        val dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        val outputsCount = JsonUtils.stringToOutputs(log.outputsJson).size

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_card_${log.id}")
                                .clickable { onSelectItem(log) },
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                            border = BorderStroke(1.dp, if (log.success) DarkSurfaceVariant else RedAlert.copy(0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (log.success) SoftTeal else RedAlert)
                                        )

                                        Text(
                                            text = if (log.success) "Run Completed Successfully" else "Run Interrupted",
                                            color = if (log.success) SoftTeal else RedAlert,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Compiled $outputsCount prompt outputs on: $dateString",
                                        color = MutedText,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Inspect run content",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
