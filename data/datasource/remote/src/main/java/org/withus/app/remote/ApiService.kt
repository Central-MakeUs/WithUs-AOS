package org.withus.app.remote

import okhttp3.RequestBody
import org.withus.app.model.ArchiveDetailResponse
import org.withus.app.model.ArchiveResponse
import org.withus.app.model.CalendarResponse
import org.withus.app.model.CommonResponse
import org.withus.app.model.CoupleKeywordsData
import org.withus.app.model.CoupleProfileResponse
import org.withus.app.model.CoupleQuestionData
import org.withus.app.model.CreateMemoryRequest
import org.withus.app.model.DailyImageRequest
import org.withus.app.model.InvitationCodeData
import org.withus.app.model.JoinCoupleData
import org.withus.app.model.JoinCouplePreviewData
import org.withus.app.model.JoinCoupleRequest
import org.withus.app.model.KeywordListResponse
import org.withus.app.model.KeywordUpdateRequest
import org.withus.app.model.KeywordsData
import org.withus.app.model.LoginResponse
import org.withus.app.model.MemoryResponse
import org.withus.app.model.PresignedUrlData
import org.withus.app.model.ProfileResponse
import org.withus.app.model.QuestionArchiveResponse
import org.withus.app.model.QuestionDetailResponse
import org.withus.app.model.QuestionImageRequest
import org.withus.app.model.StatusData
import org.withus.app.model.request.LoginRequest
import org.withus.app.model.request.LogoutRequest
import org.withus.app.model.request.PresignedUrlRequest
import org.withus.app.model.request.ProfileUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
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

    /**
     * 내 커플 키워드 목록 조회
     */
    @GET("/api/me/couple/keywords")
    suspend fun getCoupleKeywords(): Response<CommonResponse<CoupleKeywordsData>>

    /**
     * 전체 키워드 목록 조회
     */
    @GET("/api/keywords")
    suspend fun getAllKeywords(): Response<CommonResponse<KeywordsData>>

    // 1. 디폴트 키워드 목록 가져오기
    @GET("/api/keywords/default")
    suspend fun getDefaultKeywords(): Response<CommonResponse<KeywordListResponse>>

    // 2. 키워드 설정 저장하기 (PUT)
    @PUT("/api/me/couple/keywords")
    suspend fun updateCoupleKeywords(
        @Body request: KeywordUpdateRequest
    ): Response<CommonResponse<Unit>>

    @POST("/api/users/{userId}/poke")
    suspend fun pokeUser(
        @Path("userId") userId: Long
    ): Response<CommonResponse<Unit>>

    @GET("/api/me/couple/questions/image")
    suspend fun getTodayQuestion(): Response<CommonResponse<CoupleQuestionData>>

    // 2. 오늘의 질문 사진 업로드
    @POST("/api/me/couple/questions/{coupleQuestionId}/image")
    suspend fun uploadQuestionImage(
        @Path("coupleQuestionId") coupleQuestionId: Long,
        @Body request: QuestionImageRequest // imageKey가 포함된 바디 추가
    ): Response<CommonResponse<Unit>>

    @GET("/api/me/couple/keywords/{coupleKeywordId}/today")
    suspend fun getTodayKeywords(
        @Path("coupleKeywordId") coupleKeywordId: Long
    ): Response<CommonResponse<CoupleQuestionData>> // 응답 구조가 질문과 같으므로 모델 재사용

    // 2. 오늘의 일상 이미지 업로드 (ImageKey 전달)
    @POST("/api/me/couple/keywords/{coupleKeywordId}/today/image")
    suspend fun uploadDailyImage(
        @Path("coupleKeywordId") coupleKeywordId: Long,
        @Body request: DailyImageRequest
    ): Response<CommonResponse<Unit>>

    @DELETE("/api/users/me")
    suspend fun deleteAccount(): Response<CommonResponse<Unit>>

    @POST("/api/me/couple/terminate")
    suspend fun terminateCouple(): Response<CommonResponse<Unit>>

    @GET("/api/me/couple/archives")
    suspend fun getArchives(
        @Query("size") size: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<CommonResponse<ArchiveResponse>>

    @GET("/api/me/couple/archives/date")
    suspend fun getArchiveDetailByDate(
        @Query("date") date: String,
        @Query("targetId") targetId: Long? = null,
        @Query("targetType") targetType: String? = null
    ): Response<CommonResponse<ArchiveDetailResponse>>

    // 월 단위 캘린더 조회
    @GET("/api/me/couple/archives/calendar")
    suspend fun getCalendarArchives(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<CommonResponse<CalendarResponse>>


    @GET("/api/me/couple/archives/questions")
    suspend fun getQuestionArchives(
        @Query("size") size: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<CommonResponse<QuestionArchiveResponse>>

    @GET("/api/me/couple/archives/questions/{coupleQuestionId}")
    suspend fun getQuestionArchiveDetail(
        @Path("coupleQuestionId") coupleQuestionId: Long
    ): Response<CommonResponse<QuestionDetailResponse>>

    @GET("/api/me/couple/memories")
    suspend fun getMemories(
        @Query("monthKey") monthKey: String // YYYYMM 형식
    ): Response<CommonResponse<MemoryResponse>>

    // 추억 생성 API 추가
    @POST("/api/me/couple/memories/{weekEndDate}")
    suspend fun createMemoryWithDate(
        @Path("weekEndDate") weekEndDate: String,
        @Body request: CreateMemoryRequest
    ): Response<CommonResponse<Unit>>

    // 2. 주차 정보 없이 저장 시 (기본값 처리 등)
    @POST("/api/me/couple/memories")
    suspend fun createMemoryNoDate(
        @Body request: CreateMemoryRequest
    ): Response<CommonResponse<Unit>>

    @POST("/api/auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest
    ): Response<CommonResponse<Unit>>

    @GET("/api/me/couple/profile")
    suspend fun getCoupleProfile(): Response<CommonResponse<CoupleProfileResponse>>

}