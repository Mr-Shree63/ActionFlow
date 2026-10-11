package com.example.data.repository

import com.example.data.dao.WorkflowRuleDao
import com.example.data.model.WorkflowRuleEntity
import kotlinx.coroutines.flow.Flow

class WorkflowRepository(private val workflowRuleDao: WorkflowRuleDao) {
    val allRules: Flow<List<WorkflowRuleEntity>> = workflowRuleDao.getAllRules()

    suspend fun getEnabledRules(): List<WorkflowRuleEntity> = workflowRuleDao.getEnabledRules()

    suspend fun getRuleById(id: Long): WorkflowRuleEntity? = workflowRuleDao.getRuleById(id)

    suspend fun insertRule(rule: WorkflowRuleEntity): Long = workflowRuleDao.insertRule(rule)

    suspend fun updateRule(rule: WorkflowRuleEntity) = workflowRuleDao.updateRule(rule)

    suspend fun deleteRule(rule: WorkflowRuleEntity) = workflowRuleDao.deleteRule(rule)

    suspend fun deleteRuleById(id: Long) = workflowRuleDao.deleteRuleById(id)
}
