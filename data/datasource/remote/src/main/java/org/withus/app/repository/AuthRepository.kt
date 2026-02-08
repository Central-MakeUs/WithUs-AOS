package org.withus.app.repository

import org.withus.app.model.CommonResponse
import org.withus.app.model.request.LogoutRequest
import org.withus.app.remote.ApiService
import org.withus.app.token.TokenManager
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    /**
     * 서버에 로그아웃을 요청하고 성공 시 로컬 토큰을 삭제합니다.
     */
    suspend fun logout(fcmToken: String): Response<CommonResponse<Unit>> {
        val response = apiService.logout(LogoutRequest(fcmToken))

        if (response.isSuccessful && response.body()?.success == true) {
            // 서버 로그아웃 성공 시 로컬 토큰 삭제
            tokenManager.deleteAccessToken()
        }
        return response
    }
}