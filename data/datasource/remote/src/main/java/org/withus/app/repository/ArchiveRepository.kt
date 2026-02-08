package org.withus.app.repository

import org.withus.app.model.ArchiveDateGroup
import org.withus.app.model.ArchiveDetailItem
import org.withus.app.model.ArchiveDetailResponse
import org.withus.app.model.ArchiveResponse
import org.withus.app.model.CalendarResponse
import org.withus.app.model.MemoryResponse
import org.withus.app.model.MemoryStatus
import org.withus.app.model.QuestionArchiveResponse
import org.withus.app.model.UserAnswerInfo
import org.withus.app.model.WeekMemorySummary
import org.withus.app.remote.ApiService
import javax.inject.Inject

object TestTest {
    val testImageUrl = "https://img1.daumcdn.net/thumb/R1280x0.fjpg/?fname=http://t1.daumcdn.net/brunch/service/user/1dEO/image/CIieqAqy0KlR6UdFHxrc1NsGtVM.jpg"
}


class ArchiveRepository @Inject constructor(
    private val api: ApiService
) {
//    suspend fun getArchiveList(size: Int, cursor: String?): Result<ArchiveResponse> = runCatching {
//        val resp = api.getArchives(size, cursor)
//        if (resp.isSuccessful && resp.body()?.success == true) {
//            resp.body()!!.data!!
//        } else {
//            throw Exception("보관함 로드 실패")
//        }
//    }

    suspend fun getArchiveList(size: Int, cursor: String?): Result<ArchiveResponse> = runCatching {
        // 1. 실제 API 호출 대신 임시 데이터를 생성합니다.
        val mockData = ArchiveResponse(
            archiveList = listOf(
                // 오늘 날짜 데이터 1
                ArchiveDateGroup(
                    date = "2026-01-28",
                    imageInfoList = listOf(
                        UserAnswerInfo(
                            userId = 123,
                            name = "김철수",
                            profileThumbnailImageUrl = TestTest.testImageUrl,
                            questionImageUrl = TestTest.testImageUrl,
                            answeredAt = "20:30"
                        )
                    )
                ),
                // 테스트를 위해 날짜 하나 더 추가 (선택사항)
                ArchiveDateGroup(
                    date = "2026-01-27",
                    imageInfoList = listOf(
                        UserAnswerInfo(
                            userId = 456,
                            name = "쏘피",
                            profileThumbnailImageUrl = TestTest.testImageUrl,
                            questionImageUrl = TestTest.testImageUrl,
                            answeredAt = "18:15"
                        )
                    )
                )
            ),
            hasNext = true,
            nextCursor = "eyJjcmVhdGVkQXQiOiIyMDI2LTAxLTIzVDAxOjExOjUyIiwiaWQiOjExOX0"
        )

        // 2. 가짜 데이터를 반환 (runCatching이 Result.success로 감싸줍니다)
        mockData

        /* 서버가 정상화되면 아래 코드로 다시 복구하세요.
        val resp = api.getArchives(size, cursor)
        if (resp.isSuccessful && resp.body()?.success == true) {
            resp.body()!!.data!!
        } else {
            throw Exception("보관함 로드 실패")
        }
        */
    }

    suspend fun getDetailByDate(date: String, targetId: Long?, targetType: String?): Result<ArchiveDetailResponse> = runCatching {
        // 실제 API 응답 모델인 ArchiveDetailItem 리스트를 생성합니다.
        val mockDetailItems = listOf(
            ArchiveDetailItem(
                archiveType = "QUESTION",
                id = 505L,
                selected = (targetId == 505L && targetType == "QUESTION"), // 클릭한 사진 식별
                questionNumber = 1,
                questionContent = "오늘 가장 기억에 남는 일은 무엇인가요?",
                myInfo = UserAnswerInfo(
                    userId = 123L,
                    name = "나",
                    profileThumbnailImageUrl = TestTest.testImageUrl,
                    questionImageUrl = TestTest.testImageUrl,
                    answeredAt = "20:30"
                ),
                partnerInfo = UserAnswerInfo(
                    userId = 456L,
                    name = "상대방",
                    profileThumbnailImageUrl = TestTest.testImageUrl,
                    questionImageUrl = TestTest.testImageUrl,
                    answeredAt = "21:05"
                )
            ),
            ArchiveDetailItem(
                archiveType = "KEYWORD",
                id = 101L,
                selected = (targetId == 101L && targetType == "KEYWORD"), // 클릭한 사진 식별
                questionNumber = null,
                questionContent = "행복한 산책",
                myInfo = UserAnswerInfo(
                    userId = 123L,
                    name = "나",
                    profileThumbnailImageUrl = TestTest.testImageUrl,
                    questionImageUrl = TestTest.testImageUrl,
                    answeredAt = "15:00"
                ),
                partnerInfo = null // 상대방은 아직 안 올린 경우 가정
            )
        )

        // 정의하신 ArchiveDetailResponse 구조로 반환
        ArchiveDetailResponse(
            date = date,
            archiveInfoList = mockDetailItems
        )
    }

//    suspend fun getDetailByDate(date: String, targetId: Long?, targetType: String?): Result<ArchiveDetailResponse> = runCatching {
//        val resp = api.getArchiveDetailByDate(date, targetId, targetType)
//        resp.body()?.data ?: throw Exception("상세 데이터 로드 실패")
//    }

    // 월별 캘린더 데이터 조회
    suspend fun getCalendar(year: Int, month: Int): Result<CalendarResponse> = runCatching {
        val resp = api.getCalendarArchives(year, month)
        resp.body()?.data ?: throw Exception("캘린더 로드 실패")
    }

    suspend fun getQuestionArchiveList(size: Int, cursor: String?): Result<QuestionArchiveResponse> = runCatching {
        val resp = api.getQuestionArchives(size, cursor)
        if (resp.isSuccessful && resp.body()?.success == true) {
            resp.body()!!.data!!
        } else {
            throw Exception("질문 목록 로드 실패")
        }
    }

    suspend fun getMemories1(monthKey: String): Result<MemoryResponse> = runCatching {
        val resp = api.getMemories(monthKey)
        if (resp.isSuccessful && resp.body()?.success == true) {
            resp.body()!!.data!!
        } else {
            throw Exception("추억 로드 실패")
        }
    }

    suspend fun getMemories(monthKey: String): Result<MemoryResponse> = runCatching {
        // 1. 제공해주신 데이터 구조(MemoryResponse)에 직접 데이터 매핑
        val mockResponse = MemoryResponse(
            monthKey = 202604,
            weekMemorySummaries = listOf(
                WeekMemorySummary(
                    memoryType = "WEEK_MEMORY",
                    title = "4월 2주 (03.29~04.04)",
                    customMemoryId = 11L,
                    weekEndDate = "2026-02-07",
                    status = MemoryStatus.NEED_CREATE, // Enum 사용
                    // 요청하신 TestTest.testImageUrl 적용
                    needCreateImageUrls = listOf(TestTest.testImageUrl,TestTest.testImageUrl,TestTest.testImageUrl,TestTest.testImageUrl,TestTest.testImageUrl,TestTest.testImageUrl,TestTest.testImageUrl,TestTest.testImageUrl,TestTest.testImageUrl,TestTest.testImageUrl,TestTest.testImageUrl,TestTest.testImageUrl),
                    createdImageUrl = "https://s3.withus.com/memories/couple123_4w1.jpg",
                    createdAt = "2026-02-07T05:54:54.252Z"
                )
            )
        )

        // 2. Result.success로 MemoryResponse 반환
        mockResponse
    }

}