package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis(),
    val stepsListJson: String
)

@Entity(tableName = "workflow_snapshots")
data class WorkflowSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workflowId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val label: String,
    val stepsListJson: String
)

@Entity(tableName = "execution_histories")
data class ExecutionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workflowId: Int,
    val workflowName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val inputsJson: String,
    val outputsJson: String,
    val success: Boolean,
    val errorMessage: String? = null
)
