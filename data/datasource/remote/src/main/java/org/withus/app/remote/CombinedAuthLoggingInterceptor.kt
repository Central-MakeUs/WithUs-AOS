package org.withus.app.remote


import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.withus.app.token.TokenManager
import org.withus.app.di.NetworkModule.BASE_URL
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CombinedAuthLoggingInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        // 1. 외부 URL(S3 등)인지 체크
        if (originalUrl.host.contains("amazonaws.com")) {
            Log.d("OkHttp", "외부 URL 요청 감지: S3 업로드를 진행합니다.")

            // S3로 보낼 때는 내 서버의 Bearer 토큰 헤더를 제거해야 합니다. (인증 충돌 방지)
            val s3Request = originalRequest.newBuilder()
                .removeHeader("Authorization")
                .build()

            return chain.proceed(s3Request)
        }

        // 2. 내 서버 API 요청인 경우에만 Base URL 교체 로직 적용
        val newBaseUrl = BASE_URL
        val httpUrl = newBaseUrl.toHttpUrlOrNull()

        val finalRequest = if (httpUrl != null) {
            val newUrl = originalUrl.newBuilder()
                .scheme(httpUrl.scheme)
                .host(httpUrl.host)
                .port(httpUrl.port)
                .build()

            Log.d("OkHttp", "API URL 교체 적용: $newUrl")
            originalRequest.newBuilder().url(newUrl).build()
        } else {
            originalRequest
        }

        val path = finalRequest.url.encodedPath

        // 3. 토큰 추가 로직 (재빌드)
        val authAddedBuilder = finalRequest.newBuilder()
        val token = runBlocking {
            tokenManager.getAccessTokenSync()
        }
        if (!token.isNullOrBlank()) {
            authAddedBuilder.header("Authorization", "Bearer $token")
        }

        val request = authAddedBuilder.build()

        // 5. 로깅 및 실행
        logRequest(request)
        val response = chain.proceed(request)

        return logResponse(response)
    }

    private fun logRequest(request: Request) {
        Log.d("OkHttp", "--> ${request.method} ${request.url}")
        request.headers.forEach { header ->
            Log.d("OkHttp", "header ${header.first}: ${header.second}")
        }

        request.body?.let { requestBody ->
            val contentType = requestBody.contentType().toString()
            if (isImageOrBinary(contentType)) {
                Log.d("OkHttp", "    Request Body: (Binary/Image content, skipped)")
            } else {
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                Log.d("OkHttp", "    Request Body: ${buffer.readString(StandardCharsets.UTF_8)}")
            }
        }
        Log.d("OkHttp", "--> END ${request.method}")
    }

    private fun logResponse(response: Response): Response {
        val request: Request = response.request
        Log.d("OkHttp", "<-- ${response.code} ${response.message} ${request.url}")

        val responseBody = response.body ?: return response
        val contentType = responseBody.contentType()

        if (contentType != null && isImageOrBinary(contentType.toString())) {
            Log.d("OkHttp", "    Response Body: (Binary/Image content, skipped)")
            return response
        }

        return try {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer
            val charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

            val responseBodyString = buffer.clone().readString(charset)
            Log.d("OkHttp", "    Response Body: $responseBodyString")

            // 응답 본문을 다시 생성하여 반환 (스트림 소모 방지)
            val newResponseBody = responseBodyString.toResponseBody(contentType)
            response.newBuilder().body(newResponseBody).build()
        } catch (e: Exception) {
            Log.e("OkHttp", "Failed to read response body", e)
            response
        }
    }

    private fun isImageOrBinary(contentType: String): Boolean {
        return contentType.contains("image/", ignoreCase = true) ||
                contentType.contains("application/octet-stream", ignoreCase = true) ||
                contentType.contains("video/", ignoreCase = true) ||
                contentType.contains("multipart/", ignoreCase = true)
    }
}