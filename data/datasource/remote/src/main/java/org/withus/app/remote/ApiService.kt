package org.withus.app.remote

import org.withus.app.model.CommonResponse
import org.withus.app.model.LoginResponse
import org.withus.app.model.request.LoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("/api/auth/login/{provider}")
    suspend fun login(
        @Path("provider") provider: String,
        @Body loginRequest: LoginRequest
    ): Response<CommonResponse<LoginResponse>>
}