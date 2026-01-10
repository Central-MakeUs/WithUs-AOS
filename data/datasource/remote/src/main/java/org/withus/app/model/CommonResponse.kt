package org.withus.app.model

import com.google.gson.annotations.SerializedName

data class CommonResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error") val error: T? = null,
)