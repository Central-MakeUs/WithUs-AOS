package org.withus.app.model.request

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("oauthToken") val oauthToken: String,
    @SerializedName("fcmToken") val fcmToken: String,
)