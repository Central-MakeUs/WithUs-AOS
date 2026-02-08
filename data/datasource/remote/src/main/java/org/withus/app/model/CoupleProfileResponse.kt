package org.withus.app.model

data class CoupleProfileResponse(
    val meProfile: UserProfileDto,
    val partnerProfile: UserProfileDto
)

data class UserProfileDto(
    val nickname: String,
    val birthday: String, // "2026-02-09"
    val profileImageUrl: String?
)