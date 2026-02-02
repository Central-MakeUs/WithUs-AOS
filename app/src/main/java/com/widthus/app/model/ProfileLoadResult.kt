package com.widthus.app.model

sealed class ProfileLoadResult {
    object Success : ProfileLoadResult()      // 데이터가 있어 홈으로 이동
    object ToOnboarding : ProfileLoadResult() // 데이터가 없어 온보딩으로 이동
    data class Error(val message: String) : ProfileLoadResult() // 에러 발생
}