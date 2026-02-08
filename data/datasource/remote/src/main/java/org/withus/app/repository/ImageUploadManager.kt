package org.withus.app.repository

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.withus.app.model.request.PresignedUrlRequest
import org.withus.app.remote.ApiService
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class ImageUploadManager @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {
    /**
     * 이미지를 업로드하고 최종 imageKey를 반환합니다.
     * @param bitmap 업로드할 비트맵 데이터
     * @param type 이미지 타입 (예: "PROFILE", "MEMORY")
     */
    suspend fun uploadBitmap(bitmap: Bitmap, type: String): String? {
        return try {
            // 1. Pre-signed URL 발급
            val urlResponse = apiService.getPresignedUrl(PresignedUrlRequest(type))
            if (!urlResponse.isSuccessful || urlResponse.body()?.success != true) return null

            val presignedData = urlResponse.body()!!.data!!
            val uploadUrl = presignedData.uploadUrl
            val imageKey = presignedData.imageKey

            // 2. Bitmap을 ByteArray로 변환
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val requestBody = outputStream.toByteArray().toRequestBody("image/jpeg".toMediaType())

            // 3. S3 업로드
            val s3Response = apiService.uploadImageToS3(uploadUrl, requestBody)
            if (s3Response.isSuccessful) imageKey else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}