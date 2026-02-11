package org.withus.app.repository

import org.withus.app.remote.ApiService
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val api: ApiService,
) {
    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val resp = api.deleteAccount()
        if (!resp.isSuccessful || resp.body()?.success != true) {
            throw Exception(resp.body()?.error?.message ?: "회원 탈퇴 처리 중 오류가 발생했습니다.")
        }
    }
}