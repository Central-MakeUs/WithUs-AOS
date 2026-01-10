package org.koiware.demo.domain

import org.koiware.demo.CommonUtil
import org.koiware.demo.domain.model.domain.EventLog
import org.koiware.demo.domain.model.domain.LockPoint
import org.koiware.demo.domain.model.domain.LockPointStatus
import org.koiware.demo.domain.model.domain.MasterKey
import org.koiware.demo.domain.model.domain.Task
import org.koiware.demo.domain.model.domain.TaskAuth
import org.koiware.demo.domain.model.domain.UniqueKey
import org.koiware.demo.domain.model.local.UserEntity
import org.koiware.demo.domain.model.domain.UserInfo
import org.koiware.demo.domain.model.local.EventLogEntity
import org.koiware.demo.domain.model.local.LockPointEntity
import org.koiware.demo.domain.model.local.MasterKeyEntity
import org.koiware.demo.domain.model.local.TaskAuthEntity
import org.koiware.demo.domain.model.local.TaskEntity
import org.koiware.demo.domain.model.local.UniqueKeyEntity
import org.koiware.demo.domain.model.remote.LockStatusResDto
import org.koiware.demo.domain.model.remote.LoginResDto
import org.koiware.demo.domain.model.remote.TaskRelatedDto
import org.koiware.demo.domain.model.remote.TaskDto
import org.koiware.demo.domain.model.remote.UserInfoDto
import org.koiware.demo.request.EventLogReqDto

fun UserInfoDto.toUserEntity(pinCode: String): UserEntity {
    return UserEntity(
        loginId = this.loginId,
        userName = this.userNameFull,
        pinCode = pinCode,
        userId = userId,
    )
}

fun UserInfo.toUserEntity(pinCode: String): UserEntity {
    return UserEntity(
        loginId = this.loginId,
        userName = this.userName,
        pinCode = pinCode,
        userId = this.userId
    )
}

fun UserInfoDto.toDomain(initPasswd: Boolean): UserInfo {
    return UserInfo(
        loginId = this.loginId,
        userName = this.userNameFull,
        termsAgree = this.termsAgree,
        initPasswd = initPasswd,
        userId = this.userId,
    )
}


fun UserEntity.toUserInfo(): UserInfo {
    return UserInfo(
        loginId = this.loginId,
        userName = this.userName,
        pinCode = pinCode,
        userId = userId
    )
}

fun LoginResDto.loginDtoToDomain(): UserInfo {
    return UserInfo(
        loginId = this.userInfoDto?.loginId ?: "",
        userName = this.userInfoDto?.userNameFull ?: "",
        userId = this.userInfoDto?.userId ?: 0L,
        initPasswd = this.initPassword
    )
}

fun TaskRelatedDto.toDomain(): TaskAuth {
    return TaskAuth(
        taskId = task.taskId,
        loginId = task.loginId,
        taskRole = taskAuth.taskRole,
        isIssued = taskAuth.isIssued,
        isLockable = taskAuth.isLockable,
        isUnLockable = taskAuth.isUnLockable,
        isUnshackle = taskAuth.isUnshackle,
        isLogViewable = taskAuth.isLogViewable,
    )
}

fun TaskRelatedDto.toTaskAuthEntity(): TaskAuthEntity {
    return TaskAuthEntity(
        taskId = this.task.taskId,
        loginId = this.task.loginId,
        taskRole = this.taskAuth.taskRole,
        isIssued = this.taskAuth.isIssued,
        isLockable = this.taskAuth.isLockable,
        isUnLockable = this.taskAuth.isUnLockable,
        isUnshackle = this.taskAuth.isUnshackle,
        isLogViewable = this.taskAuth.isLogViewable,
    )
}

fun TaskRelatedDto.toLockPointList(): List<LockPointEntity> {
    return this.lockPoints.map { lockPointDto ->
        LockPointEntity(
            lockPointId = lockPointDto.lockPointId,
            taskId = lockPointDto.taskId,
            customerLockPointCode = lockPointDto.customerLockPointCode,
            lockPointName = lockPointDto.lockPointName,
            lockSerialNumber = lockPointDto.lockSerialNumber,
            dangerLevel = lockPointDto.dangerLevel,
            createdAt = CommonUtil.getCurrentTime(),
            solutionLockPointCode = lockPointDto.solutionLockPointCode,
        )
    }
}

fun TaskRelatedDto.toUniqueKeyList(): List<UniqueKeyEntity> {
    return this.uniqueKeys.map { uniqueKey ->
        UniqueKeyEntity(
            keyId = uniqueKey.keyId,
            taskId = uniqueKey.taskId,
            keyType = uniqueKey.keyType,
            path = "",
            isVlockKey = uniqueKey.isVlockKey,
            lockSerialNumber = uniqueKey.lockSerialNumber,
            dangerLevel = uniqueKey.dangerLevel,
            isEnabled = false,
            expiredAt = uniqueKey.expiredAt,
            issuedUserId = uniqueKey.issuedUserId,
            createdAt = CommonUtil.getCurrentTime(),
            lockPointId = uniqueKey.lockPointId
        )
    }
}

fun TaskRelatedDto.toMasterKey(): MasterKeyEntity? {
    return this.masterKey?.run {
        MasterKeyEntity(
            keyId = this.keyId,
            path = "",
            expiredAt = this.expiredAt,
            issuedUserId = this.issuedUserId,
            createdAt = CommonUtil.getCurrentTime(),
            taskId = task.taskId
        )
    }
}

fun LockPointEntity.toDomain(): LockPoint {
    return LockPoint(
        lockPointId = this.lockPointId,
        taskId = this.taskId,
        customerLockPointCode = this.customerLockPointCode,
        lockPointName = this.lockPointName,
        lockSerialNumber = this.lockSerialNumber,
        dangerLevel = this.dangerLevel,
        createdAt = this.createdAt,
        solutionLockPointCode = this.solutionLockPointCode,
    )
}

fun UniqueKeyEntity.toDomain(): UniqueKey {
    return UniqueKey(
        keyId = this.keyId,
        taskId = this.taskId,
        keyType = this.keyType,
        path = this.path,
        lockSerialNumber = this.lockSerialNumber,
        dangerLevel = this.dangerLevel,
        isEnabled = this.isEnabled,
        expiredAt = this.expiredAt,
        issuedUserId = this.issuedUserId,
        createdAt = this.createdAt,
        lockPointId = this.lockPointId,
        isVlockKey = this.isVlockKey,
    )
}

fun UniqueKey.toEntity(): UniqueKeyEntity {
    return UniqueKeyEntity(
        keyId = this.keyId,
        taskId = this.taskId,
        keyType = this.keyType,
        path = this.path,
        lockSerialNumber = this.lockSerialNumber,
        dangerLevel = this.dangerLevel,
        isEnabled = this.isEnabled,
        expiredAt = this.expiredAt,
        issuedUserId = this.issuedUserId,
        createdAt = this.createdAt,
        lockPointId = this.lockPointId,
        isVlockKey = this.isVlockKey
    )
}

fun MasterKeyEntity.toDomain(): MasterKey {
    return MasterKey(
        keyId = this.keyId,
        taskId = this.taskId,
        path = this.path,
        expiredAt = this.expiredAt,
        issuedUserId = this.issuedUserId,
        createdAt = this.createdAt
    )
}

fun MasterKey.toEntity(): MasterKeyEntity {
    return MasterKeyEntity(
        keyId = this.keyId,
        taskId = this.taskId,
        path = this.path,
        expiredAt = this.expiredAt,
        issuedUserId = this.issuedUserId,
        createdAt = this.createdAt,
    )
}

fun LockPoint.toEntity(): LockPointEntity {
    return LockPointEntity(
        lockPointId = this.lockPointId,
        taskId = this.taskId,
        customerLockPointCode = this.customerLockPointCode,
        lockPointName = this.lockPointName,
        lockSerialNumber = this.lockSerialNumber,
        dangerLevel = this.dangerLevel,
        createdAt = this.createdAt,
        solutionLockPointCode = this.solutionLockPointCode,
    )
}

fun TaskDto.toEntity(loginId: String): TaskEntity {
    return TaskEntity(
        taskId = this.taskId,
        taskName = this.taskName,
        loginId = loginId
    )
}

fun EventLogEntity.toDomain(lockPoint: LockPoint?): EventLog {
    return EventLog(
        lockPointName = lockPoint?.lockPointName ?: "",
        customerLockPointCode = lockPoint?.displayLockPointCode,
        status = this.status,
        eventAt = this.eventAt,
        serialNumber = this.serialNumber,
    )
}

fun TaskDto.toEntity(loginId: String, lastSyncedAt: String): TaskEntity {
    return TaskEntity(
        taskId = this.taskId,
        taskName = this.taskName,
        loginId = loginId,
        lastSyncedAt = lastSyncedAt
    )
}

fun TaskEntity.toTaskDomain(taskRole: String): Task {
    return Task(
        taskId = this.taskId,
        taskName = this.taskName,
        loginId = loginId,
        lastSyncedAt = this.lastSyncedAt,
        taskRole = taskRole
    )
}

fun TaskAuthEntity.toDomain(): TaskAuth {
    return TaskAuth(
        taskId = this.taskId,
        loginId = this.loginId,
        taskRole = this.taskRole,
        isIssued = this.isIssued,
        isLockable = this.isLockable,
        isUnLockable = this.isUnLockable,
        isUnshackle = this.isUnshackle,
        isLogViewable = this.isLogViewable,
        createdAt = this.createdAt
    )
}

fun EventLogEntity.toDto(): EventLogReqDto {
    return EventLogReqDto(
        serialNumber = this.serialNumber,
        taskId = this.taskId,
        lockPointId = this.lockPointId,
        // KT_001: 유니크키, KT_002: 마스터키
        keyType = if (this.issuanceId.contains("UK")) "KT_001" else "KT_002",
        issuanceId = this.issuanceId,
        resultStatus = this.resultCode,
        resultMessage = if (this.resultCode == "0000") "SUCCESS" else "FAIL",
        controlStatus = this.status,
        controlAt = this.eventAt,
        controlBy = this.controlUserId,
    )
}

fun LockStatusResDto.toLockPointStatus(): LockPointStatus {
    return LockPointStatus(
        lockPointId = this.lockPointId,
        controlStatus = this.controlStatus,
        controlStatusName = this.controlStatusName,
    )
}