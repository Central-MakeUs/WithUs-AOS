package org.withus.app.model.request

import com.google.gson.annotations.SerializedName

data class PresignedUrlRequest(
    @SerializedName("imageType") val imageType: String // "PROFILE", "MEMORY", "FOUR_CUT"
)