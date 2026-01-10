package org.koiware.demo.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koiware.demo.CommonUtil
import org.koiware.demo.KeyStoreManager
import org.koiware.demo.crypto.ServerAESCipherHelper.getDecryptDataKey
import org.koiware.demo.debug
import org.koiware.demo.domain.local.dao.LockPointDao
import org.koiware.demo.domain.local.dao.MasterKeyDao
import org.koiware.demo.domain.local.dao.TaskAuthDao
import org.koiware.demo.domain.local.dao.TaskDao
import org.koiware.demo.domain.local.dao.UniqueKeyDao
import org.koiware.demo.domain.model.domain.LockPoint
import org.koiware.demo.domain.model.domain.LockPointStatus
import org.koiware.demo.domain.model.domain.MasterKey
import org.koiware.demo.domain.model.domain.Task
import org.koiware.demo.domain.model.domain.TaskAuth
import org.koiware.demo.domain.model.domain.UniqueKey
import org.koiware.demo.domain.model.local.LockPointEntity
import org.koiware.demo.domain.model.local.MasterKeyEntity
import org.koiware.demo.domain.model.local.TaskAuthEntity
import org.koiware.demo.domain.model.local.TaskEntity
import org.koiware.demo.domain.model.local.UniqueKeyEntity
import org.koiware.demo.domain.model.remote.TaskRelatedDto
import org.koiware.demo.interfaces.TaskRepository
import org.koiware.demo.remote.ApiService
import org.koiware.demo.domain.model.remote.safeApiCall
import org.koiware.demo.domain.toDomain
import org.koiware.demo.domain.toLockPointStatus
import org.koiware.demo.domain.toTaskDomain
import org.koiware.demo.errorLog
import org.koiware.demo.interfaces.UserInfoRepository
import org.koiware.demo.warn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val taskDao: TaskDao,
    private val taskAuthDao: TaskAuthDao,
    private val lockPointDao: LockPointDao,
    private val uniqueKeyDao: UniqueKeyDao,
    private val masterKeyDao: MasterKeyDao,
    private val keyStoreManager: KeyStoreManager,
    private val userInfoRepository: UserInfoRepository,
) : TaskRepository {

    override suspend fun downloadMasterKey(taskId: Long): Result<MasterKey?> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.downloadMasterKey(taskId) // Response<ResponseBody>를 받음

                if (response.isSuccessful) {
                    val responseBody = response.body()

                    if (responseBody == null) {
                        errorLog("응답 바디가 비어있습니다.")
                        return@withContext Result.failure(IllegalStateException("응답 바디가 비어있습니다."))
                    } else if (responseBody.responseCode != "0000") {
                        return@withContext Result.failure(IllegalStateException(responseBody.responseMessage))
                    } else {
                        val masterKeyContent = responseBody.data?.encData
                        val issuedUserId = responseBody.data?.issuanceId

                        try {
                            val decryptedData = getDecryptDataKey(masterKeyContent!!)

                            debug("final decryptedData : $decryptedData")

                            debug("responseBody : $responseBody")
                            debug("AuthRepositoryImpl", "마스터 키 다운로드 성공: $masterKeyContent")

                            val savedFilePath = keyStoreManager.saveKeyToInternalStorage(
                                masterKeyContent, issuedUserId!!
                            )

                            var pendingMasterKey = MasterKeyEntity(
                                keyId = issuedUserId,
                                taskId = taskId,
                                path = savedFilePath,
                                expiredAt = decryptedData.expired,
                                issuedUserId = userInfoRepository.currentUser.value?.loginId ?: "",
                                createdAt = CommonUtil.getCurrentTime()
                            )

                            masterKeyDao.insertMasterKey(pendingMasterKey)
                            return@withContext Result.success(pendingMasterKey.toDomain())
                        } catch (e: Exception) {
                            val errorMessage = when (e) {
                                is IllegalArgumentException -> {
                                    "유효하지 않은 인자 오류: ${e.message}"
                                }

                                is javax.crypto.AEADBadTagException -> {
                                    "인증 태그 오류 (키, IV 또는 데이터 무결성 확인 필요): ${e.message}"
                                }

                                else -> {
                                    "예상치 못한 복호화 오류: ${e.localizedMessage}"
                                }
                            }
                            errorLog("[DecryptionUtil] 복호화 오류 발생: $errorMessage")
                            // 여기에서 오류 상태를 반환하거나 다른 오류 처리 로직을 실행합니다.
                            Result.failure(Exception(errorMessage))
                        }
                    }
                } else {
                    // HTTP 오류 (2xx가 아닌 경우, 예: 401 Unauthorized, 500 Internal Server Error)
                    val errorBody = response.errorBody()?.string()
                    val errorMessage =
                        "HTTP 오류: ${response.code()} - ${errorBody ?: "알 수 없는 HTTP 오류"}"
                    Log.e("AuthRepositoryImpl", errorMessage)
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                // 네트워크 연결 문제, I/O 오류, 기타 예외 처리
                val errorMessage = "네트워크 또는 마스터 키 다운로드 처리 중 오류 발생: ${e.message ?: "알 수 없는 오류"}"
                Log.e("AuthRepositoryImpl", errorMessage, e)
                Result.failure(e)
            }
        }

    override suspend fun downloadUniqueKey(
        progressId: String?,
        lockPointId: Long,
        isVlockKey: Boolean,
        keyType: String,
        taskId: Long,
        isEnabled: Boolean,
    ): Result<UniqueKey> {
        debug("downloadUniqueKey !")

        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.downloadUniqueKey(
                    taskId, progressId, lockPointId, isVlockKey, keyType
                )

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody == null) {
                        errorLog("응답 바디가 비어있습니다.")
                        return@withContext Result.failure(IllegalStateException("응답 바디가 비어있습니다."))
                    } else if (responseBody.responseCode != "0000") {
                        return@withContext Result.failure(IllegalStateException(responseBody.responseMessage))
                    }

                    val uniqueKeyString = responseBody.data?.encData
                    val issuedUserId = responseBody.data?.issuanceId

                    try {
                        val decryptedData = getDecryptDataKey(uniqueKeyString!!)
                        debug("final decryptedData : $decryptedData")

                        val savedFilePath = keyStoreManager.saveKeyToInternalStorage(
                            uniqueKeyString, issuedUserId!!
                        )

                        var pendingUniqueKey: UniqueKeyEntity?

                        pendingUniqueKey = uniqueKeyDao.getUniqueKeyByKeyId(decryptedData.cid)
                        pendingUniqueKey?.let {
                            debug("pendingUniqueKey is already exist")
                            val uniqueKeyToSave: UniqueKeyEntity = it.copy(
                                path = savedFilePath, issuedUserId = issuedUserId
                            )
                            if (uniqueKeyToSave.keyId.isEmpty()) {
                                uniqueKeyToSave.expiredAt = decryptedData.expired
                            }

                            uniqueKeyToSave.let {
                                debug("saved unique key info : $uniqueKeyToSave")
                                uniqueKeyDao.insertUniqueKey(uniqueKeyToSave)
                                debug(
                                    "save result : ${
                                        uniqueKeyDao.getAllUniqueKeys().joinToString()
                                    }}"
                                )
                            }
                        } ?: run {
                            debug("pendingUniqueKey is null")
                            pendingUniqueKey =
                                lockPointDao.getLockPointByTaskIdAndById(lockPointId, taskId)?.let {
                                    UniqueKeyEntity(
                                        keyId = issuedUserId,
                                        taskId = taskId,
                                        keyType = keyType,
                                        path = savedFilePath,
                                        lockPointId = lockPointId,
                                        lockSerialNumber = decryptedData.serialNumber,
                                        dangerLevel = it.dangerLevel,
                                        isEnabled = isEnabled,
                                        isVlockKey = isVlockKey,
                                        expiredAt = decryptedData.expired,
                                        issuedUserId = userInfoRepository.currentUser.value?.loginId
                                            ?: "",
                                        createdAt = CommonUtil.getCurrentTime()
                                    )
                                }
                            pendingUniqueKey?.let {
                                debug("saved unique key info : $pendingUniqueKey")
                                uniqueKeyDao.insertUniqueKey(it)
                            }
                        }

                        return@withContext pendingUniqueKey?.let {
                            Result.success(it.toDomain())
                        } ?: run {
                            Result.failure(Exception("App DB Error"))
                        }
                    } catch (e: Exception) {
                        val errorMessage = when (e) {
                            is IllegalArgumentException -> {
                                "유효하지 않은 인자 오류: ${e.message}"
                            }

                            is javax.crypto.AEADBadTagException -> {
                                "인증 태그 오류 (키, IV 또는 데이터 무결성 확인 필요): ${e.message}"
                            }

                            else -> {
                                "예상치 못한 복호화 오류: ${e.localizedMessage}"
                            }
                        }
                        errorLog("[DecryptionUtil] 복호화 오류 발생: $errorMessage")
                        Result.failure(Exception(e))
                    }
                } else {
                    errorLog("다운로드 실패: ${response.code()} ${response.message()}")
                    Result.failure(RuntimeException("다운로드 실패: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                errorLog("유니크 키 다운로드 중 오류 발생: ${e.localizedMessage}")
                Result.failure(e)
            }
        }
    }

    override suspend fun updateUniqueKeyEnabledStatus(
        lockPointId: Long, isEnabled: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            debug("insertUniqueKey updateUniqueKeyEnabledStatus !")
            uniqueKeyDao.updateIsEnabled(lockPointId, isEnabled)
            debug("UniqueKeyRepo", "lockPointId: $lockPointId 의 isEnabled 상태를 $isEnabled 로 업데이트 성공")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(
                "UniqueKeyRepo", "lockPointId: $lockPointId 의 isEnabled 상태 업데이트 실패: ${e.message}", e
            )
            Result.failure(e)
        }
    }

    override suspend fun returnMasterKey(taskId: Long): Result<Unit> {
        val masterKey =
            masterKeyDao.getMasterKey(taskId, userInfoRepository.currentUser.value?.loginId!!)
        masterKey?.let {
            val requestBodyMap = mapOf("keyId" to masterKey.keyId)

            val result = safeApiCall(
                call = { apiService.returnMasterKey(requestBodyMap) },
                transform = { response ->
                })

            result.onFailure { exception ->
                Log.e("KeyManager", "${masterKey.keyId} 반환 실패: ${exception.message}", exception)
                return Result.failure(exception)
            }
            masterKeyDao.deleteMasterKey(masterKey)
            debug("KeyManager", "${masterKey.keyId} 반환 성공")
            debug("모든 MasterKey 반환 작업 성공")
            return Result.success(Unit)
        } ?: run {
            return Result.failure(Exception("반납 할 키가 없습니다."))
        }
    }

    override suspend fun returnUniqueKey(lockPointId: Long, taskId: Long): Result<Unit> {
        val uniqueKeyList = uniqueKeyDao.getUniqueKeysByTaskIdAndLockPointId(taskId, lockPointId)
        debug("returnUniqueKey ")
        var isAllDeleteSuccess = true
        if (uniqueKeyList.isEmpty()) {
            debug("uniqueKeyList.isEmpty() ")
            isAllDeleteSuccess = false
        } else {
            for (uniqueKey in uniqueKeyList) {
                if (uniqueKey.path.isEmpty()) {
                    debug("uniqueKey.path.isEmpty() : ${uniqueKey.keyId}")
                    isAllDeleteSuccess = false
                }
                uniqueKeyDao.deleteUniqueKey(uniqueKey)
                debug("KeyManager", "${uniqueKey.keyId} 반환 성공")
            }
        }
        debug("KeyManager", "isAllDeleteSuccess : ${isAllDeleteSuccess} ")
        return if (isAllDeleteSuccess) Result.failure(Exception("해당 키가 없습니다")) else Result.success(
            Unit
        )
    }

    override suspend fun getTasksFromLocal(loginId: String): Result<List<Task>> {
        return Result.success(taskDao.getTasksByLoginId(loginId).map {
            it.toTaskDomain(taskAuthDao.getTaskAuthByLoginIdAndTaskId(loginId, it.taskId)?.taskRole ?: "")
        })
    }

    override suspend fun getTaskRelatedFromRemote(taskId: Long): Result<TaskRelatedDto> {
        val requestBodyMap = mapOf("taskId" to taskId)
        return safeApiCall(
            call = { apiService.fetchTaskRelated(requestBodyMap) },
            // 여기서 transform 람다를 추가하여 CommonResponse<TaskDetail>에서 TaskDetail을 추출합니다.
            transform = { baseResponse ->
                baseResponse.data
                    ?: throw IllegalStateException("TaskRelatedDto data is null in response")
            })
    }

    override suspend fun getTaskFromLocal(
        loginId: String,
        taskId: Long
    ): Result<Task?> {
        return Result.success(taskAuthDao.getTaskAuthByLoginIdAndTaskId(loginId, taskId)?.let {
            return Result.success(taskDao.getTaskByLoginIdAndTaskId(loginId, taskId)?.toTaskDomain(it.taskRole))
        })
    }

    override suspend fun getTaskAuthFromLocal(loginId: String, taskId: Long): Result<TaskAuth?> {
        debug("getTaskAuthFromLocal : loginId:$loginId, taskId:$taskId")
        return Result.success(
            taskAuthDao.getTaskAuthByLoginIdAndTaskId(loginId, taskId)?.toDomain()
        )
    }

    override suspend fun getLockPointListFromLocal(taskId: Long): Result<List<LockPoint>> {
        debug("taskId : $taskId")
        return Result.success(lockPointDao.getLockPointsByTaskId(taskId).map {
            debug("getLockPointListFromLocal : $it")
            it.toDomain()
        })
    }

    override suspend fun getLockPointFromLocal(lockPointId: Long, taskId: Long): LockPoint? {
        return lockPointDao.getLockPointByTaskIdAndById(lockPointId, taskId)?.toDomain()
    }

    override suspend fun getLockPointStatus(lockPointList: List<LockPoint>): List<LockPointStatus> {
        debug("getLockPointStatus ! : ${lockPointList.joinToString()}")
        val lockPointIds = lockPointList.map { it.lockPointId }

        val result = safeApiCall(
            call = { apiService.getLockStatus(lockPointIds) },
            transform = { response ->
                response.list?.map {
                    it.toLockPointStatus()
                }
            })

        return result.getOrNull() ?: listOf()
    }

    override suspend fun getUniqueKeyListFromLocal(taskId: Long): Result<List<UniqueKey>> {
        return Result.success(uniqueKeyDao.getUniqueKeysByTaskId(taskId).map {
            it.toDomain()
        })
    }

    override suspend fun getUniqueKeyFromLocal(lockPointId: Long): Result<UniqueKey> {
        return try {
            val uniqueKeyEntity = uniqueKeyDao.getUniqueKeysByLockPointId(lockPointId).firstOrNull()

            if (uniqueKeyEntity != null) {
                Result.success(uniqueKeyEntity.toDomain())
            } else {
                val errorMessage = "LockPoint ID $lockPointId 에 해당하는 UniqueKey를 찾을 수 없습니다."
                Log.e("TaskRepositoryImpl", errorMessage)
                Result.failure(NoSuchElementException(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = "UniqueKey를 로컬에서 가져오는 중 오류 발생: ${e.message}"
            Log.e("TaskRepositoryImpl", errorMessage, e)
            Result.failure(e)
        }
    }

    override suspend fun getMasterKeyFromLocal(taskId: Long, loginId: String): Result<MasterKey?> {
        debug("getMasterKeyFromLocal : taskId : $taskId")
        return Result.success(masterKeyDao.getMasterKey(taskId, loginId)?.apply {
        }?.toDomain())
    }

    override suspend fun saveTaskListLocally(tasks: List<TaskEntity>) {
        taskDao.insertAll(tasks)
    }

    override suspend fun saveTaskAuth(taskAuthEntity: TaskAuthEntity) {
        userInfoRepository.currentUser.value?.loginId?.let { loginId ->
            val existingTask = taskDao.getTaskByLoginIdAndTaskId(loginId, taskAuthEntity.taskId)
            existingTask?.let { task ->
                val updatedTask =
                    task.copy(lastSyncedAt = CommonUtil.getCurrentTime(), loginId = loginId)
                debug("loginId : $loginId")
                debug("update sync time : $task")
                debug("update sync time : $updatedTask")
                taskDao.updateTask(updatedTask)
            }
        }
        taskAuthDao.insertTaskAuth(taskAuthEntity)
    }

    override suspend fun synchronizeUniqueKeysForLockPoints(
        lockPoints: List<LockPointEntity>, uniqueKeyList: List<UniqueKeyEntity>
    ) {
        val lockPointIds = lockPoints.map { it.lockPointId }.toSet()
        debug("lockPointIds : ${lockPointIds.joinToString()}")
        uniqueKeyList.forEach { uniqueKey ->
            debug("uniqueKey.lockPointId : ${uniqueKey.lockPointId}}")

            if (!lockPointIds.contains(uniqueKey.lockPointId)) {
                warn(
                    "uniqueKey ID '${uniqueKey.lockPointId}' does not have a corresponding UniqueKey in the provided uniqueKeyList."
                )

                val dbUniqueKeys = uniqueKeyDao.getUniqueKeysByTaskIdAndLockPointId(
                    uniqueKey.taskId, uniqueKey.lockPointId
                )
                dbUniqueKeys.forEach { deletedUniqueKey ->
                    debug("deleteUniqueKey ! : $deletedUniqueKey")
                    uniqueKeyDao.deleteUniqueKey(deletedUniqueKey)
                }
            }
        }
    }

    override suspend fun synchronizeUniqueKeysForTask(
        taskId: Long,
        userId: String,
        newUniqueKeyList: List<UniqueKeyEntity>
    ) {
        // 1. 기존 로컬 키 조회
        val localKeyMap: Map<String, UniqueKeyEntity> = uniqueKeyDao
            .getUniqueKeysByTaskIdAndUserId(taskId, userId)
            .associateBy { it.keyId }

        // 2. new key list 맵 생성
        val newKeyMap: Map<String, UniqueKeyEntity> = newUniqueKeyList
            .associateBy { it.keyId }

        debug("localKeyMap : $localKeyMap")
        debug("newKeyMap : $newKeyMap")

        // 3. 삭제할 키: 새로운 목록에 없는 로컬 키
        val keysToDelete = localKeyMap.values.filter { localKey ->
            !newKeyMap.containsKey(localKey.keyId)
        }
        keysToDelete.forEach {
            debug("delete unique key: $it")
            uniqueKeyDao.deleteUniqueKey(it)
            keyStoreManager.deleteKeyFile(it.path)
        }

        // 4. 업데이트
        newUniqueKeyList.forEach { newKey ->
            val localKey = localKeyMap[newKey.keyId]
            localKey?.let {
                val updatedKey = newKey.copy(path = localKey.path, isEnabled = localKey.isEnabled)
                debug("update unique key: $updatedKey")

                uniqueKeyDao.updateUniqueKey(updatedKey)
            }
        }
    }

    override suspend fun synchronizeMasterKeysForTask(
        taskId: Long,
        newMasterKeyEntity: MasterKeyEntity?
    ) {

        // user id 와 task id로 조회
        val localMasterKey =
            masterKeyDao.getMasterKey(
                taskId,
                newMasterKeyEntity?.issuedUserId ?: userInfoRepository.currentUser.value?.loginId
                ?: ""
            )

        debug("localMasterKey : $localMasterKey")
        debug("newMasterKeyEntity : $newMasterKeyEntity")

        newMasterKeyEntity?.let {
            // master key id가 달라졌다면 다른 유저에서 받았다고 판단하여 삭제
            if (localMasterKey != null && localMasterKey.keyId != newMasterKeyEntity.keyId
                && newMasterKeyEntity.taskId == localMasterKey.taskId
            ) {
                masterKeyDao.deleteMasterKey(localMasterKey)
                debug("기존 KeyId 불일치로 로컬 마스터 키 삭제: ${localMasterKey.keyId}");
            }
        } ?: run {
            // 동기화 마스터키가 없으면 local master key 삭제
            localMasterKey?.let {
                masterKeyDao.deleteMasterKey(it)
            }
        }
    }


    override suspend fun saveLockPointList(lockPoints: List<LockPointEntity>) {
        lockPoints.forEach {
            debug("save lockPoints : $it")
        }
        lockPointDao.insertAll(lockPoints)
    }

    override suspend fun saveUniqueKeyList(uniqueKeyList: List<UniqueKeyEntity>) {
        uniqueKeyList.forEach { uniqueKeyEntity ->
            val saveUniqueKey = uniqueKeyDao.getUniqueKeyByKeyId(uniqueKeyEntity.keyId)

            if (saveUniqueKey != null) {
                debug("saveUniqueKey : $saveUniqueKey")
                debug("uniqueKeyEntity : ${uniqueKeyEntity}")
                uniqueKeyEntity.path = saveUniqueKey.path
                uniqueKeyEntity.isEnabled = saveUniqueKey.isEnabled
            }
            debug("insertUniqueKey insertUniqueKey !")
            uniqueKeyDao.insertUniqueKey(uniqueKeyEntity)
        }
    }

}
