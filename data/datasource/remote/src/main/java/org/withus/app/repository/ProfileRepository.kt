package org.withus.app.repository

import android.app.Application
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.withus.app.model.ApiException
import org.withus.app.model.ProfileResponse
import org.withus.app.model.request.PresignedUrlRequest
import org.withus.app.model.request.ProfileUpdateRequest
import org.withus.app.remote.ApiService
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

//@Singleton
class ProfileRepository @Inject constructor(
    private val api: ApiService,
    private val app: Application // Hilt는 Application을 기본으로 제공합니다.
) {
    // Uri -> ByteArray
    private fun readBytesFromUri(uri: Uri): ByteArray {
        app.contentResolver.openInputStream(uri).use { input ->
            return input?.readBytes() ?: throw IOException("Failed to read image")
        }
    }

    suspend fun uploadImageAndGetKey(imageUri: Uri): Result<String> = runCatching {
        // 1) 파일명과 contentType 추출 (간단히 파일명 생성)
        val fileName = "profile_${System.currentTimeMillis()}.jpg"
        val contentType = app.contentResolver.getType(imageUri) ?: "image/jpeg"

        // 2) presigned url 요청
        val presignedResp = api.getPresignedUrl(PresignedUrlRequest("PROFILE"))
        if (!presignedResp.isSuccessful) throw ApiException(
            presignedResp.code(),
            presignedResp.errorBody()?.string().orEmpty()
        )
        val presignedBody =
            presignedResp.body() ?: throw ApiException(presignedResp.code(), "Empty presigned body")
        if (presignedBody.success != true || presignedBody.data == null) throw ApiException(
            presignedResp.code(),
            presignedBody.error?.message ?: "Presigned failed"
        )

        val uploadUrl = presignedBody.data.uploadUrl
        val imageKey = presignedBody.data.imageKey

        // 3) 로컬 Uri -> 바이트
        val bytes = readBytesFromUri(imageUri)

        // 4) RequestBody 생성 (contentType 사용)
        val requestBody = RequestBody.create(contentType.toMediaTypeOrNull(), bytes)

        // 5) S3 업로드 (PUT)
        val uploadResp = api.uploadImageToS3(uploadUrl, requestBody)
        if (!uploadResp.isSuccessful) throw ApiException(
            uploadResp.code(),
            uploadResp.errorBody()?.string().orEmpty()
        )

        // 6) 성공하면 imageKey 반환
        imageKey
    }

    suspend fun updateUserProfile(request: ProfileUpdateRequest): Result<ProfileResponse> =
        runCatching {
            val resp = api.updateUserProfile(request)
            if (!resp.isSuccessful) throw ApiException(
                resp.code(),
                resp.errorBody()?.string().orEmpty()
            )
            val body = resp.body() ?: throw ApiException(resp.code(), "Empty body")
            if (body.success != true || body.data == null) throw ApiException(
                resp.code(),
                body.error?.message ?: "Update failed"
            )
            body.data
        }
}
