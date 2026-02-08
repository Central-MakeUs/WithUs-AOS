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
import org.withus.app.debug
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
import org.withus.app.repository.TestTest
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
    private val tokenManager: TokenManager,
    private val preferenceManager: PreferenceManager,
    private val archiveRepository: ArchiveRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    var isCreatingManual by mutableStateOf(false)
    private val _myCode = MutableStateFlow<String?>(null)
    val myCode: StateFlow<String?> = _myCode.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    var isOnboardingComplete by mutableStateOf(false)
        private set
    // 1단계: 닉네임


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


    init {
        // 앱 실행 시 저장된 값들을 로드
        viewModelScope.launch {
            preferenceManager.isOnboardingComplete.collect { savedStatus ->
                isOnboardingComplete = savedStatus
            }
        }

        loadDummyMemories()
    }


    // 3단계: 첫 만남 기념일
    var anniversaryDate by mutableStateOf("")
        private set
    fun updateAnniversaryDate(input: String) {
        if (input.length <= 8 && input.all { it.isDigit() }) {
            anniversaryDate = input
        }
    }

    // 4단계: 프로필 이미지 URI

//    var userUploadedImage by mutableStateOf<Uri?>(null)
    var userUploadedImage by mutableStateOf<Uri?>(
        Uri.parse("https://img1.daumcdn.net/thumb/R1280x0.fjpg/?fname=http://t1.daumcdn.net/brunch/service/user/1dEO/image/CIieqAqy0KlR6UdFHxrc1NsGtVM.jpg")
    )

//    var partnerUploadedImage by mutableStateOf<Uri?>(
//        Uri.parse("https://img1.daumcdn.net/thumb/R1280x0.fjpg/?fname=http://t1.daumcdn.net/brunch/service/user/1dEO/image/CIieqAqy0KlR6UdFHxrc1NsGtVM.jpg")
//    )
    var partnerUploadedImage by mutableStateOf<Uri?>(null)


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
        OnboardingPage("오늘의 질문", "매일 전달되는 질문으로\n서로에 대해 더 알아가요.", "매일 제시되는 질문에 사진으로 답하면, \n" +
                "상대의 생각을 자연스럽게 엿볼 수 있어요.", R.drawable.image_onboarding_step1),
        OnboardingPage("오늘의 일상 ", "오늘 하루를 \n" +
                "사진 한 장으로 나눠요", "함께 정한 키워드로 사진 한 장씩,\n" +
                "부담 없이 가볍고 다정한 일상 공유가 시작돼요.", R.drawable.image_onboarding_step2),
        OnboardingPage("자동 추억 생성", "쌓이는 일상이 \n" +
                "우리의 이야기가 돼요", "일주일의 일상 사진이 자동으로 추억이 되고, \n" +
                "원하는 순간을 직접 담을 수도 있어요.", R.drawable.image_onboarding_step3),

    )

    fun updateSelectedImage(uri: Uri?) {
        selectedImageUri = uri
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

    var deleteStep by mutableIntStateOf(1)
        private set

    fun updateDeleteStep(step: Int) {
        deleteStep = step
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

    fun fetchTodayQuestionTest() {
        viewModelScope.launch {
            // 실제 API 호출 대신 예시 데이터 주입
            val mockData = CoupleQuestionData(
                coupleQuestionId = 505L,
                question = "상대가 가장 사랑스러워 보였던 순간은 언제인가요?",
                date = LocalDate.now(),
                myInfo = UserAnswerInfo(
                    userId = 1L,
                    name = "나",
                    profileThumbnailImageUrl = TestTest.testImageUrl,
//                    questionImageUrl = "https://img1.daumcdn.net/thumb/R1280x0.fjpg/?fname=http://t1.daumcdn.net/brunch/service/user/1dEO/image/CIieqAqy0KlR6UdFHxrc1NsGtVM.jpg",
                    questionImageUrl = null,
                    answeredAt = "20:30"
                ),
                partnerInfo = UserAnswerInfo(
                    userId = 2L,
                    name = "상대방",
                    profileThumbnailImageUrl = TestTest.testImageUrl,
                    questionImageUrl = "https://img1.daumcdn.net/thumb/R1280x0.fjpg/?fname=http://t1.daumcdn.net/brunch/service/user/1dEO/image/CIieqAqy0KlR6UdFHxrc1NsGtVM.jpg",
                    answeredAt = "21:00"
                )
            )

            questionData = mockData
        }
    }

    // 질문 조회 로직
    fun fetchTodayQuestion() {
        viewModelScope.launch {
            try {
                // 경로상의 ID를 모를 때는 "current" 혹은 서버 약속된 값을 사용
                val response = apiService.getTodayQuestion()
                if (response.isSuccessful) {
                    questionData = response.body()?.data
                }
            } catch (e: Exception) { /* 에러 처리 */ }
        }
    }

    // 2. 사진 서버 업로드 함수
    fun uploadTodayQuestionImage(uri: Uri) {
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

    var detailList by mutableStateOf<List<QuestionDetailResponse>>(emptyList())

    var scrollIndex by mutableStateOf(0)

    fun fetchDetailByDate(date: String, targetId: Long? = null, targetType: String? = null) {
        viewModelScope.launch {
            archiveRepository.getDetailByDate(date, targetId, targetType)
                .onSuccess { response ->
                    // 1. ArchiveDateGroup 리스트를 QuestionDetailResponse 리스트로 매핑
                    val mappedList = response.archiveInfoList.map { group ->
                        QuestionDetailResponse(
                            coupleQuestionId = targetId ?: 0L, // 식별용
                            questionNumber = 0L,
                            questionContent = "질문 내용", // 필요시 추가 필드 확인
                            myInfo = group.myInfo,
                            partnerInfo = group.partnerInfo
                        )
                    }

                    detailList = mappedList

                    detailList = mappedList

                    // [중요] 서버 응답에 selected 필드가 있다면 그걸로 인덱스 찾기
                    val index = response.archiveInfoList.indexOfFirst { it.selected }
                    scrollIndex = if (index != -1) index else 0

                    // 상세 데이터가 준비되었음을 알림 (기존 detailData 변수 업데이트)
                    selectedQuestionDetail = mappedList.getOrNull(scrollIndex)
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

    fun ArchiveDateGroup.toQuestionDetail(myId: Long): QuestionDetailResponse {
        // imageInfoList에서 내 정보와 상대방 정보를 분리
        val myInfo = this.imageInfoList.find { it.userId == myId }
        val partnerInfo = this.imageInfoList.find { it.userId != myId }

        return QuestionDetailResponse(
            coupleQuestionId = 0L, // JSON에 ID가 없다면 임시값 또는 필드 추가 필요
            questionNumber = 0L,   // JSON에 없다면 필드 추가 필요
            questionContent = "질문 내용이 여기에 들어갑니다.", // API 응답에 맞게 수정
            myInfo = myInfo,
            partnerInfo = partnerInfo
        )
    }

}