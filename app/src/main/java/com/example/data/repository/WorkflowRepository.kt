package com.example.data.repository

import com.example.data.database.WorkflowDao
import com.example.data.database.WorkflowEntity
import com.example.data.database.WorkflowSnapshotEntity
import com.example.data.database.ExecutionHistoryEntity
import com.example.data.model.WorkflowStep
import com.example.data.model.JsonUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class WorkflowRepository(private val dao: WorkflowDao) {
    val allWorkflows: Flow<List<WorkflowEntity>> = dao.getAllWorkflows()
    val allExecutionHistories: Flow<List<ExecutionHistoryEntity>> = dao.getAllExecutionHistories()

    fun getWorkflowByIdFlow(id: Int): Flow<WorkflowEntity?> = dao.getWorkflowByIdFlow(id)

    suspend fun getWorkflowById(id: Int): WorkflowEntity? = dao.getWorkflowById(id)

    suspend fun insertWorkflow(workflow: WorkflowEntity): Long = dao.insertWorkflow(workflow)

    suspend fun updateWorkflow(workflow: WorkflowEntity) = dao.updateWorkflow(workflow)

    suspend fun deleteWorkflow(id: Int) {
        dao.deleteWorkflowById(id)
        dao.clearSnapshotsForWorkflow(id)
    }

    // Snapshots
    fun getSnapshotsForWorkflow(workflowId: Int): Flow<List<WorkflowSnapshotEntity>> =
        dao.getSnapshotsForWorkflow(workflowId)

    suspend fun createSnapshot(workflowId: Int, label: String, steps: List<WorkflowStep>): Long {
        val snapshot = WorkflowSnapshotEntity(
            workflowId = workflowId,
            label = label,
            stepsListJson = JsonUtils.stepsToString(steps)
        )
        return dao.insertSnapshot(snapshot)
    }

    suspend fun deleteSnapshot(snapshotId: Int) = dao.deleteSnapshot(snapshotId)

    // Histories
    fun getExecutionHistoriesForWorkflow(workflowId: Int): Flow<List<ExecutionHistoryEntity>> =
        dao.getExecutionHistoriesForWorkflow(workflowId)

    suspend fun insertExecutionHistory(history: ExecutionHistoryEntity): Long =
        dao.insertExecutionHistory(history)

    suspend fun deleteExecutionHistory(id: Int) = dao.deleteExecutionHistory(id)

    suspend fun getAllWorkflowsDirect(): List<WorkflowEntity> = dao.getAllWorkflowsDirect()

    // See helper
    suspend fun seedInitialDataIfDbEmpty() {
        val existing = dao.getAllWorkflowsDirect()
        if (existing.isEmpty()) {
            val templates = listOf(
                WorkflowEntity(
                    name = "Viral TikTok Script System",
                    description = "Runs raw niche concepts through systematic Hook Discovery, energetic vertical Script Drafting, and platform-specific SEO planning.",
                    category = "Video Content",
                    stepsListJson = JsonUtils.stepsToString(
                        listOf(
                            WorkflowStep(
                                stepName = "Hook Discovery Engine",
                                promptTemplate = "Analyze the niche topic '{topic}' specifically targeting '{audience}'. Create 5 engaging short-form TikTok hook angles that grab attention in the first 2 seconds.",
                                systemInstruction = "You are a viral hook specialist with deep insights into digital retention metrics."
                            ),
                            WorkflowStep(
                                stepName = "Aesthetic Scriptwriter",
                                promptTemplate = "Based on these hooks: {step1_output}, pick the single best hook and draft a full 150-word energetic verbal TikTok script. Include director annotations in brackets (like [laughing] or [b-roll of setup]). Structure with: Hook (first 3s), Core Content, and High-Retention CTA.",
                                systemInstruction = "You are an expert short-form scriptwriter. Keep responses snappy, clear, and highly verbal."
                            ),
                            WorkflowStep(
                                stepName = "Metadata & Thumbnail Planner",
                                promptTemplate = "Using the generated script: {step2_output}, provide 3 highly clickable text titles for the overlay, 8 highly optimized search SEO keywords, a description with trending hashtags, and a detailed b-roll list.",
                                systemInstruction = "You are a technical search platform optimizer. Maximize click-through rates & discoverability."
                            )
                        )
                    )
                ),
                WorkflowEntity(
                    name = "Deep SEO Blog Blueprint",
                    description = "Transforms any keyword topic idea into qualified SEO search intentions, H-tag structural outlines, and custom intro text hooks.",
                    category = "SEO & Writing",
                    stepsListJson = JsonUtils.stepsToString(
                        listOf(
                            WorkflowStep(
                                stepName = "Intent & Keyword Match",
                                promptTemplate = "For the raw keyword idea '{topic}', identify 5 semantic secondary keywords. Determine the user search intent (informational, transactional, etc.) and write a 1-sentence content angle.",
                                systemInstruction = "You are an SEO Strategist who understands modern semantic search engine models."
                            ),
                            WorkflowStep(
                                stepName = "Semantic Outline Builder",
                                promptTemplate = "Using the keyword blueprint: {step1_output}, build a comprehensive SEO outline including H1, H2 sections, and H3 sub-items. For each block, specify what primary search queries should be answered.",
                                systemInstruction = "You are an expert content architect. Lay out exhaustive, logical outlines with clear visual structures."
                            ),
                            WorkflowStep(
                                stepName = "Empathetic Intro Writer",
                                promptTemplate = "Using the outline: {step2_output}, draft a highly compelling blog intro paragraph (approx 120 words). Connect immediately to the user's core problem, build trust, and end with a curiosity hook.",
                                systemInstruction = "You are a professional conversion copywriter writing copy that keeps readers scrolling."
                            )
                        )
                    )
                ),
                WorkflowEntity(
                    name = "LinkedIn Authority Orchestrator",
                    description = "Converts professional topics and lessons into scroll-stopping, structured thought-leadership pieces optimized for mobile feeds.",
                    category = "Personal Brand",
                    stepsListJson = JsonUtils.stepsToString(
                        listOf(
                            WorkflowStep(
                                stepName = "Contrarian Concept Sieve",
                                promptTemplate = "Think about the industry topic '{topic}' and identify one contrarian belief, common myth, or standard industry mistake associated with it. Explain why it is holding professionals back.",
                                systemInstruction = "You are a sharp industry critic who values practical execution over theory."
                            ),
                            WorkflowStep(
                                stepName = "Storyboarding & Lessons",
                                promptTemplate = "Translate this insight: {step1_output} into a clear storytelling framework. Frame it with a classic setup (The standard myth), conflict (How it fails), and result (The better solution) with three clear lessons.",
                                systemInstruction = "You are an executive storyteller. Craft narratives with high clarity and commercial sense."
                            ),
                            WorkflowStep(
                                stepName = "Mobile Format Editor",
                                promptTemplate = "Using the storyboard: {step2_output}, format it into an authoritative, scroll-stopping LinkedIn post. Ensure every line is brief (spacing is critical for mobile screens), there is no corporate jargon, and end with an engaging open-ended question.",
                                systemInstruction = "You are a LinkedIn personal branding advisor. Optimize copy for casual timeline readers."
                            )
                        )
                    )
                )
            )
            for (t in templates) {
                dao.insertWorkflow(t)
            }
        }
    }
}
