package org.withus.app.model

data class ArchiveResponse(
    val archiveList: List<ArchiveDateGroup>,
    val hasNext: Boolean,
    val nextCursor: String?
)

data class ArchiveDateGroup(
    val date: String, // "2026-01-28"
    val imageInfoList: List<UserAnswerInfo> // 기존 사용하던 모델 재사용
)

data class ArchiveDetailResponse(
    val date: String,
    val archiveInfoList: List<ArchiveDateGroup> // 기존 정의한 ArchiveDateGroup 재사용
)

// 2. 캘린더 월별 조회 모델
data class CalendarResponse(
    val year: Int,
    val month: Int,
    val days: List<CalendarDayInfo>
)

data class CalendarDayInfo(
    val date: String,
    val meImageThumbnailUrl: String?,
    val partnerImageThumbnailUrl: String?
)

data class QuestionArchiveResponse(
    val questionList: List<ArchiveQuestionItem>,
    val hasNext: Boolean,
    val nextCursor: String?
)

data class ArchiveQuestionItem(
    val coupleQuestionId: Long,
    val questionNumber: Long,
    val questionContent: String
)


data class QuestionDetailResponse(
    val coupleQuestionId: Long,
    val questionNumber: Long,
    val questionContent: String,
    val myInfo: UserAnswerInfo?, // 사진 삭제 시 null 가능성 대비
    val partnerInfo: UserAnswerInfo?
)
