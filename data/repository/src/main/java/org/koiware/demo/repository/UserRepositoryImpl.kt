package org.koiware.demo.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koiware.demo.CommonUtil.LOGIN_RESULT_TOKEN_INVALID
import org.koiware.demo.SharedPreferencesUtil
import org.koiware.demo.debug
import org.koiware.demo.domain.local.dao.UserDao
import org.koiware.demo.domain.loginDtoToDomain
import org.koiware.demo.interfaces.UserInfoRepository
import org.koiware.demo.domain.model.domain.UserInfo
import org.koiware.demo.domain.model.local.UserEntity
import org.koiware.demo.domain.model.remote.CommonResponse
import org.koiware.demo.domain.model.remote.UserInfoDto
import org.koiware.demo.domain.model.remote.request.PassWordChangeReqDto
import org.koiware.demo.domain.toUserInfo
import org.koiware.demo.remote.ApiService
import org.koiware.demo.request.LoginReqDto
import org.koiware.demo.domain.model.remote.safeApiCall
import org.koiware.demo.domain.toDomain
import org.koiware.demo.token.TokenManager
import javax.inject.Inject


class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) : UserInfoRepository {

    override var currentUserDto: MutableStateFlow<UserInfoDto?> = MutableStateFlow(null)
    override var currentUser: MutableStateFlow<UserInfo?> = MutableStateFlow(null)

    override suspend fun login(userId: String, userPw: String): Result<UserInfo> {
        val requestDto = LoginReqDto(userId, userPw, "app")
        return safeApiCall(
            call = { apiService.login(requestDto) }, // API 서비스 호출
            transform = { baseResponse ->
                currentUserDto.value = baseResponse.data?.userInfoDto
                currentUser.value = currentUserDto.value?.toDomain(baseResponse.data!!.initPassword)

                baseResponse.data?.let {
                    if (baseResponse.data?.initPassword == true) {
                        return@safeApiCall it.loginDtoToDomain()
                    }

                    val accessToken = baseResponse.data?.accessToken
                        ?: throw IllegalStateException(LOGIN_RESULT_TOKEN_INVALID)

                    tokenManager.saveAccessToken(accessToken)

                    return@safeApiCall it.loginDtoToDomain()
                } ?: run {
                    throw IllegalStateException("로그인 데이터 없음")
                }
            },
            onCookieReceived = { headers ->
                val serverSettings = SharedPreferencesUtil.loadServerSettings()
                tokenManager.saveRefreshTokenToWebView(
                    headers,
                    SharedPreferencesUtil.buildAppBaseUrl(serverSettings)
                )
            },
        )
    }

    override suspend fun saveUserData(userEntity: UserEntity?, pinCode: String): Result<Unit> {
        return try {
            userEntity?.let { currentUserInfo ->
                val existingCount =
                    userDao.countOtherUsersWithSpecificPinCode(currentUserInfo.loginId, pinCode)

                if (existingCount > 0) {
                    // 다른 사용자가 동일한 핀 코드를 사용 중인 경우 실패 반환
                    debug("LoginRepositoryImpl", "다른 사용자가 이미 핀 코드 '$pinCode'를 사용 중입니다.")
                    return Result.failure(IllegalStateException("다른 사용자가 이미 동일한 핀 코드를 사용 중입니다."))
                }

                // 2. 다른 사용자가 사용 중이 아니라면, 현재 사용자 데이터 저장 또는 업데이트
                //    UserEntity의 pinCode는 Non-Nullable이므로 String 값이 항상 제공되어야 합니다.
                userDao.insertUser(userEntity) // onConflict = REPLACE이므로 기존 사용자라면 업데이트, 없다면 삽입
                debug(
                    "LoginRepositoryImpl",
                    "사용자 ${currentUserInfo.loginId}의 핀 코드 저장 성공. \n ${userEntity}"
                )
                Result.success(Unit) // 성공적으로 저장했음을 알림
            } ?: Result.failure(IllegalArgumentException("저장할 UserInfo가 null입니다."))
        } catch (e: Exception) {
            Log.e("LoginRepositoryImpl", "핀 코드 저장 중 오류 발생", e)
            Result.failure(e)
        }
    }

    override suspend fun checkIfPinCodeExists(pinCode: String): Result<UserInfo> {
        val userEntity = userDao.getUserByPinCode(pinCode)
        return userEntity?.let {
            debug("currentUser.value : ${currentUser.value }")
            currentUser.value = userEntity.toUserInfo()
            Result.success(userEntity.toUserInfo())
        } ?: Result.failure(IllegalStateException(""))
    }

    override suspend fun getAccessToken(): Flow<String?> {
        return tokenManager.getAccessToken()
    }

    override suspend fun saveAccessToken(token: String) {
        tokenManager.saveAccessToken(token)
    }

    override suspend fun changePassword(passWordChangeReqDto: PassWordChangeReqDto): Result<CommonResponse<Unit>> {
        return safeApiCall(
            call = { apiService.changePassWord(passWordChangeReqDto) }, // API 서비스 호출
            transform = { baseResponse ->
                baseResponse
            },
        )
    }

    override suspend fun isPinCodeExist(): Boolean {
        val users = userDao.getAllUsers()
        return users.any { user ->
            debug("user : $user")
            user.pinCode.isNotEmpty()
        }
    }
}