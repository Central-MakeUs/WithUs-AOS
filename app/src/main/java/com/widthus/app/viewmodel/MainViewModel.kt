package com.widthus.app.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.widthus.app.model.MemoryItem
import com.widthus.app.model.ScheduleItem
import com.widthus.app.model.OnboardingPage
import com.widthus.app.model.ProfileLoadResult
import com.widthus.app.screen.Screen
import com.widthus.app.utils.PreferenceManager
import com.withus.app.R
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
import org.withus.app.model.ArchiveDateGroup
import org.withus.app.model.ArchiveQuestionItem
import org.withus.app.model.CalendarDayInfo
import org.withus.app.model.CoupleQuestionData
import org.withus.app.model.JoinCouplePreviewData
import org.withus.app.model.JoinCoupleRequest
import org.withus.app.model.KeywordInfo
import org.withus.app.model.KeywordUpdateRequest
import org.withus.app.model.ProfileSettingStatus
import org.withus.app.model.QuestionDetailResponse
import org.withus.app.model.UserAnswerInfo
import org.withus.app.token.TokenManager
import org.withus.app.model.request.LoginRequest
import org.withus.app.model.request.PresignedUrlRequest
import org.withus.app.model.request.ProfileUpdateRequest
import org.withus.app.remote.ApiService
import org.withus.app.repository.ArchiveRepository
import org.withus.app.repository.CoupleRepository
import org.withus.app.repository.DailyRepository
import org.withus.app.repository.ProfileRepository
import org.withus.app.repository.UserRepository
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val apiService: ApiService,
    private val profileRepository: ProfileRepository,
    private val dailyRepository: DailyRepository,
    private val userRepository: UserRepository,
    private val coupleRepository: CoupleRepository,
    private val tokenManager: TokenManager,
    private val preferenceManager: PreferenceManager,
    private val archiveRepository: ArchiveRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _profileUpdated = MutableSharedFlow<Unit>()
    val profileUpdated: SharedFlow<Unit> = _profileUpdated.asSharedFlow()

    var isDisconnectSuccess by mutableStateOf(false)
        private set

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

    fun resetDisconnectStatus() {
        isDisconnectSuccess = false
    }

    private val _myCode = MutableStateFlow<String?>(null)
    val myCode: StateFlow<String?> = _myCode.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    var isOnboardingComplete by mutableStateOf(false)
        private set
    // 1단계: 닉네임
    var nickname by mutableStateOf("jpg") // 기본값 설정
        private set


    var partnerNickname by mutableStateOf("쏘피") // 파트너 이름
        private set

    // 탈퇴 성공 여부를 알리는 이벤트 (UI에서 관찰하여 로그아웃 처리)
    var isDeleteAccountSuccess by mutableStateOf(false)
        private set

    fun deleteAccount() {
        viewModelScope.launch {
            userRepository.deleteAccount()
                .onSuccess {
                    // 탈퇴 성공
                    isDeleteAccountSuccess = true
                }
                .onFailure { error ->
                    // 에러 발생 시 토스트 메시지 등을 위한 처리
                    Log.e("DELETE_ACCOUNT", "실패: ${error.message}")
                }
        }
    }

    private val _memorySets = mutableStateListOf<CoupleQuestionData>()
    val memorySets: List<CoupleQuestionData> = _memorySets

    // 가입 날짜 (가입일로부터 오늘까지 계산하거나 고정값 사용)
    val joinDate = "2024년 10월 6일"

    var isConnect = false

    var jwtToken = String()

    init {
        // 앱 실행 시 저장된 값들을 로드
        viewModelScope.launch {
            preferenceManager.isOnboardingComplete.collect { savedStatus ->
                isOnboardingComplete = savedStatus
            }
        }

        loadDummyMemories()
    }

    fun updateNickname(input: String) { nickname = input }

    // 2단계: 생일
    var birthdayValue by mutableStateOf(TextFieldValue("")) // String 대신 TextFieldValue 사용
        private set

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

    var partnerBirthdayValue by mutableStateOf(TextFieldValue("1998.05.12"))
    var partnerProfileUri by mutableStateOf<Uri?>(null)

    // 3단계: 첫 만남 기념일
    var anniversaryDate by mutableStateOf("")
        private set
    fun updateAnniversaryDate(input: String) {
        if (input.length <= 8 && input.all { it.isDigit() }) {
            anniversaryDate = input
        }
    }

    // 4단계: 프로필 이미지 URI
    var profileImageUri by mutableStateOf<Uri?>(null)

//    var userUploadedImage by mutableStateOf<Uri?>(null)
    var userUploadedImage by mutableStateOf<Uri?>(
        Uri.parse("https://img1.daumcdn.net/thumb/R1280x0.fjpg/?fname=http://t1.daumcdn.net/brunch/service/user/1dEO/image/CIieqAqy0KlR6UdFHxrc1NsGtVM.jpg")
    )

//    var partnerUploadedImage by mutableStateOf<Uri?>(
//        Uri.parse("https://img1.daumcdn.net/thumb/R1280x0.fjpg/?fname=http://t1.daumcdn.net/brunch/service/user/1dEO/image/CIieqAqy0KlR6UdFHxrc1NsGtVM.jpg")
//    )
    var partnerUploadedImage by mutableStateOf<Uri?>(null)
//
    fun updateProfileImage(uri: Uri?) {
        profileImageUri = uri
    }


    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    // 1. 일정 더미 데이터
    val dummySchedules = listOf(
        ScheduleItem(1, "쏘피와 점심 데이트", "12:30"),
        ScheduleItem(2, "영화 '파묘' 예매", "19:00"),
        ScheduleItem(3, "기념일 케이크 픽업", "18:00")
    )

    // 2. 사진 더미 데이터
    val dummyMemories = listOf(
        // 지금은 임시로 안드로이드 기본 아이콘을 넣었습니다.
        // 실제 사용시: MemoryItem(1, R.drawable.image_01),
        MemoryItem(1, R.drawable.dummy_couple_1),
        MemoryItem(2, R.drawable.dummy_couple_2),
        MemoryItem(3, R.drawable.dummy_couple_3),
    )

    val onboardingPages = listOf(
        OnboardingPage("매일 발송되는 랜덤 질문", "주어진 질문에 사진 한 장으로\n서로의 마음을 확인해요"),
        OnboardingPage("사진으로 일상을 함께", "쌓여가는 둘만의 사진 기록을\n한눈에 확인해요"),
        OnboardingPage("우리 취향대로 커플네컷", "원하는 사진으로\n둘만의 인생 네컷을 만들어봐요"),
    )

    fun updateSelectedImage(uri: Uri?) {
        selectedImageUri = uri
    }

    /**
     * 카카오 로그인 처리
     * NetworkModule에서 제공된 apiService를 사용합니다.
     */
    suspend fun handleKakaoLogin(token: String): Boolean {
        return try {
            // 1. 요청 객체 생성
            val request = LoginRequest(oauthToken = token)

            // 2. NetworkModule에서 주입받은 apiService 사용
            val response = apiService.login("kakao", request)

            // 3. 응답 처리
            if (response.isSuccessful && response.body()?.success == true) {
                val serverToken = response.body()?.data?.jwt
                Log.d("LOGIN", "서버 로그인 성공: $serverToken")

                if (serverToken != null) {
                    jwtToken = serverToken
                    // TokenManager를 통해 토큰을 저장 (Interceptor에서 사용됨)
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

    suspend fun uploadProfileAndSave(isUpdate: Boolean): ProfileLoadResult {
        return try {
            val currentUri = profileImageUri ?: return ProfileLoadResult.Error("이미지가 선택되지 않았습니다.")
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
                nickname = nickname,
                birthday = formattedBirthday,
                imageKey = imageKey
            )

            val response = if (isUpdate) apiService.updateUserProfile(request) else apiService.uploadUserProfile(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val updatedData = response.body()?.data
                if (updatedData != null) {
                    // 수정된 정보를 다시 상태값에 동기화
                    nickname = updatedData.nickname

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
                        nickname = profileData.nickname

                        val pureBirthdayDigits = profileData.birthday.replace("-", "")
                        birthdayValue = TextFieldValue(
                            text = pureBirthdayDigits,
                            selection = TextRange(pureBirthdayDigits.length)
                        )

                        profileImageUri = if (!profileData.profileImageUrl.isNullOrEmpty()) {
                            Uri.parse(profileData.profileImageUrl)
                        } else {
                            null
                        }

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

    suspend fun fetchUserStatus(): Result<ProfileSettingStatus> {
        return runCatching {
            val response = apiService.getUserStatus()
            if (!response.isSuccessful) {
                val body = response.errorBody()?.string().orEmpty()
                throw ApiException(response.code(), body.ifBlank { "HTTP ${response.code()}" })
            }
            val body = response.body() ?: throw ApiException(response.code(), "Empty body")
            if (body.success != true || body.data == null) {
                val msg = body.error?.message ?: "Unknown API error"
                throw ApiException(response.code(), msg)
            }
            body.data!!.profileSettingStatus
        }
    }


    suspend fun checkUserStatus(): ProfileSettingStatus {
        // 성공이면 status 반환, 실패면 예외가 호출자에게 전달됨
        return fetchUserStatus().getOrThrow()
    }


    fun logout() {
    }

    var deleteStep by mutableIntStateOf(1)
        private set

    fun updateDeleteStep(step: Int) {
        deleteStep = step
    }

    fun changeProfile() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // 1) 이미지 업로드(있다면)
                val imageKey: String? = profileImageUri?.let { uri ->
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
                    nickname = nickname,
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

    fun completeOnboarding() {
        viewModelScope.launch {
            preferenceManager.updateOnboardingStatus(true)
        }
    }

    private fun loadDummyMemories() {
        val dummyData = listOf(
            CoupleQuestionData(
                coupleQuestionId = 505,
                question = "상대가 가장 사랑스러워 보였던 순간은 언제인가요?",
                myInfo = UserAnswerInfo(
                    userId = 123,
                    name = "김철수",
                    profileThumbnailImageUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200", // 테스트 이미지
                    questionImageUrl = "https://images.unsplash.com/photo-1518173946687-a4c8892bbd9f?w=800",
                    answeredAt = "20:30"
                ),
                partnerInfo = UserAnswerInfo(
                    userId = 456,
                    name = "쏘피",
                    profileThumbnailImageUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                    questionImageUrl = "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=800",
                    answeredAt = "21:15"
                )
            ),
            CoupleQuestionData(
                coupleQuestionId = 504,
                question = "함께 간 여행지에서 찍은 사진은?\n혹은 가고 싶은 여행지?",
                myInfo = UserAnswerInfo(
                    userId = 123,
                    name = "김철수",
                    profileThumbnailImageUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
                    questionImageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800",
                    answeredAt = "PM 12:30"
                ),
                partnerInfo = UserAnswerInfo(
                    userId = 456,
                    name = "쏘피",
                    profileThumbnailImageUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                    questionImageUrl = "https://images.unsplash.com/photo-1473448912268-2022ce9509d8?w=800",
                    answeredAt = "PM 01:10"
                )
            ),
            CoupleQuestionData(
                coupleQuestionId = 503,
                question = "우리가 처음 만난 날, 상대방의 첫인상은?",
                myInfo = UserAnswerInfo(
                    userId = 123,
                    name = "김철수",
                    profileThumbnailImageUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
                    questionImageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800",
                    answeredAt = "어제"
                ),
                partnerInfo = UserAnswerInfo(
                    userId = 456,
                    name = "쏘피",
                    profileThumbnailImageUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                    questionImageUrl = "https://images.unsplash.com/photo-1522071823991-b1ae5e3a7c8e?w=800",
                    answeredAt = "어제"
                )
            )
        )

        _memorySets.clear()
        _memorySets.addAll(dummyData)
    }

    fun isValidDate(dateStr: String): Boolean {
        if (dateStr.length != 8) return false
        return try {
            val year = dateStr.substring(0, 4).toInt()
            val month = dateStr.substring(4, 6).toInt()
            val day = dateStr.substring(6, 8).toInt()

            // LocalDate.of에서 유효하지 않은 날짜(예: 13월, 32일 등)일 경우 Exception이 발생합니다.
            LocalDate.of(year, month, day)
            true
        } catch (e: Exception) {
            false
        }
    }

    // 1) 상태만 받아 네비게이션 수행
    suspend fun navigateToNextScreenBasedOnStatus(
        navController: NavHostController
    ) {
        when (checkUserStatus()) {
            ProfileSettingStatus.NEED_USER_INITIAL_SETUP -> {
                navController.navigate(Screen.Onboarding.route)
            }
            ProfileSettingStatus.NEED_COUPLE_CONNECT,
            ProfileSettingStatus.NEED_COUPLE_INITIAL_SETUP -> {
                // 둘 다 같은 화면으로 이동하므로 묶음 처리
                navController.navigate(Screen.OnboardingConnect.route)
            }
            ProfileSettingStatus.COMPLETED -> {
                isConnect = true
                navController.navigate(Screen.Home.route)
            }
        }
    }

    suspend fun joinCouple(inviteCode: String): Result<Long> = runCatching {
        val resp = apiService.joinCouple(JoinCoupleRequest(inviteCode))
        if (!resp.isSuccessful) throw ApiException(resp.code(), resp.errorBody()?.string().orEmpty())
        val body = resp.body() ?: throw ApiException(resp.code(), "Empty body")
        if (body.success != true || body.data == null) throw ApiException(resp.code(), body.error?.message ?: "Join failed")
        body.data!!.coupleId
    }

    suspend fun previewJoinCouple(inviteCode: String): Result<JoinCouplePreviewData> = runCatching {
        val resp = apiService.previewJoinCouple(JoinCoupleRequest(inviteCode))
        if (!resp.isSuccessful) throw ApiException(resp.code(), resp.errorBody()?.string().orEmpty())
        val body = resp.body() ?: throw ApiException(resp.code(), "Empty body")
        if (body.success != true || body.data == null) throw ApiException(resp.code(), body.error?.message ?: "Preview failed")
        
        val data = body.data!!
        data
    }

    suspend fun fetchInvitationCode(): Result<String> = runCatching {
        val resp = apiService.createInvitationCode()
        if (!resp.isSuccessful) {
            val body = resp.errorBody()?.string().orEmpty()
            throw ApiException(resp.code(), body.ifBlank { "HTTP ${resp.code()}" })
        }
        val body = resp.body() ?: throw ApiException(resp.code(), "Empty body")
        if (body.success != true || body.data == null) {
            throw ApiException(resp.code(), body.error?.message ?: "Failed to get invitation code")
        }
        body.data!!.invitationCode
    }

    fun loadInvitationCode() {
        viewModelScope.launch {
            _loading.value = true
            fetchInvitationCode()
                .onSuccess { code ->
                    _myCode.value = code
                }
                .onFailure { throwable ->
                    val message = when (throwable) {
                        is ApiException -> throwable.message
                        is IOException -> "네트워크 오류가 발생했습니다."
                        else -> "알 수 없는 오류가 발생했습니다."
                    }
                    _error.emit(message)
                }
            _loading.value = false
        }
    }

    // UI에 보여줄 전체 키워드 리스트 (디폴트 + 커스텀)
    private val _displayKeywords = MutableStateFlow<List<KeywordInfo>>(emptyList())
    val displayKeywords: StateFlow<List<KeywordInfo>> = _displayKeywords

    var selectedKeywordId by mutableStateOf<Long?>(null)
        private set
    fun selectKeyword(id: Long) {
        selectedKeywordId = id
        fetchDailyData(id) // 해당 ID의 일상 데이터를 가져오는 API 호출
    }

    // 서버에서 받은 디폴트 키워드 매핑용 (Content -> ID)
    private var defaultKeywordMap = mapOf<String, Long>()

    // 화면 진입 시 호출: 디폴트 키워드 로딩
    // 1. 키워드 로드 함수 수정
    fun loadDefaultKeywords() {
        viewModelScope.launch {
            try {
                val response = apiService.getDefaultKeywords()

                if (response.isSuccessful && response.body()?.success == true) {
                    val list = response.body()?.data?.keywordInfoList ?: emptyList()

                    // 매핑 맵 생성
                    defaultKeywordMap = list.associate { it.content to it.keywordId.toLong() }

                    // UI 리스트 업데이트 (정렬)
                    val sortedList = list.sortedBy { it.displayOrder }
                    _displayKeywords.value = sortedList

                    // [추가] 리스트가 비어있지 않다면 첫 번째 키워드를 자동으로 선택
                    if (sortedList.isNotEmpty() && selectedKeywordId == null) {
                        selectKeyword(sortedList.first().keywordId.toLong())
                    }
                }
            } catch (e: Exception) {
                Log.e("API", "키워드 로드 에러", e)
            }
        }
    }

    // "다음" 버튼 클릭 시 호출: 선택된 키워드 분류 및 업로드
    fun saveKeywords(selectedKeywords: Set<String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val defaultIds = mutableListOf<Long>()
                val customList = mutableListOf<String>()

                // 선택된 키워드가 디폴트인지 커스텀인지 분류
                selectedKeywords.forEach { keyword ->
                    val id = defaultKeywordMap[keyword]
                    if (id != null) {
                        defaultIds.add(id)
                    } else {
                        customList.add(keyword)
                    }
                }

                // 커스텀 키워드 리스트를 요청 포맷 문자열로 변환
                // 예: "['산책', '맛집']" (작은따옴표 주의)
                val customKeywordsString = if (customList.isEmpty()) {
                    "[]"
                } else {
                    customList.joinToString(prefix = "['", separator = "', '", postfix = "']")
                }

                val request = KeywordUpdateRequest(
                    defaultKeywordIds = defaultIds,
                    customKeywords = customKeywordsString
                )

                // API 호출
                val response = apiService.updateCoupleKeywords(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("API", "키워드 저장 성공")
                    onResult(true)
                } else {
                    Log.e("API", "키워드 저장 실패: ${response.errorBody()?.string()}")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("API", "키워드 저장 에러", e)
                onResult(false)
            }
        }
    }

    // 직접 추가한 키워드를 UI 목록에 반영하는 함수
    fun addCustomKeywordToDisplay(newKeyword: String) {
        val current = _displayKeywords.value.toMutableList()
        // todo key word 추가
        current.add(KeywordInfo(
            keywordId = 0,
            content = newKeyword,
            displayOrder = 0,
        ))
        _displayKeywords.value = current
    }

    var showPokeSuccessDialog by mutableStateOf(false)
        private set

    fun pokePartner() {
        viewModelScope.launch {
            try {
                val response = apiService.pokeUser(questionData!!.partnerInfo.userId)
                if (response.isSuccessful && response.body()?.success == true) {
                    // 콕 찌르기 성공 시 다이얼로그 띄움
                    showPokeSuccessDialog = true
                } else {
                    // 에러 처리 로직
                }
            } catch (e: Exception) {
                // 네트워크 에러 처리
            }
        }
    }

    fun dismissPokeDialog() {
        showPokeSuccessDialog = false
    }

    var questionData by mutableStateOf<CoupleQuestionData?>(null)
        private set

    // 질문 조회 로직
    fun fetchTodayQuestion() {
        viewModelScope.launch {
            try {
                // 경로상의 ID를 모를 때는 "current" 혹은 서버 약속된 값을 사용
                val response = apiService.getTodayQuestion("current")
                if (response.isSuccessful) {
                    questionData = response.body()?.data
                }
            } catch (e: Exception) { /* 에러 처리 */ }
        }
    }

    // 2. 사진 서버 업로드 함수
    fun uploadImage(uri: Uri) {
        val questionId = questionData?.coupleQuestionId ?: return

        viewModelScope.launch {
            try {
                // 수정된 로직: S3 업로드 후 서버에 imageKey 전달
                val result = dailyRepository.uploadQuestionPhoto(questionId, uri)

                result.onSuccess {
                    // 업로드 성공 후 화면 갱신을 위해 데이터 다시 로드
                    fetchTodayQuestion()
                }.onFailure {
                    // 에러 알림 등 처리
                }
            } catch (e: Exception) { /* 에러 처리 */ }
        }
    }

    var dailyData by mutableStateOf<CoupleQuestionData?>(null)
        private set

    // 키워드 탭 클릭 시 호출
    fun fetchDailyData(coupleKeywordId: Long) {
        viewModelScope.launch {
            dailyRepository.getTodayDaily(coupleKeywordId)
                .onSuccess { dailyData = it }
        }
    }

    // 오늘의 일상 사진 업로드
    fun uploadDailyImage(uri: Uri) {
        val id = selectedKeywordId ?: return
        viewModelScope.launch {
            dailyRepository.uploadDailyPhoto(id, uri).onSuccess {
                fetchDailyData(id) // 업로드 성공 후 화면 갱신을 위해 데이터 재조회
            }
        }
    }

    var archiveItems by mutableStateOf<List<Pair<String, UserAnswerInfo>>>(emptyList())
        private set

    private var nextCursor: String? = null
    var hasNext by mutableStateOf(true)
    var isLoading by mutableStateOf(false)

    fun fetchArchives(isRefresh: Boolean = false) {
        // 로딩 중이거나, 새로고침이 아닌데 다음 페이지가 없으면 리턴
        if (isLoading || (!isRefresh && !hasNext)) return

        isLoading = true

        // 새로고침이면 커서를 null로, 아니면 기존 nextCursor 사용
        val currentCursor = if (isRefresh) null else nextCursor

        viewModelScope.launch {
            archiveRepository.getArchiveList(size = 20, cursor = currentCursor)
                .onSuccess { data ->
                    val newItems = data.archiveList.flatMap { group ->
                        group.imageInfoList.map { info -> group.date to info }
                    }

                    // 데이터 업데이트
                    archiveItems = if (isRefresh) newItems else archiveItems + newItems
                    nextCursor = data.nextCursor
                    hasNext = data.hasNext
                }
                .onFailure { /* 에러 처리 */ }
            isLoading = false
        }
    }
    // 캘린더 화면용 데이터
    var calendarDays by mutableStateOf<List<CalendarDayInfo>>(emptyList())
        private set

    // 상세 화면용 데이터
    var detailItems by mutableStateOf<List<ArchiveDateGroup>>(emptyList())
        private set


    // 2. 상세 데이터 불러오기 (최신순 클릭 시 혹은 캘린더 클릭 시)
    fun fetchDetail(date: String, targetId: Long? = null, targetType: String? = null) {
        viewModelScope.launch {
            archiveRepository.getDetailByDate(date, targetId, targetType)
                .onSuccess { data ->
                    detailItems = data.archiveInfoList

                    // TODO: UI에서 detailItems 중 selected: true인 항목의 인덱스를 찾아
                    // LazyColumn의 initialFirstVisibleItemIndex 등으로 전달해야 합니다.
                }
        }
    }

    // 현재 사용자가 보고 있는 기준 날짜 (기본값: 오늘)
    var currentCalendarDate by mutableStateOf(LocalDate.now())
        private set

    // 월 변경 시 호출
    fun updateCalendarMonth(monthsToAdd: Long) {
        currentCalendarDate = currentCalendarDate.plusMonths(monthsToAdd)
        fetchCalendar(currentCalendarDate.year, currentCalendarDate.monthValue)
    }

    // 캘린더 데이터 조회
    fun fetchCalendar(year: Int, month: Int) {
        viewModelScope.launch {
            archiveRepository.getCalendar(year, month)
                .onSuccess { data -> calendarDays = data.days }
        }
    }


    // 질문 목록 데이터 상태
    var archiveQuestions by mutableStateOf<List<ArchiveQuestionItem>>(emptyList())
        private set

    private var questionNextCursor: String? = null
    var hasQuestionNext by mutableStateOf(true)
    var isQuestionLoading by mutableStateOf(false)

    fun fetchQuestionArchives(isRefresh: Boolean = false) {
        if (isQuestionLoading || (!isRefresh && !hasQuestionNext)) return

        isQuestionLoading = true
        val currentCursor = if (isRefresh) null else questionNextCursor

        viewModelScope.launch {
            archiveRepository.getQuestionArchiveList(size = 20, cursor = currentCursor)
                .onSuccess { data ->
                    archiveQuestions = if (isRefresh) data.questionList else archiveQuestions + data.questionList
                    questionNextCursor = data.nextCursor
                    hasQuestionNext = data.hasNext
                }
                .onFailure { /* 에러 처리 */ }
            isQuestionLoading = false
        }
    }

    var selectedQuestionDetail by mutableStateOf<QuestionDetailResponse?>(null)
        private set

    fun fetchQuestionDetail(id: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.getQuestionArchiveDetail(id)
                if (response.isSuccessful && response.body()?.success == true) {
                    selectedQuestionDetail = response.body()?.data
                }
            } catch (e: Exception) {
                Log.e("API", "질문 상세 조회 실패", e)
            }
        }
    }
}