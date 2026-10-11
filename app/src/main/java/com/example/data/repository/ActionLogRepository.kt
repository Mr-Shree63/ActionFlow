package com.example.data.repository

import com.example.data.dao.ActionLogDao
import com.example.data.model.ActionLogEntity
import kotlinx.coroutines.flow.Flow

class ActionLogRepository(private val actionLogDao: ActionLogDao) {
    val allLogs: Flow<List<ActionLogEntity>> = actionLogDao.getAllLogs()

    suspend fun insertLog(log: ActionLogEntity): Long = actionLogDao.insertLog(log)

    suspend fun clearLogs() = actionLogDao.clearLogs()
}
