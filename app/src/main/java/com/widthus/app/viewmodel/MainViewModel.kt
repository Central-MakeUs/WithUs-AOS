package com.widthus.app.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.widthus.app.model.MemoryItem
import com.widthus.app.model.ScheduleItem
import com.widthus.app.model.OnboardingPage
import com.withus.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import org.withus.app.token.TokenManager
import org.withus.app.model.request.LoginRequest
import org.withus.app.remote.ApiService
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {
    // 1단계: 닉네임
    var nickname by mutableStateOf("")
        private set

    var jwtToken = String()

    var partnerNickname by mutableStateOf("감자")
        private set
    fun updateNickname(input: String) { nickname = input }

    // 2단계: 생일
    var birthDate by mutableStateOf("")
        private set
    fun updateBirthDate(input: String) {
        if (input.length <= 8 && input.all { it.isDigit() }) {
            birthDate = input
        }
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
    var profileImageUri by mutableStateOf<Uri?>(null)
        private set

    fun updateProfileImage(uri: Uri?) {
        profileImageUri = uri
    }

    // 날짜 입력 제한 (8자리)
    fun updateDateInput(input: String, type: Int) {
        val digits = input.filter { it.isDigit() }
        if (digits.length <= 8) {
            if (type == 2) birthDate = digits else anniversaryDate = digits
        }
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
}