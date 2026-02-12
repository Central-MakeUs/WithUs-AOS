package org.withus.app.remote


import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.withus.app.debug
import org.withus.app.token.TokenManager
import org.withus.app.di.NetworkModule.BASE_URL
import org.withus.app.errorLog
import org.withus.app.model.CommonResponse
import org.withus.app.model.request.RefreshRequest
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CombinedAuthLoggingInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val networkLoadingManager: NetworkLoadingManager,
    private val apiService: dagger.Lazy<ApiService>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        networkLoadingManager.showLoading()

        return try {
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            // 1. 외부 URL(S3 등) 체크 및 처리
            if (originalUrl.host.contains("amazonaws.com")) {
                val s3Request = originalRequest.newBuilder()
                    .removeHeader("Authorization")
                    .build()
                return chain.proceed(s3Request)
            }

            // 2. Base URL 및 기존 토큰 추가
            val httpUrl = BASE_URL.toHttpUrlOrNull()
            val initialRequest = originalRequest.newBuilder().apply {
                if (httpUrl != null) {
                    url(
                        originalUrl.newBuilder()
                            .scheme(httpUrl.scheme)
                            .host(httpUrl.host)
                            .port(httpUrl.port)
                            .build()
                    )
                }
                val token = runBlocking { tokenManager.getAccessTokenSync() }
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }.build()

            logRequest(initialRequest)
            var response = chain.proceed(initialRequest)

            logResponse(response)

            // 3. 토큰 만료 에러 체크 (EXPIRED_JWT_TOKEN)
            if (isTokenExpired(response)) {
                debug("토큰 만료 감지 (EXPIRED_JWT_TOKEN): 갱신을 시도합니다.")

                // 4. 새로운 토큰 받아오기 (동기적 실행)
                val newToken = runBlocking {
                    refreshAccessToken()
                }

                if (newToken != null) {
                    // 기존 response 닫기 (메모리 누수 방지)
                    response.close()

                    // 5. 새 토큰으로 기존 요청 재시도
                    val retryRequest = initialRequest.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()

                    debug("토큰 갱신 성공: 재요청을 보냅니다.")
                    response = chain.proceed(retryRequest)
                } else {
                    // 토큰 갱신 실패 (로그아웃 처리 등 필요)
                    debug("토큰 갱신 실패: 로그인이 필요합니다.")
                }
            }

            response


        } catch (e: Exception) {
            errorLog("API Request Failed", e)
            throw e
        } finally {
            // 성공/실패 여부와 상관없이 무조건 카운트 감소
            networkLoadingManager.hideLoading()
        }
    }

    private fun logRequest(request: Request) {
        debug("--> ${request.method} ${request.url}")
        request.headers.forEach { header ->
            debug("header ${header.first}: ${header.second}")
        }

        request.body?.let { requestBody ->
            val contentType = requestBody.contentType().toString()
            if (isImageOrBinary(contentType)) {
                debug("    Request Body: (Binary/Image content, skipped)")
            } else {
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                debug("    Request Body: ${buffer.readString(StandardCharsets.UTF_8)}")
            }
        }
        debug("--> END ${request.method}")
    }

    private fun logResponse(response: Response): Response {
        val request: Request = response.request
        debug("<-- ${response.code} ${response.message}")

        val responseBody = response.body ?: return response
        val contentType = responseBody.contentType()

        if (contentType != null && isImageOrBinary(contentType.toString())) {
            debug("    Response Body: (Binary/Image content, skipped)")
            return response
        }

        return try {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer
            val charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

            val responseBodyString = buffer.clone().readString(charset)
            debug("    Response Body: $responseBodyString")

            // 응답 본문을 다시 생성하여 반환 (스트림 소모 방지)
            val newResponseBody = responseBodyString.toResponseBody(contentType)
            response.newBuilder().body(newResponseBody).build()
        } catch (e: Exception) {
            errorLog("Failed to read response body", e)
            response
        }
    }

    private fun isImageOrBinary(contentType: String): Boolean {
        return contentType.contains("image/", ignoreCase = true) ||
                contentType.contains("application/octet-stream", ignoreCase = true) ||
                contentType.contains("video/", ignoreCase = true) ||
                contentType.contains("multipart/", ignoreCase = true)
    }

    /**
     * Response Body를 파싱하여 에러 코드가 EXPIRED_JWT_TOKEN인지 확인
     */
    private fun isTokenExpired(response: Response): Boolean {
        if (response.isSuccessful) return false

        return try {
            val source = response.peekBody(Long.MAX_VALUE).source()
            val bodyString = source.buffer.clone().readUtf8()
            val commonResponse = Gson().fromJson(bodyString, CommonResponse::class.java)
            commonResponse?.error?.code == "EXPIRED_JWT_TOKEN"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Refresh API를 호출하여 토큰을 갱신하고 저장
     */
    private suspend fun refreshAccessToken(): String? {
        return try {
            val refreshToken = tokenManager.getRefreshTokenSync() ?: return null

            val refreshResponse = apiService.get().refresh(RefreshRequest(refreshToken))

            if (refreshResponse.isSuccessful && refreshResponse.body()?.success == true) {
                val newData = refreshResponse.body()?.data
                if (newData != null) {
                    tokenManager.saveAccessToken(newData.accessToken, newData.refreshToken)
                    return newData.accessToken
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

}