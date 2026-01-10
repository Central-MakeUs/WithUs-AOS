package org.koiware.demo.repository

import org.koiware.demo.CommonUtil.TIME_DESIRED_PATTERN
import org.koiware.demo.debug
import org.koiware.demo.domain.local.dao.EventLogDao
import org.koiware.demo.domain.model.local.EventLogEntity
import org.koiware.demo.domain.model.remote.safeApiCall
import org.koiware.demo.domain.toDto
import org.koiware.demo.errorLog
import org.koiware.demo.interfaces.LockControlRepository
import org.koiware.demo.remote.ApiService
import org.koiware.demo.request.ControlLogRequest
import org.koiware.demo.warn
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class LockControlRepositoryImpl @Inject constructor(
    private val eventLogDao: EventLogDao,
    private val apiService: ApiService,
) : LockControlRepository {
    override suspend fun insertEventLog(eventLog: EventLogEntity) {
        debug("insertEventLog : $eventLog")
        eventLogDao.insertEventLog(eventLog)
    }

    override suspend fun insertAllEventLogs(eventLogs: List<EventLogEntity>) {
        eventLogDao.insertAll(eventLogs)
    }

    override suspend fun getEventLogById(logId: Int): EventLogEntity? {
        return eventLogDao.getEventLogById(logId)
    }

    override suspend fun getEventLogsByTaskId(taskId: Long): List<EventLogEntity> {
        cleanUpOldLogs()
        return eventLogDao.getEventLogsByTaskId(taskId)
    }

    override suspend fun getEventLogsByTaskIdAndUserId(
        taskId: Long, userId: Long
    ): List<EventLogEntity> {
        return eventLogDao.getEventLogsByTaskIdAndUserId(taskId, userId)
    }

    override suspend fun getAllEventLogs(): List<EventLogEntity> {
        return eventLogDao.getAllEventLogs()
    }

    override suspend fun deleteEventLog(eventLogEntity: EventLogEntity) {
        eventLogDao.deleteEventLog(eventLogEntity)
    }

    override suspend fun updateEventLog(eventLogEntity: EventLogEntity) {
        eventLogDao.updateEventLog(eventLogEntity)
    }

    override suspend fun uploadControlEventLog(eventLog: EventLogEntity?): Result<Unit> {
        debug("uploadControlEventLog! ")
        val logEntityList = if (eventLog == null) getAllEventLogs() else listOf(eventLog)

        logEntityList.forEach { logEntity ->
            debug("logEntity : $logEntity")
        }
        val unSyncedLogs = logEntityList.filter { !it.isSynced }
        val logDtoList = unSyncedLogs.map { it.toDto() }

        val controlLogRequest = ControlLogRequest(logDtoList)
        if (unSyncedLogs.isNotEmpty()) {
            return safeApiCall(
                call = { apiService.updateControlEventLog(controlLogRequest) },
                transform = { baseResponse ->
                },
            ).apply {
                if (eventLog == null) {
                    debug("eventLog 저장 !")
                    this.onSuccess {
                        eventLogDao.insertAll(unSyncedLogs.map {
                            it.copy(isSynced = true)
                        })
                    }
                }
            }
        } else {
            return Result.success(Unit)
        }
    }

    override suspend fun updateAllEventLogsToSynced() {
        eventLogDao.updateAllEventLogsToSynced()

    }

    override suspend fun updateEventLogsToSynced(eventLogs: List<EventLogEntity>) {
        val logIds = eventLogs.map { it.logId }
        eventLogDao.updateEventLogsToSyncedByIds(logIds)
    }

    override suspend fun deleteAllEventLogs() {
        eventLogDao.deleteAllEventLogs()
    }

    private suspend fun cleanUpOldLogs() {
        val deletedCount = eventLogDao.deleteOldSyncedEventLogs()
        debug("Deleted $deletedCount old event logs.")
    }

    override suspend fun checkKeyValidity(issuanceId: String): Result<Unit> {
        return safeApiCall(
            call = { apiService.checkKeyValidity(issuanceId) },
            transform = { response ->
            })
    }
}
