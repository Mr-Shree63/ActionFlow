package com.example.data.repository

import com.example.data.dao.TaskDao
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val pendingTasks: Flow<List<TaskEntity>> = taskDao.getPendingTasks()

    fun getTasksForDate(date: String): Flow<List<TaskEntity>> = taskDao.getTasksForDate(date)

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    suspend fun findTasksByTitle(query: String): List<TaskEntity> = taskDao.findTasksByTitle(query)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun insertTasks(tasks: List<TaskEntity>): List<Long> = taskDao.insertTasks(tasks)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)

    suspend fun deleteCompletedTasks(): Int = taskDao.deleteCompletedTasks()

    suspend fun getTasksForWorkflowAndDate(ruleId: Long, date: String): List<TaskEntity> =
        taskDao.getTasksForWorkflowAndDate(ruleId, date)
}
