package com.widthus.app.model

data class OnboardingPage(
    val tag: String,      // "오늘의 질문", "자동 추억 생성" 등
    val title: String,    // "매일 전달되는 질문으로..."
    val content: String,  // "매일 제시되는 질문에 사진으로 답하면..."
    val imageRes: Int     // R.drawable.image_onboarding_step1 등
)