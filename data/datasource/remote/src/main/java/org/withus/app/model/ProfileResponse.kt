package org.withus.app.model

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @SerializedName("userId") val userId: Int,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("birthday") val birthday: String, // "2000-01-02"
    @SerializedName("profileImageUrl") val profileImageUrl: String?
)