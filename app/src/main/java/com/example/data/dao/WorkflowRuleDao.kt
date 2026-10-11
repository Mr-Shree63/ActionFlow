package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WorkflowRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowRuleDao {
    @Query("SELECT * FROM workflow_rules ORDER BY scheduledTime ASC")
    fun getAllRules(): Flow<List<WorkflowRuleEntity>>

    @Query("SELECT * FROM workflow_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<WorkflowRuleEntity>

    @Query("SELECT * FROM workflow_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: Long): WorkflowRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: WorkflowRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<WorkflowRuleEntity>)

    @Update
    suspend fun updateRule(rule: WorkflowRuleEntity)

    @Delete
    suspend fun deleteRule(rule: WorkflowRuleEntity)

    @Query("DELETE FROM workflow_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}
