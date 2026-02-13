package org.withus.app.repository

import android.app.Application
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.withus.app.model.CoupleQuestionData
import org.withus.app.model.DailyImageRequest
import org.withus.app.model.QuestionImageRequest
import org.withus.app.model.request.PresignedUrlRequest
import org.withus.app.remote.ApiService
import java.io.IOException
import javax.inject.Inject

class DailyRepository @Inject constructor(
    private val api: ApiService,
    private val app: Application
) {
    // Uri -> ByteArray (기존 로직 활용)
    private fun readBytesFromUri(uri: Uri): ByteArray {
        app.contentResolver.openInputStream(uri).use { input ->
            return input?.readBytes() ?: throw IOException("이미지를 읽을 수 없습니다.")
        }
    }

    // [통합 프로세스] 1.S3업로드 -> 2.ImageKey획득 -> 3.서버에 등록
    suspend fun uploadDailyPhoto(coupleKeywordId: Long, imageUri: Uri): Result<Unit> = runCatching {
        val presignedResp = api.getPresignedUrl(PresignedUrlRequest("ARCHIVE"))
        val presignedData = presignedResp.body()?.data ?: throw Exception("Presigned URL 발급 실패")

        // 2) S3 업로드
        val bytes = readBytesFromUri(imageUri)
        val contentType = app.contentResolver.getType(imageUri) ?: "image/jpeg"
        val requestBody = RequestBody.create(contentType.toMediaTypeOrNull(), bytes)

        val uploadResp = api.uploadImageToS3(presignedData.uploadUrl, requestBody)
        if (!uploadResp.isSuccessful) throw Exception("S3 업로드 실패")

        // 3) 우리 서버에 ImageKey 등록
        val registerResp = api.uploadDailyImage(
            coupleKeywordId = coupleKeywordId,
            request = DailyImageRequest(imageKey = presignedData.imageKey)
        )

        if (!registerResp.isSuccessful || registerResp.body()?.success != true) {
            throw Exception(registerResp.body()?.error?.message ?: "이미지 등록 실패")
        }
    }

    // 오늘의 일상 데이터 조회
    suspend fun getTodayDaily(coupleKeywordId: Long): Result<CoupleQuestionData> = runCatching {
        val resp = api.getTodayKeywords(coupleKeywordId)
        resp.body()?.data ?: throw Exception("데이터 로드 실패")
    }

    suspend fun uploadQuestionPhoto(coupleQuestionId: Long, imageUri: Uri): Result<Unit> = runCatching {
        // 1. S3 Pre-signed URL 요청 (카테고리는 MEMORY 또는 QUESTION 등 서버 명세에 맞춤)
        val presignedResp = api.getPresignedUrl(PresignedUrlRequest("ARCHIVE"))
        val presignedData = presignedResp.body()?.data ?: throw Exception("URL 발급 실패")

        // 2. S3에 실제 파일 업로드 (기존에 작성하신 로직 활용)
        val bytes = readBytesFromUri(imageUri)
        val contentType = app.contentResolver.getType(imageUri) ?: "image/jpeg"
        val requestBody = RequestBody.create(contentType.toMediaTypeOrNull(), bytes)
        api.uploadImageToS3(presignedData.uploadUrl, requestBody)

        // 3. 우리 서버에 imageKey 등록 (여기서 @Body 전달)
        val response = api.uploadQuestionImage(
            coupleQuestionId = coupleQuestionId,
            request = QuestionImageRequest(imageKey = presignedData.imageKey)
        )

        if (!response.isSuccessful || response.body()?.success != true) {
            throw Exception(response.body()?.error?.message ?: "등록 실패")
        }
    }
}