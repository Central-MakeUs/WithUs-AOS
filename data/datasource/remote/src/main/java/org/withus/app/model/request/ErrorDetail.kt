package org.withus.app.model.request

import com.google.gson.annotations.SerializedName

data class ErrorDetail(
    @SerializedName("message") val message: String,
    @SerializedName("code") val code: String
)