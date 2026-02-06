package org.withus.app.repository

import org.withus.app.model.CoupleKeyword
import org.withus.app.model.KeywordInfo
import org.withus.app.remote.ApiService
import org.withus.app.remote.NetworkResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeywordRepository @Inject constructor(private val apiService: ApiService) {

    // 내 커플 키워드 가져오기
    suspend fun getCoupleKeywords(): NetworkResult<List<CoupleKeyword>> {
        return try {
            val response = apiService.getCoupleKeywords()
            val body = response.body()

            if (response.isSuccessful && body != null && body.success) {
                // 데이터가 null일 경우 빈 리스트 반환
                NetworkResult.Success(body.data?.coupleKeywords ?: emptyList())
            } else {
                NetworkResult.Error(body?.error?.message ?: "Unknown Error", body?.error?.code)
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    // 전체 키워드 목록 가져오기
    suspend fun getAllKeywords(): NetworkResult<List<KeywordInfo>> {
        return try {
            val response = apiService.getAllKeywords()
            val body = response.body()

            if (response.isSuccessful && body != null && body.success) {
                NetworkResult.Success(body.data?.keywordInfoList ?: emptyList())
            } else {
                NetworkResult.Error(body?.error?.message ?: "Unknown Error", body?.error?.code)
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}