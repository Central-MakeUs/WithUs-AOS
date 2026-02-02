package com.widthus.app.model

sealed class ProfileLoadResult {
    object Success : ProfileLoadResult()
    data class Error(val message: String) : ProfileLoadResult()
}