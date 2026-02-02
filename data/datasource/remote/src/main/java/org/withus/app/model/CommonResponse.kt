package org.withus.app.model

import com.google.gson.annotations.SerializedName
import org.withus.app.model.request.ErrorDetail

data class CommonResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error") val error: ErrorDetail? = null,
)