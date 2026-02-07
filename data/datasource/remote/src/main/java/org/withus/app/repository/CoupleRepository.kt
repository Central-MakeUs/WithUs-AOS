package org.withus.app.repository

import org.withus.app.remote.ApiService
import javax.inject.Inject

class CoupleRepository @Inject constructor(
    private val api: ApiService
) {
    suspend fun terminateCouple(): Result<Unit> = runCatching {
        val resp = api.terminateCouple()
        if (!resp.isSuccessful || resp.body()?.success != true) {
            throw Exception(resp.body()?.error?.message ?: "연결 해제 중 오류가 발생했습니다.")
        }
    }
}