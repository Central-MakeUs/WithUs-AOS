package org.withus.app.model

import java.time.LocalDate

data class CoupleQuestionResponse(
    val success: Boolean,
    val data: CoupleQuestionData?
)

data class CoupleQuestionData(
    val coupleQuestionId: Long,
    val question: String,
    val myInfo: UserAnswerInfo,
    val partnerInfo: UserAnswerInfo,
    val date: LocalDate = LocalDate.now()
)

data class UserAnswerInfo(
    val userId: Long,
    val name: String,
    val profileThumbnailImageUrl: String,
    val questionImageUrl: String,
    val answeredAt: String // "20:30" 형태
)

data class QuestionUserInfo(
    val userId: Long,
    val name: String,
    val profileThumbnailImageUrl: String?,
    val questionImageUrl: String?,
    val answeredAt: String?
)

data class QuestionImageRequest(
    val imageKey: String
)
