package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {
    @Query("SELECT * FROM workflows ORDER BY createdAt DESC")
    fun getAllWorkflows(): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows ORDER BY createdAt DESC")
    suspend fun getAllWorkflowsDirect(): List<WorkflowEntity>

    @Query("SELECT * FROM workflows WHERE id = :id")
    suspend fun getWorkflowById(id: Int): WorkflowEntity?

    @Query("SELECT * FROM workflows WHERE id = :id")
    fun getWorkflowByIdFlow(id: Int): Flow<WorkflowEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: WorkflowEntity): Long

    @Update
    suspend fun updateWorkflow(workflow: WorkflowEntity)

    @Query("DELETE FROM workflows WHERE id = :id")
    suspend fun deleteWorkflowById(id: Int)

    // Snapshots
    @Query("SELECT * FROM workflow_snapshots WHERE workflowId = :workflowId ORDER BY timestamp DESC")
    fun getSnapshotsForWorkflow(workflowId: Int): Flow<List<WorkflowSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: WorkflowSnapshotEntity): Long

    @Query("DELETE FROM workflow_snapshots WHERE id = :id")
    suspend fun deleteSnapshot(id: Int)

    @Query("DELETE FROM workflow_snapshots WHERE workflowId = :workflowId")
    suspend fun clearSnapshotsForWorkflow(workflowId: Int)

    // Execution Histories
    @Query("SELECT * FROM execution_histories ORDER BY timestamp DESC")
    fun getAllExecutionHistories(): Flow<List<ExecutionHistoryEntity>>

    @Query("SELECT * FROM execution_histories WHERE workflowId = :workflowId ORDER BY timestamp DESC")
    fun getExecutionHistoriesForWorkflow(workflowId: Int): Flow<List<ExecutionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecutionHistory(history: ExecutionHistoryEntity): Long

    @Query("DELETE FROM execution_histories WHERE id = :id")
    suspend fun deleteExecutionHistory(id: Int)
}
