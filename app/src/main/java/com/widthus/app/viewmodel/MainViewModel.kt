package com.widthus.app.viewmodel

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
import com.widthus.app.model.MemoryItem
import com.widthus.app.model.ScheduleItem
import com.widthus.app.model.OnboardingPage
import com.widthus.app.utils.PreferenceManager
import com.withus.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.withus.app.model.CoupleQuestionData
import org.withus.app.model.UserAnswerInfo
import org.withus.app.token.TokenManager
import org.withus.app.model.request.LoginRequest
import org.withus.app.remote.ApiService
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val preferenceManager: PreferenceManager
) : ViewModel() {
    var isOnboardingComplete by mutableStateOf(false)
        private set
    // 1단계: 닉네임
    var nickname by mutableStateOf("jpg") // 기본값 설정
        private set

    var partnerNickname by mutableStateOf("쏘피") // 파트너 이름
        private set

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
                    append(".")
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

    var userUploadedImage by mutableStateOf<Uri?>(null)

    var partnerUploadedImage by mutableStateOf<Uri?>(null)

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

    fun logout() {
    }

    var deleteStep by mutableIntStateOf(1)
        private set

    fun updateDeleteStep(step: Int) {
        deleteStep = step
    }

    fun changeProfile() {

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
}