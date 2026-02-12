package org.withus.app.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("jwt") val jwt: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("isInitialized") val isInitialized: Boolean,
)