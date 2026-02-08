package com.widthus.app.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.widthus.app.model.ProfileLoadResult
import com.widthus.app.model.UserInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.withus.app.model.ApiException
import org.withus.app.model.request.LoginRequest
import org.withus.app.model.request.PresignedUrlRequest
import org.withus.app.model.request.ProfileUpdateRequest
import org.withus.app.remote.ApiService
import org.withus.app.repository.AuthRepository
import org.withus.app.repository.CoupleRepository
import org.withus.app.repository.ProfileRepository
import org.withus.app.repository.UserRepository
import org.withus.app.token.TokenManager
import java.io.IOException
import javax.inject.Inject
import androidx.compose.runtime.State as ComposeState

// 공통 UI 상태 관리용 클래스
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val profileRepository: ProfileRepository,
    private val userRepository: UserRepository,
    private val coupleRepository: CoupleRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {


    private val _profileUpdated = MutableSharedFlow<Unit>()
    val profileUpdated: SharedFlow<Unit> = _profileUpdated.asSharedFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    var jwtToken = String()

    var currentUserInfo by mutableStateOf(UserInfo("Me"))
        private set

    var partnerUserInfo by mutableStateOf(UserInfo("Partner")) // 파트너 이름
        private set

    var partnerBirthdayValue by mutableStateOf(TextFieldValue("1998.05.12"))
    var partnerProfileUri by mutableStateOf<Uri?>(null)

    fun updateNickname(input: String) {
        currentUserInfo = currentUserInfo.copy(nickname = input)    }

    var isDisconnectSuccess by mutableStateOf(false)
        private set

    fun resetDisconnectStatus() {
        isDisconnectSuccess = false
    }

    private val _logoutState = mutableStateOf<UiState<Unit>>(UiState.Idle)
    val logoutState: ComposeState<UiState<Unit>> = _logoutState

    // 2단계: 생일
    var birthdayValue by mutableStateOf(TextFieldValue("")) // String 대신 TextFieldValue 사용
        private set


    fun logout() {
        viewModelScope.launch {
            _logoutState.value = UiState.Loading

            try {

                // Repository에서 서버 호출 + 로컬 토큰 삭제를 한 번에 처리
                getFcmToken()?.let {
                    val response = authRepository.logout(it)

                    if (response.isSuccessful && response.body()?.success == true) {
                        _logoutState.value = UiState.Success(Unit)
                        Log.d("Logout", "로그아웃 성공")
                    } else {
                        val errorMsg = response.body()?.error?.message ?: "로그아웃 실패"
                        _logoutState.value = UiState.Error(errorMsg)
                    }
                }

            } catch (e: Exception) {
                _logoutState.value = UiState.Error(e.message ?: "네트워크 오류 발생")
                Log.e("Logout", "오류", e)
            }
        }
    }


    suspend fun updateUserProfile(imageKey: String?, isUpdate: Boolean): ProfileLoadResult {
        return try {
            val rawBirthday = birthdayValue.text
            val formattedBirthday = if (rawBirthday.length == 8) {
                "${rawBirthday.substring(0, 4)}-${rawBirthday.substring(4, 6)}-${rawBirthday.substring(6, 8)}"
            } else {
                rawBirthday
            }

            // 2. 요청 객체 생성
            val request = ProfileUpdateRequest(
                nickname = currentUserInfo.nickname,
                birthday = formattedBirthday,
                imageKey = imageKey
            )

            val response = if (isUpdate) apiService.updateUserProfile(request) else apiService.uploadUserProfile(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val updatedData = response.body()?.data
                if (updatedData != null) {
                    // 수정된 정보를 다시 상태값에 동기화
                    currentUserInfo = currentUserInfo.copy(nickname =  updatedData.nickname)

                    ProfileLoadResult.Success
                } else {
                    ProfileLoadResult.Error("응답 데이터가 비어있습니다.")
                }
            } else {
                val errorMsg = response.body()?.error?.message ?: "수정에 실패했습니다."
                ProfileLoadResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            ProfileLoadResult.Error("네트워크 오류가 발생했습니다.")
        }
    }

    /**
     * 카카오 로그인 처리
     * NetworkModule에서 제공된 apiService를 사용합니다.
     */
    suspend fun handleKakaoLogin(token: String): Boolean {
        return try {
            // 1. 요청 객체 생성
            val request = LoginRequest(oauthToken = token, fcmToken = getFcmToken() ?: "")

            // 2. NetworkModule에서 주입받은 apiService 사용
            val response = apiService.login("kakao", request)

            // 3. 응답 처리
            if (response.isSuccessful && response.body()?.success == true) {
                val serverToken = response.body()?.data?.jwt
                Log.d("LOGIN", "서버 로그인 성공: $serverToken")

                if (serverToken != null) {
                    jwtToken = serverToken
                    tokenManager.saveAccessToken(serverToken)
                    true
                } else {
                    false
                }
            } else {
                Log.e("LOGIN", "로그인 실패: ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e("LOGIN", "네트워크 오류 발생", e)
            false
        }
    }

    suspend fun getUserProfile(): ProfileLoadResult {
        Log.d("UserProfile", "getUserProfile 시작")
        return try {
            val response = apiService.getUserProfile()
            Log.d("UserProfile", "getUserProfile 응답: $response")

            if (response.isSuccessful) {
                val commonResponse = response.body()

                if (commonResponse?.success == true) {
                    val profileData = commonResponse.data

                    if (profileData != null) {
                        // --- 1. UI 상태 변수 업데이트 ---
                        currentUserInfo = currentUserInfo.copy(profileData.nickname)

                        val pureBirthdayDigits = profileData.birthday.replace("-", "")
                        birthdayValue = TextFieldValue(
                            text = pureBirthdayDigits,
                            selection = TextRange(pureBirthdayDigits.length)
                        )
                        fetchCoupleProfile()

                        Log.d("PROFILE", "프로필 로드 성공 (홈 이동)")
                        ProfileLoadResult.Success
                    } else {
                        Log.d("PROFILE", "프로필 데이터 없음 (온보딩 이동)")
                        ProfileLoadResult.Error("프로필 데이터 없음")
                    }
                } else {
                    // 서버 내부 성공 플래그가 false인 경우 (예: 세션 만료 등)
                    val errorMessage = commonResponse?.error?.message ?: "알 수 없는 에러가 발생했습니다."
                    ProfileLoadResult.Error(errorMessage)
                }
            } else {
                // HTTP 에러 (4xx, 5xx)
                Log.d("PROFILE", "프로필 데이터 없음 (온보딩 이동)")
                ProfileLoadResult.Error("HTTP 에러")

//                val errorBody = response.errorBody()?.string()
//                Log.e("PROFILE", "HTTP 에러: $errorBody")
//                ProfileLoadResult.Error("서버와의 통신이 원활하지 않습니다.")
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "네트워크/런타임 오류", e)
            ProfileLoadResult.Error("네트워크 연결을 확인해 주세요.")
        }
    }


    fun changeProfile() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // 1) 이미지 업로드(있다면)
                val imageKey: String? = currentUserInfo.profileUrl?.let { uri ->
                    // uploadImageAndGetKey returns Result<String>
                    profileRepository.uploadImageAndGetKey(uri).getOrElse { throwable ->
                        // 업로드 실패 시 예외 던지거나 null로 처리
                        // 여기서는 에러로 처리
                        throw throwable
                    }
                }

                // 2) 프로필 요청 생성
                val formattedBirthday = formatBirthday(birthdayValue.text)
                val request = ProfileUpdateRequest(
                    nickname = currentUserInfo.nickname,
                    birthday = formattedBirthday,
                    imageKey = imageKey
                )

                // 3) 서버에 업데이트
                profileRepository.updateUserProfile(request).fold(
                    onSuccess = {
                        // 성공 처리: UI에 알림(예: 닫기, 토스트, 리프레시)
                        _profileUpdated.emit(Unit)
                    },
                    onFailure = { throwable ->
                        throw throwable
                    }
                )
            } catch (e: Exception) {
                val message = when (e) {
                    is ApiException -> e.message
                    is IOException -> "네트워크 오류가 발생했습니다."
                    else -> e.message ?: "알 수 없는 오류가 발생했습니다."
                }
                _error.emit(message)
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun uploadProfileAndSave(isUpdate: Boolean): ProfileLoadResult {
        return try {
            val currentUri = currentUserInfo.profileUrl ?: return ProfileLoadResult.Error("이미지가 선택되지 않았습니다.")
            val contentType = context.contentResolver.getType(currentUri) ?: "image/jpeg"

            // 단계 1: Pre-signed URL 발급 받기
            val urlResponse = apiService.getPresignedUrl(PresignedUrlRequest("PROFILE"))
            if (!urlResponse.isSuccessful || urlResponse.body()?.success != true) {
                return ProfileLoadResult.Error("업로드 URL 발급에 실패했습니다.")
            }

            val presignedData = urlResponse.body()!!.data!!
            val uploadUrl = presignedData.uploadUrl
            val imageKey = presignedData.imageKey

            // 단계 2: S3에 이미지 업로드 (PUT)
            // 로컬 Uri로부터 InputStream을 열어 RequestBody 생성
            val inputStream = context.contentResolver.openInputStream(currentUri)
            val byteArray = inputStream?.readBytes() ?: return ProfileLoadResult.Error("이미지를 읽을 수 없습니다.")
            val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())

            val s3Response = apiService.uploadImageToS3(uploadUrl, requestBody)
            if (!s3Response.isSuccessful) {
                return ProfileLoadResult.Error("S3 이미지 업로드에 실패했습니다.")
            }

            // 단계 3: 성공한 imageKey를 담아 프로필 수정 API 호출
            updateUserProfile(imageKey, isUpdate)

        } catch (e: Exception) {
            Log.e("UPLOAD", "전체 프로세스 오류", e)
            ProfileLoadResult.Error("처리 중 오류가 발생했습니다.")
        }
    }

    fun updateBirthday(input: TextFieldValue) {
        val originalText = input.text
        val digitsOnly = originalText.filter { it.isDigit() }
        val limitedDigits = if (digitsOnly.length > 8) digitsOnly.substring(0, 8) else digitsOnly

        val formatted = buildString {
            for (i in limitedDigits.indices) {
                append(limitedDigits[i])
                if ((i == 3 || i == 5) && i != limitedDigits.lastIndex) {
                    append("-")
                }
            }
        }

        birthdayValue = TextFieldValue(
            text = formatted,
            selection = TextRange(formatted.length)
        )
    }

    // helper: "YYYYMMDD" 또는 "YYYY-MM-DD" -> "YYYY-MM-DD"
    private fun formatBirthday(input: String): String {
        val digits = input.filter { it.isDigit() }
        return if (digits.length == 8) {
            "${digits.substring(0,4)}-${digits.substring(4,6)}-${digits.substring(6,8)}"
        } else {
            // 이미 포맷되어 있거나 비어있으면 그대로 반환
            input
        }
    }

    fun terminateCouple() {
        viewModelScope.launch {
            coupleRepository.terminateCouple()
                .onSuccess {
                    isDisconnectSuccess = true
                }
                .onFailure { error ->
                    Log.e("TERMINATE", "실패: ${error.message}")
                    // 에러 팝업이나 토스트 메시지 처리
                }
        }
    }

    fun fetchCoupleProfile() {
        viewModelScope.launch {
            try {
                val response = apiService.getCoupleProfile()

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        val profileData = body.data

                        // [중후한 업데이트] copy()를 사용해 불변성 유지하며 상태 갱신
                        currentUserInfo = currentUserInfo.copy(
                            nickname = profileData?.meProfile!!.nickname,
                            profileUrl = profileData.meProfile.profileImageUrl?.let { Uri.parse(it) } ?: Uri.EMPTY                        )

                        partnerUserInfo = partnerUserInfo.copy(
                            nickname = profileData.partnerProfile.nickname,
                            profileUrl = profileData.partnerProfile.profileImageUrl?.let { Uri.parse(it) } ?: Uri.EMPTY                        )

                        Log.d("AuthViewModel", "프로필 갱신 완료: ${currentUserInfo.nickname}")
                    }
                } else {
                    Log.e("AuthViewModel", "프로필 로드 실패: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "네트워크 오류 발생", e)
            }
        }
    }

    fun getFcmToken(): String? = tokenManager.fcmToken.value
}