package org.withus.app.repository

import okhttp3.RequestBody
import org.withus.app.model.request.PresignedUrlRequest
import org.withus.app.remote.ApiService
import org.withus.app.remote.NetworkResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepository @Inject constructor(private val apiService: ApiService) {
    /**
     * @param imageType: "PROFILE", "MEMORY", "FOUR_CUT" 중 하나
     * @param imageRequestBody: .jpg 형식의 이미지 파일 바디
     * @return 성공 시 서버에 저장할 imageKey 반환
     */
    suspend fun uploadImageAndGetKey(
        imageType: String,
        imageRequestBody: RequestBody
    ): NetworkResult<String> {
        return try {
            // 1. Presigned URL 발급 요청
            val urlResponse = apiService.getPresignedUrl(PresignedUrlRequest(imageType))
            val urlBody = urlResponse.body()

            if (urlResponse.isSuccessful && urlBody?.data != null) {
                val uploadUrl = urlBody.data.uploadUrl
                val imageKey = urlBody.data.imageKey // 나중에 도메인 API에 보내야 할 값

                // 2. 응답받은 uploadUrl로 PUT 요청 (S3 업로드)
                val uploadResponse = apiService.uploadImageToS3(uploadUrl, imageRequestBody)

                if (uploadResponse.isSuccessful) {
                    // 성공 시 imageKey를 반환하여 다음 API(프로필 설정 등)에서 쓰도록 함
                    NetworkResult.Success(imageKey)
                } else {
                    NetworkResult.Error("S3 업로드 실패")
                }
            } else {
                NetworkResult.Error(urlBody?.error?.message ?: "URL 발급 실패")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}