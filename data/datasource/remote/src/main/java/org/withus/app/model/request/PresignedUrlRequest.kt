package org.withus.app.model.request

import com.google.gson.annotations.SerializedName

// presigned 관련 DTO (예시)
data class PresignedUrlRequest(
    @SerializedName("imageType") val imageType: String // PROFILE, MEMORY, FOUR_CUT
)

data class PresignedUrlData(
    @SerializedName("uploadUrl") val uploadUrl: String,
    @SerializedName("imageKey") val imageKey: String
)
