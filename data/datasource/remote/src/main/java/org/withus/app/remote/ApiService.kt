package org.withus.app.remote

import okhttp3.RequestBody
import org.withus.app.model.CommonResponse
import org.withus.app.model.InvitationCodeData
import org.withus.app.model.JoinCoupleData
import org.withus.app.model.JoinCouplePreviewData
import org.withus.app.model.JoinCoupleRequest
import org.withus.app.model.LoginResponse
import org.withus.app.model.PresignedUrlData
import org.withus.app.model.ProfileResponse
import org.withus.app.model.StatusData
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

    @POST("/api/me/status") suspend fun getUserStatus(): Response<CommonResponse<StatusData>>

    @POST("/api/me/couple/join")
    suspend fun joinCouple(
        @Body request: JoinCoupleRequest
    ): Response<CommonResponse<JoinCoupleData>>

    // 초대 코드로 상대 정보 미리보기
    @POST("/api/me/couple/join/preview")
    suspend fun previewJoinCouple(
        @Body request: JoinCoupleRequest
    ): Response<CommonResponse<JoinCouplePreviewData>>

    @POST("/api/me/user/invitation-codes") suspend fun createInvitationCode(): Response<CommonResponse<InvitationCodeData>>
}