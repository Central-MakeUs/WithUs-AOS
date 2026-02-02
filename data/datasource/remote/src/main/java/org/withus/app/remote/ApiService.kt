package org.withus.app.remote

import okhttp3.RequestBody
import org.withus.app.model.CommonResponse
import org.withus.app.model.LoginResponse
import org.withus.app.model.PresignedUrlData
import org.withus.app.model.ProfileResponse
import org.withus.app.model.request.LoginRequest
import org.withus.app.model.request.PresignedUrlRequest
import org.withus.app.model.request.ProfileUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Url

interface ApiService {

    @POST("/api/images/presigned-url")
    suspend fun getPresignedUrl(
        @Body request: PresignedUrlRequest
    ): Response<CommonResponse<PresignedUrlData>>

    // 2. S3 서버로 이미지 직접 업로드 (PUT)
    @PUT
    suspend fun uploadImageToS3(
        @Url uploadUrl: String,
        @Body image: RequestBody
    ): Response<Unit>

    @POST("/api/auth/login/{provider}")
    suspend fun login(
        @Path("provider") provider: String,
        @Body loginRequest: LoginRequest
    ): Response<CommonResponse<LoginResponse>>

    @GET("/api/me/user/profile")
    suspend fun getUserProfile(): Response<CommonResponse<ProfileResponse>>

    @PUT("/api/me/user/profile")
    suspend fun updateUserProfile(
        @Body profileRequest: ProfileUpdateRequest
    ): Response<CommonResponse<ProfileResponse>>

    @PUT("/api/me/onboarding")
    suspend fun uploadUserProfile(
        @Body profileRequest: ProfileUpdateRequest
    ): Response<CommonResponse<ProfileResponse>>
}

