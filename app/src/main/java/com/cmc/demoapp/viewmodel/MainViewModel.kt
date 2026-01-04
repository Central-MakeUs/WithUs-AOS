package com.cmc.demoapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cmc.demoapp.model.MemoryItem
import com.cmc.demoapp.model.ScheduleItem
import com.cmc.demoapp.model.OnboardingPage
import com.koiware.demoapp.R

class MainViewModel : ViewModel() {
    // 닉네임
    var nickname by mutableStateOf("")
        private set

    fun updateNickname(name: String) {
        nickname = name
    }

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
        OnboardingPage("주어진 질문에 사진 한 장으로\n둘만의 일상을 공유해요"),
        OnboardingPage("쌓여가는 둘만의 사진 기록을\n한눈에 확인해요"),
        OnboardingPage("원하는 사진으로\n둘만의 인생 네컷을 만들어봐요"),
        OnboardingPage("사진으로 이어지는\n둘만의 일상, 지금 시작해요")
    )

}