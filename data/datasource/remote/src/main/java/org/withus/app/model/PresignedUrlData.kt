package org.withus.app.model

import com.google.gson.annotations.SerializedName

data class PresignedUrlData(
    @SerializedName("uploadUrl") val uploadUrl: String,
    @SerializedName("imageKey") val imageKey: String
)