package org.withus.app.model.request

import com.google.gson.annotations.SerializedName

data class ProfileUpdateRequest(
    @SerializedName("nickname") val nickname: String,
    @SerializedName("birthday") val birthday: String, // "2000-01-02"
    @SerializedName("isImageUpdated") val isImageUpdated: Boolean,
    @SerializedName("imageKey") val imageKey: String?
)