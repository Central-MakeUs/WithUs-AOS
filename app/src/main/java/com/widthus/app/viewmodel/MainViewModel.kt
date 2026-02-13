package com.widthus.app.viewmodel

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.widthus.app.model.OnboardingPage
import com.widthus.app.screen.BottomNavItem
import com.widthus.app.screen.Screen
import com.widthus.app.utils.PreferenceManager
import com.widthus.app.widget.MyWithUsWidget
import com.withus.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.withus.app.debug
import org.withus.app.model.ApiException
import org.withus.app.model.ArchiveDateGroup
import org.withus.app.model.ArchiveQuestionItem
import org.withus.app.model.ArchiveUserAnswerInfo
import org.withus.app.model.CalendarDayInfo
import org.withus.app.model.CoupleKeyword
import org.withus.app.model.CoupleQuestionData
import org.withus.app.model.JoinCouplePreviewData
import org.withus.app.model.JoinCoupleRequest
import org.withus.app.model.KeywordEditItem
import org.withus.app.model.KeywordInfo
import org.withus.app.model.KeywordUpdateRequest
import org.withus.app.model.ProfileSettingStatus
import org.withus.app.model.QuestionDetailResponse
import org.withus.app.token.TokenManager
import org.withus.app.remote.ApiService
import org.withus.app.remote.NetworkResult
import org.withus.app.repository.ArchiveRepository
import org.withus.app.repository.DailyRepository
import org.withus.app.repository.KeywordRepository
import org.withus.app.repository.ProfileRepository
import org.withus.app.repository.UserRepository
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
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
    private val keywordRepository: KeywordRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    var isCreatingManual by mutableStateOf(false)
    private val _myCode = MutableStateFlow<String?>(null)
    val myCode: StateFlow<String?> = _myCode.asStateFlow()

    private val _selectedMainTab = MutableStateFlow("오늘의 질문")
    val selectedMainTab: StateFlow<String> = _selectedMainTab

    val startDestination: StateFlow<String?> = preferenceManager.isOnboardingComplete
        .map { isComplete ->
            if (isComplete) Screen.Login.route else Screen.Onboarding.route
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateMainTab(tab: String) {
        _selectedMainTab.value = tab
    }


    private val _currentBottomRoute = MutableStateFlow(BottomNavItem.Home.route)
    val currentBottomRoute: StateFlow<String> = _currentBottomRoute.asStateFlow()

    fun updateRoute(route: String) {
        _currentBottomRoute.value = route
    }

    var joinPreviewData by mutableStateOf<JoinCouplePreviewData?>(null)
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

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

    var isConnect = false

    private val _displayedMonths = mutableStateListOf<YearMonth>() // 여기서 바로 초기화!
    val displayedMonths: List<YearMonth> = _displayedMonths

    private val _calendarDataMap = mutableStateMapOf<YearMonth, List<CalendarDayInfo>>()
    val calendarDataMap: Map<YearMonth, List<CalendarDayInfo>> = _calendarDataMap

    init {
        val current = YearMonth.now()
        for (i in 0 until 12) {
            _displayedMonths.add(current.minusMonths(i.toLong()))
        }
    }


    // 3단계: 첫 만남 기념일
    var anniversaryDate by mutableStateOf("")
        private set

    fun updateAnniversaryDate(input: String) {
        val current = YearMonth.now()

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
        try {
            when (checkUserStatus()) {
                ProfileSettingStatus.NEED_USER_INITIAL_SETUP -> {
                    navController.navigate(Screen.StepInput.route)
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
            }.also {
                debug("checkUserStatus : $it!")
            }
        } catch (
            e: Exception
        ) {
            Toast.makeText(context, "로그인 실패", Toast.LENGTH_SHORT).show()
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

    private val _coupleKeyword = MutableStateFlow<List<CoupleKeyword>>(emptyList())
    val coupleKeyword: StateFlow<List<CoupleKeyword>> = _coupleKeyword

    // 업데이트 함수
    fun updateSelectedKeywords(newList: List<CoupleKeyword>) {
        _coupleKeyword.value = newList
    }
    // UI에 보여줄 전체 키워드 리스트 (디폴트 + 커스텀)
    private val _defaultKeywords = MutableStateFlow<List<KeywordInfo>>(emptyList())
    val defaultKeywords: StateFlow<List<KeywordInfo>> = _defaultKeywords

    private val _editKeywords = MutableStateFlow<List<KeywordEditItem>>(emptyList())
    val editKeywords: StateFlow<List<KeywordEditItem>> = _editKeywords

    private val _selectedKeywordId = MutableStateFlow<Long>(0L)
    val selectedKeywordId: StateFlow<Long> = _selectedKeywordId

    fun selectKeyword(id: Long) {
        _selectedKeywordId.value = id
        val coupleKeywordId = getSelectedKeyId()?.coupleKeywordId
        fetchDailyKeywordData(coupleKeywordId?.toLong() ?: 0L) // 해당 ID의 일상 데이터를 가져오는 API 호출
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
                    _defaultKeywords.value = sortedList

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

                // 1. 분류 로직 (기존과 동일)
                selectedKeywords.forEach { keyword ->
                    val id = defaultKeywordMap[keyword]
                    if (id != null) {
                        defaultIds.add(id)
                    } else {
                        customList.add(keyword)
                    }
                }

                val request = KeywordUpdateRequest(
                    defaultKeywordIds = defaultIds,
                    customKeywords = customList
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
        val current = _defaultKeywords.value.toMutableList()
        current.add(KeywordInfo(
            keywordId = 0,
            content = newKeyword,
            displayOrder = 0,
        ))
        _defaultKeywords.value = current
    }

    var showPokeSuccessDialog by mutableStateOf(false)
        private set

    fun pokePartner() {
        val partnerId = questionData?.partnerInfo?.userId?.run {
            this
        } ?: keywordDailyData?.partnerInfo?.userId
        debug("pokePartner ! : $partnerId")

        viewModelScope.launch {
            try {
                val response = apiService.pokeUser(partnerId!!)
                debug("response : $response")
                if (response.isSuccessful && response.body()?.success == true) {
                    // 콕 찌르기 성공 시 다이얼로그 띄움
                    showPokeSuccessDialog = true
                } else {
                    // 에러 처리 로직
                }
            } catch (e: Exception) {
                debug("error : $e")
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
                val response = apiService.getTodayQuestion()
                if (response.isSuccessful) {
                    questionData = response.body()?.data

                    questionData?.let { data ->
                        data.partnerInfo?.questionImageUrl?.let {
                            updateMyWidget(context, data.question, it)
                        }
                    }
                }
            } catch (e: Exception) { /* 에러 처리 */ }
        }
    }

    private fun updateMyWidget(context: Context, title: String, uri: String) {
        val intent = Intent(context, MyWithUsWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

            // 위젯에 전달할 데이터 담기
            putExtra("EXTRA_QUESTION", title)
            // 프로필 이미지를 위젯 배경으로 쓰고 싶다면 (예: 본인 정보)
            putExtra("EXTRA_IMAGE_URL", uri)

            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, MyWithUsWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }

    // 2. 사진 서버 업로드 함수
    fun uploadTodayQuestionImage(uri: Uri) {
        debug("uploadTodayQuestionImage : uri ")
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

    var keywordDailyData by mutableStateOf<CoupleQuestionData?>(null)
        private set

    // 키워드 탭 클릭 시 호출
    fun fetchDailyKeywordData(coupleKeywordId: Long) {
        viewModelScope.launch {
            dailyRepository.getTodayDaily(coupleKeywordId)
                .onSuccess {
                    keywordDailyData = it

                    keywordDailyData?.let { data ->
                        data.partnerInfo?.questionImageUrl?.let { imageUrl ->
                            updateMyWidget(context, data.question, imageUrl)
                        }
                    }
                }
        }

    }

    // 오늘의 일상 사진 업로드
    fun uploadDailyImage(uri: Uri) {
        // 1. 현재 선택된 keywordId 가져오기
        val currentSelectedId = selectedKeywordId.value
        if (currentSelectedId == 0L) return

        // 2. 전체 키워드 리스트에서 해당 keywordId와 일치하는 CoupleKeyword 객체 찾기
        val targetKeyword = _coupleKeyword.value.find {
            it.keywordId.toLong() == currentSelectedId
        }

        // 3. 일치하는 항목이 있고, coupleKeywordId가 유효한 경우에만 업로드 진행
        targetKeyword?.coupleKeywordId?.let { coupleKeywordId ->
            viewModelScope.launch {
                // 주석: 서버 API가 요구하는 coupleKeywordId(Int)를 전달합니다.
                dailyRepository.uploadDailyPhoto(coupleKeywordId.toLong(), uri).onSuccess {
                    // 업로드 성공 후 화면 갱신을 위해 데이터 재조회 (기존 선택된 ID 유지)
                    fetchDailyKeywordData(coupleKeywordId.toLong())
                }
            }
        }
    }

    var archiveItems by mutableStateOf<List<Pair<String, ArchiveUserAnswerInfo>>>(emptyList())
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

                    // [중요] 서버 응답에 selected 필드가 있다면 그걸로 인덱스 찾기
                    val index = response.archiveInfoList.indexOfFirst { it.selected }
                    scrollIndex = if (index != -1) index else 0
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

    fun fetchCalendar(yearMonth: YearMonth) {
        // 이미 데이터가 있다면 다시 부르지 않음 (캐싱)
        if (_calendarDataMap.containsKey(yearMonth)) return

        viewModelScope.launch {
            // API 호출 (기존 로직 활용)
            archiveRepository.getCalendar(yearMonth.year, yearMonth.monthValue)
                .onSuccess { data ->
                    // 받아온 데이터를 Map에 저장
                    _calendarDataMap[yearMonth] = data.days
                }
                .onFailure {
                    // 에러 처리 (빈 리스트라도 넣어주어야 계속 로딩 안 함)
                    _calendarDataMap[yearMonth] = emptyList()
                }
        }
    }

    fun loadMorePastMonths() {
        val lastMonth = _displayedMonths.last()
        for (i in 1..6) {
            _displayedMonths.add(lastMonth.minusMonths(i.toLong()))
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


    fun fetchQuestionDetail(id: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.getQuestionArchiveDetail(id)

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data

                    if (data != null) {
                        detailList = listOf(data)
                    }
                }
            } catch (e: Exception) {
                Log.e("API", "질문 상세 조회 실패", e)
            }
        }
    }
    fun getCoupleKeyword() {
        viewModelScope.launch {
            when (val result = keywordRepository.getCoupleKeywords()) {
                is NetworkResult.Success -> {
                    val keywords = result.data ?: emptyList()
                    _coupleKeyword.value = keywords

                    // 1. 데이터가 존재하고, 아직 선택된 키워드가 없는 경우 (초기 진입 시)
                    if (keywords.isNotEmpty() && selectedKeywordId.value == 0L) {
                        val firstId = keywords[0].keywordId.toLong()
                        selectKeyword(firstId)
                    }
                }

                is NetworkResult.Error -> { /* 에러 처리 */
                }

                is NetworkResult.Exception -> { /* 예외 처리 */
                }
            }
        }
    }


    fun loadEditableKeywords() {
        viewModelScope.launch {
            val response = apiService.getEditableKeywords()
            if (response.isSuccessful) {
                val allKeywords = response.body()?.data?.keywords ?: emptyList()

                // 1. 화면에 보여줄 전체 리스트 업데이트
                _editKeywords.value = allKeywords

                // 2. 그 중 선택된(isSelected == true) 것들만 추출해서 선택 상태 초기화
                val initialSelected = allKeywords
                    .filter { it.isSelected }
                    .map { it.content }
                    .toSet()

                // UI Layer에서 관찰 중인 selectedKeywords를 이 값으로 세팅하시면 됩니다.
            }
        }
    }

    private fun getSelectedKeyId(): CoupleKeyword? {
        val currentId = selectedKeywordId.value
        return _coupleKeyword.value.find { it.keywordId.toLong() == currentId }
    }
}