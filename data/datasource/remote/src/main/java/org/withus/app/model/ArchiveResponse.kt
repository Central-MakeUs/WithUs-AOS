package org.withus.app.model

data class ArchiveResponse(
    val archiveList: List<ArchiveDateGroup>,
    val hasNext: Boolean,
    val nextCursor: String?
)

data class ArchiveDateGroup(
    val date: String, // "2026-01-28"
    val imageInfoList: List<UserAnswerInfo>,
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


// 상세 페이지용 아이템 모델 (명세서의 "selected" 반영)
data class ArchiveDetailItem(
    val archiveType: String,
    val id: Long,
    val question: String?,
    val myInfo: UserAnswerInfo?,
    val partnerInfo: UserAnswerInfo?,
    val selected: Boolean,
)

data class UserArchiveInfo(
    val userId: Long,
    val name: String,
    val profileThumbnailImageUrl: String,
    val answerImageUrl: String?,
    val answeredAt: String?
)

data class ArchiveDetailResponse(
    val date: String,
    val archiveInfoList: List<ArchiveDetailItem>
)

data class ArchiveListResponse(
    val archiveList: List<ArchiveGroupItem>,
    val hasNext: Boolean,
    val nextCursor: String?
)

data class ArchiveGroupItem(
    val date: String,
    val imageInfoList: List<ArchiveUserAnswerInfo> // 여기에 실제 이미지 URL들이 들어있음
)

data class ArchiveUserAnswerInfo(
    val archiveType: String,
    val id: Long,
    val myImageUrl: String?,      // 로그상의 키값
    val partnerImageUrl: String?  // 로그상의 키값
)
