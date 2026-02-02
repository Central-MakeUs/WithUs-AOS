package org.withus.app.model

import com.google.gson.annotations.SerializedName

// 1) 상태 열거형
enum class ProfileSettingStatus {
    NEED_USER_INITIAL_SETUP,   // 사용자 프로필 설정 필요
    NEED_COUPLE_CONNECT,       // 커플 연결 필요
    NEED_COUPLE_INITIAL_SETUP, // 커플 초기 설정 필요
    COMPLETED                  // 모든 설정 완료
}

// 2) 응답 데이터 모델
data class StatusData(
    @SerializedName("status")
    val profileSettingStatus: ProfileSettingStatus
)