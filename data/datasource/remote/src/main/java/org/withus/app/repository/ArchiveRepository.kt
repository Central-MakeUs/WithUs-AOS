package org.withus.app.repository

import org.withus.app.model.ArchiveDetailResponse
import org.withus.app.model.ArchiveResponse
import org.withus.app.model.CalendarResponse
import org.withus.app.model.QuestionArchiveResponse
import org.withus.app.remote.ApiService
import javax.inject.Inject

class ArchiveRepository @Inject constructor(
    private val api: ApiService
) {
    suspend fun getArchiveList(size: Int, cursor: String?): Result<ArchiveResponse> = runCatching {
        val resp = api.getArchives(size, cursor)
        if (resp.isSuccessful && resp.body()?.success == true) {
            resp.body()!!.data!!
        } else {
            throw Exception("보관함 로드 실패")
        }
    }

    suspend fun getDetailByDate(date: String, targetId: Long?, targetType: String?): Result<ArchiveDetailResponse> = runCatching {
        val resp = api.getArchiveDetailByDate(date, targetId, targetType)
        resp.body()?.data ?: throw Exception("상세 데이터 로드 실패")
    }

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
}