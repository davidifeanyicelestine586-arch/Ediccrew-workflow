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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
    val savedTemplates by viewModel.savedTemplates.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Steps, 1: Execute, 2: Snapshots, 3: History
    var showApiKeyInfoDialog by remember { mutableStateOf(false) }
    var showSnapshotCreateDialog by remember { mutableStateOf(false) }
    var newSnapshotLabel by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Saved Prompt Templates local state (localStorage)
    var pipelinesExpanded by remember { mutableStateOf(true) }
    var templatesExpanded by remember { mutableStateOf(true) }
    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var showTemplateDetailDialog by remember { mutableStateOf(false) }
    var selectedTemplateForDetail by remember { mutableStateOf<com.example.data.database.SavedPromptTemplate?>(null) }
    var templateTitleInput by remember { mutableStateOf("") }
    var templateDescriptionInput by remember { mutableStateOf("") }
    var templateContentInput by remember { mutableStateOf("") }
    var isEditingTemplate by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 720
    var showMobileSidebar by remember { mutableStateOf(true) }

    // Dialog layout state for responsive widths
    Scaffold(
        modifier = modifier.testTag("dashboard_root"),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (!isWideScreen) {
                        IconButton(
                            onClick = { showMobileSidebar = !showMobileSidebar },
                            modifier = Modifier.testTag("mobile_sidebar_toggle")
                        ) {
                            Icon(
                                imageVector = if (showMobileSidebar) Icons.Default.Menu else Icons.Default.ArrowBack,
                                contentDescription = "Toggle Sidebar",
                                tint = WhitePrimary
                            )
                        }
                    }
                },
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
                if (isWideScreen || showMobileSidebar) {
                    // Panel 1: Vertical Directory list (Sidebar on wider devices, collapses if needed)
                    Card(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(bottom = 12.dp)
                            .testTag("directory_panel")
                            .then(
                                if (isWideScreen) Modifier.width(280.dp) else Modifier.fillMaxWidth()
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkSurfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // --- SECTION 1: SYSTEM PIPELINES (ROOM) ---
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { pipelinesExpanded = !pipelinesExpanded }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (pipelinesExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = MutedText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "SYSTEM PIPELINES",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.2.sp,
                                                    color = if (pipelinesExpanded) NeonCyan else MutedText
                                                )
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.createNewWorkflow()
                                                if (!isWideScreen) {
                                                    showMobileSidebar = false
                                                }
                                            },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .testTag("add_workflow_button"),
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = NeonCyan.copy(alpha = 0.1f),
                                                contentColor = NeonCyan
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "New Workflow",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                if (pipelinesExpanded) {
                                    if (workflows.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = "No Pipelines",
                                                        tint = MutedText,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = "NO PIPELINES",
                                                        style = TextStyle(
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp,
                                                            color = MutedText
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    } else {
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
                                                    .clickable {
                                                        viewModel.selectWorkflow(workflow.id)
                                                        if (!isWideScreen) {
                                                            showMobileSidebar = false
                                                        }
                                                    }
                                                    .padding(8.dp)
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
                                                                fontSize = 13.sp,
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
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = VioletGlow
                                                        ),
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )
                                                    Text(
                                                        text = workflow.description,
                                                        style = TextStyle(
                                                            fontSize = 10.sp,
                                                            color = MutedText
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(
                                        color = DarkSurfaceVariant,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                // --- SECTION 2: SAVED PROMPT TEMPLATES (LOCAL) ---
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { templatesExpanded = !templatesExpanded }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (templatesExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = MutedText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Column {
                                                Text(
                                                    text = "SAVED TEMPLATES",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 1.2.sp,
                                                        color = if (templatesExpanded) VioletGlow else MutedText
                                                    )
                                                )
                                                Text(
                                                    text = "LOCALSTORAGE PERSISTED",
                                                    style = TextStyle(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MutedText.copy(alpha = 0.5f)
                                                    )
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                templateTitleInput = ""
                                                templateDescriptionInput = ""
                                                templateContentInput = ""
                                                showCreateTemplateDialog = true
                                            },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .testTag("add_template_button"),
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = VioletGlow.copy(alpha = 0.15f),
                                                contentColor = VioletGlow
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "New Prompt Template",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                if (templatesExpanded) {
                                    if (savedTemplates.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp)
                                                    .border(1.dp, DarkSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No custom templates yet.\nTap '+' to create one.",
                                                    style = TextStyle(
                                                        fontSize = 11.sp,
                                                        color = MutedText,
                                                        textAlign = TextAlign.Center
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        items(savedTemplates) { template ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(DarkBackground.copy(alpha = 0.6f))
                                                    .border(
                                                        width = 1.dp,
                                                        color = DarkSurfaceVariant,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        selectedTemplateForDetail = template
                                                        templateTitleInput = template.title
                                                        templateDescriptionInput = template.description
                                                        templateContentInput = template.template
                                                        isEditingTemplate = false
                                                        showTemplateDetailDialog = true
                                                    }
                                                    .padding(10.dp)
                                            ) {
                                                Column {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Text(
                                                            text = template.title,
                                                            style = TextStyle(
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = VioletGlow
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.deleteLocalTemplate(template.id)
                                                            },
                                                            modifier = Modifier.size(20.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete Template",
                                                                tint = Color.Red.copy(alpha = 0.7f),
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                    }
                                                    if (template.description.isNotEmpty()) {
                                                        Text(
                                                            text = template.description,
                                                            style = TextStyle(
                                                                fontSize = 10.sp,
                                                                color = MutedText
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.padding(top = 1.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = template.template,
                                                        style = TextStyle(
                                                            fontSize = 10.sp,
                                                            color = WhitePrimary.copy(alpha = 0.6f),
                                                            fontFamily = FontFamily.Monospace
                                                        ),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier
                                                            .padding(top = 3.dp)
                                                            .fillMaxWidth()
                                                            .background(DarkSurface.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                            .padding(4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }

                if (isWideScreen) {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                if (isWideScreen || !showMobileSidebar) {
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
                                            onValueChange = { viewModel.updateActiveName(it) },
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
                                        onValueChange = { viewModel.updateActiveDescription(it) },
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
    }

    // LocalStorage: Create Prompt Template Dialog
    if (showCreateTemplateDialog) {
        Dialog(onDismissRequest = { showCreateTemplateDialog = false }) {
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
                        text = "Create Saved Template",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = VioletGlow,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = "Build general templates stored in persistent local storage. Reference inputs using double curly brackets: {{variable}}.",
                        fontSize = 11.sp,
                        color = MutedText
                    )

                    OutlinedTextField(
                        value = templateTitleInput,
                        onValueChange = { templateTitleInput = it },
                        label = { Text("Template Title", fontSize = 11.sp, color = MutedText) },
                        textStyle = TextStyle(color = WhitePrimary, fontSize = 13.sp),
                        isError = templateTitleInput.isBlank(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioletGlow,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = templateDescriptionInput,
                        onValueChange = { templateDescriptionInput = it },
                        label = { Text("Short Description", fontSize = 11.sp, color = MutedText) },
                        textStyle = TextStyle(color = WhitePrimary, fontSize = 13.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioletGlow,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = templateContentInput,
                        onValueChange = { templateContentInput = it },
                        label = { Text("Prompt Template (e.g. {{topic}})", fontSize = 11.sp, color = MutedText) },
                        textStyle = TextStyle(color = WhitePrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        minLines = 4,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioletGlow,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showCreateTemplateDialog = false }
                        ) {
                            Text("Cancel", color = MutedText, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (templateTitleInput.isNotBlank() && templateContentInput.isNotBlank()) {
                                    viewModel.saveLocalTemplate(
                                        title = templateTitleInput,
                                        description = templateDescriptionInput,
                                        template = templateContentInput
                                    )
                                    showCreateTemplateDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VioletGlow, contentColor = Color.White),
                            enabled = templateTitleInput.isNotBlank() && templateContentInput.isNotBlank(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save to LocalStorage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // LocalStorage: Template Details, Quick-Inject & Edit Dialog
    if (showTemplateDetailDialog && selectedTemplateForDetail != null) {
        val template = selectedTemplateForDetail!!
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

        Dialog(onDismissRequest = { showTemplateDetailDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkSurfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEditingTemplate) "Edit Template" else "Template Details",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = VioletGlow,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        
                        Text(
                            text = "LOCAL PERSISTED",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = VioletGlow.copy(alpha = 0.6f)
                            )
                        )
                    }

                    if (isEditingTemplate) {
                        OutlinedTextField(
                            value = templateTitleInput,
                            onValueChange = { templateTitleInput = it },
                            label = { Text("Title", fontSize = 11.sp) },
                            textStyle = TextStyle(color = WhitePrimary, fontSize = 12.sp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VioletGlow,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = templateDescriptionInput,
                            onValueChange = { templateDescriptionInput = it },
                            label = { Text("Description", fontSize = 11.sp) },
                            textStyle = TextStyle(color = WhitePrimary, fontSize = 12.sp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VioletGlow,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = templateContentInput,
                            onValueChange = { templateContentInput = it },
                            label = { Text("Template Content", fontSize = 11.sp) },
                            textStyle = TextStyle(color = WhitePrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            minLines = 4,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VioletGlow,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = template.title,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            )
                            if (template.description.isNotEmpty()) {
                                Text(
                                    text = template.description,
                                    style = TextStyle(fontSize = 11.sp, color = MutedText)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = DarkSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Text(
                                text = "TEMPLATE PROMPT:",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = VioletGlow, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = template.template,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = WhitePrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isEditingTemplate) {
                            Row {
                                Button(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(template.template))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DarkSurfaceVariant,
                                        contentColor = WhitePrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.sizeIn(minHeight = 28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Copy Text", fontSize = 10.sp)
                                }
                                
                                Spacer(modifier = Modifier.width(6.dp))
                                
                                Button(
                                    onClick = {
                                        isEditingTemplate = true
                                        templateTitleInput = template.title
                                        templateDescriptionInput = template.description
                                        templateContentInput = template.template
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DarkSurfaceVariant,
                                        contentColor = NeonCyan
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.sizeIn(minHeight = 28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Edit", fontSize = 10.sp)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Row {
                            TextButton(
                                onClick = {
                                    if (isEditingTemplate) {
                                        isEditingTemplate = false
                                    } else {
                                        showTemplateDetailDialog = false
                                    }
                                }
                            ) {
                                Text(if (isEditingTemplate) "Discard" else "Close", color = MutedText, fontSize = 11.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            if (isEditingTemplate) {
                                Button(
                                    onClick = {
                                        if (templateTitleInput.isNotBlank() && templateContentInput.isNotBlank()) {
                                            viewModel.updateLocalTemplate(
                                                id = template.id,
                                                title = templateTitleInput,
                                                description = templateDescriptionInput,
                                                template = templateContentInput
                                            )
                                            isEditingTemplate = false
                                            selectedTemplateForDetail = template.copy(
                                                title = templateTitleInput,
                                                description = templateDescriptionInput,
                                                template = templateContentInput
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VioletGlow, contentColor = Color.White),
                                    shape = RoundedCornerShape(6.dp),
                                    enabled = templateTitleInput.isNotBlank() && templateContentInput.isNotBlank()
                                ) {
                                    Text("Save Changes", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        // Inject step!
                                        viewModel.addStepWithTemplate(
                                            stepName = "Stage: ${template.title}",
                                            template = template.template
                                        )
                                        showTemplateDetailDialog = false
                                        // Optionally direct user to builder tab
                                        activeTab = 0
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Insert Into Pipeline", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
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

                                RichPromptTemplateArea(
                                    promptTemplate = step.promptTemplate,
                                    onValueChange = { onUpdateStep(index, step.copy(promptTemplate = it)) },
                                    stepIndex = index
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // --- DATA FLOW PORT META ---
                                val clipboardManager = LocalClipboardManager.current
                                val currentStepOutputToken = "step${index + 1}_output"
                                val friendlyKey = step.stepName.lowercase().replace(Regex("[^a-z0-9_]"), "_") + "_output"
                                
                                val referencedPriorSteps = remember(step.promptTemplate) {
                                    val refs = mutableListOf<Pair<String, String>>()
                                    for (i in 0 until index) {
                                        val priorStep = steps[i]
                                        val key1 = "step${i + 1}_output"
                                        val key2 = priorStep.stepName.lowercase().replace(Regex("[^a-z0-9_]"), "_") + "_output"
                                        
                                        if (step.promptTemplate.contains(key1, ignoreCase = true) || 
                                            step.promptTemplate.contains(key2, ignoreCase = true)) {
                                            refs.add(Pair(priorStep.stepName.ifEmpty { "Stage ${i + 1}" }, key1))
                                        }
                                    }
                                    refs
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkSurface.copy(alpha = 0.5f))
                                        .border(BorderStroke(1.dp, DarkSurfaceVariant.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "STAGE PORT OUTFLOW:",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = SoftTeal,
                                                fontWeight = FontWeight.Bold
                                            )
                                            
                                            // Main indicator token
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SoftTeal.copy(alpha = 0.15f))
                                                    .clickable { clipboardManager.setText(AnnotatedString("{{$currentStepOutputToken}}")) }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(
                                                        text = "{{$currentStepOutputToken}}",
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = SoftTeal,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(8.dp))
                                                }
                                            }
                                            
                                            // Friendly token descriptor if different
                                            if (friendlyKey != currentStepOutputToken) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(NeonCyan.copy(alpha = 0.12f))
                                                        .clickable { clipboardManager.setText(AnnotatedString("{{$friendlyKey}}")) }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text(
                                                            text = "{{$friendlyKey}}",
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = NeonCyan,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(8.dp))
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (referencedPriorSteps.isNotEmpty()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Text(
                                                    text = "CONSUMING COMPASS INFLOWS:",
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = VioletGlow,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                referencedPriorSteps.forEach { pair ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(VioletGlow.copy(alpha = 0.15f))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "🔗 ${pair.first}",
                                                            fontSize = 8.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = VioletGlow,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "No prior stages output ingested yet. Use {{stepN_output}} variables.",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MutedText.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

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

                        if (index < steps.lastIndex) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(16.dp)
                                        .background(VioletGlow.copy(alpha = 0.4f))
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(VioletGlow.copy(alpha = 0.12f))
                                        .border(BorderStroke(1.dp, VioletGlow.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = VioletGlow,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "PIPELINE LOGIC LINK: OUTFLOW FLOWS DOWNWARD",
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = VioletGlow
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(16.dp)
                                        .background(VioletGlow.copy(alpha = 0.4f))
                                )
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

class VariableHighlightTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        val builder = AnnotatedString.Builder()
        
        val doubleBraceRegex = Regex("\\{\\{([A-Za-z0-9_\\s]+)\\}\\}")
        val singleBraceRegex = Regex("\\{([A-Za-z0-9_\\s]+)\\}")
        
        // Let's build styled text
        builder.append(rawText)
        
        // Format double braces with high priority dynamic look
        doubleBraceRegex.findAll(rawText).forEach { match ->
            builder.addStyle(
                style = SpanStyle(
                    color = NeonCyan,
                    background = NeonCyan.copy(alpha = 0.15f),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
        
        // Format single braces for backwards compatibility and clarity
        singleBraceRegex.findAll(rawText).forEach { match ->
            // Skip matches that are already covered by double braces
            val isDoubleBrace = match.range.first > 0 && 
                               match.range.last < rawText.length - 1 &&
                               rawText[match.range.first - 1] == '{' && 
                               rawText[match.range.last + 1] == '}'
            if (!isDoubleBrace) {
                builder.addStyle(
                    style = SpanStyle(
                        color = VioletGlow,
                        background = VioletGlow.copy(alpha = 0.12f),
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
        
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RichPromptTemplateArea(
    promptTemplate: String,
    onValueChange: (String) -> Unit,
    stepIndex: Int,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(promptTemplate) {
        mutableStateOf(
            TextFieldValue(
                text = promptTemplate,
                selection = TextRange(promptTemplate.length)
            )
        )
    }

    var showAddVarDialog by remember { mutableStateOf(false) }
    var customVarName by remember { mutableStateOf("") }

    val insertVariable = { variableName: String ->
        val formatted = "{{$variableName}}"
        val text = textFieldValue.text
        val sel = textFieldValue.selection
        val before = text.substring(0, sel.min)
        val after = text.substring(sel.max)
        val updatedText = before + formatted + after
        val updatedSelection = TextRange(before.length + formatted.length)
        
        textFieldValue = TextFieldValue(text = updatedText, selection = updatedSelection)
        onValueChange(updatedText)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PROMPT FORMULA BUILDER:",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                fontFamily = FontFamily.Monospace
            )
            
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { showAddVarDialog = true }
                    .background(NeonCyan.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Custom Variable",
                    tint = NeonCyan,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "INSERT VARIABLE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }

        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                if (it.text != promptTemplate) {
                    onValueChange(it.text)
                }
            },
            placeholder = { Text("Compile custom instruction e.g., Generate a hook about {{topic}} for {{audience}}...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedTextColor = WhitePrimary,
                unfocusedTextColor = WhitePrimary,
                focusedPlaceholderColor = MutedText,
                unfocusedPlaceholderColor = MutedText
            ),
            textStyle = TextStyle(fontSize = 12.sp),
            visualTransformation = VariableHighlightTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .testTag("prompt_input_$stepIndex")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal scroll row for quick insertion of variables.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUGGESTED:",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                modifier = Modifier.padding(end = 6.dp)
            )

            val standardVars = listOf("topic", "audience", "tone", "platform", "theme")
            val priorStepsOutputs = (1..stepIndex).map { "step${it}_output" }

            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(standardVars) { variable ->
                    SuggestionVariableChip(
                        label = variable,
                        color = NeonCyan,
                        onClick = { insertVariable(variable) }
                    )
                }

                items(priorStepsOutputs) { stepOutput ->
                    SuggestionVariableChip(
                        label = stepOutput,
                        color = VioletGlow,
                        onClick = { insertVariable(stepOutput) }
                    )
                }
            }
        }
    }

    if (showAddVarDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddVarDialog = false
                customVarName = ""
            },
            title = {
                Text(
                    "CREATE DYNAMIC VARIABLE",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            },
            text = {
                Column {
                    Text(
                        "Input variable name to insert e.g. {{niche}} as a dynamic template token.",
                        fontSize = 11.sp,
                        color = MutedText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = customVarName,
                        onValueChange = { customVarName = it.replace(Regex("[^A-Za-z0-9_]"), "") },
                        placeholder = { Text("e.g. niche") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = WhitePrimary,
                            unfocusedTextColor = WhitePrimary
                        ),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customVarName.isNotBlank()) {
                            insertVariable(customVarName.trim().lowercase())
                        }
                        showAddVarDialog = false
                        customVarName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
                ) {
                    Text("Insert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddVarDialog = false
                        customVarName = ""
                    }
                ) {
                    Text("Cancel", color = MutedText, fontSize = 11.sp)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun SuggestionVariableChip(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = null,
                tint = color.copy(alpha = 0.8f),
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "{{$label}}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = color
            )
        }
    }
}
