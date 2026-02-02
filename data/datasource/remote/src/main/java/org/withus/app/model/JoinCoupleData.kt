package org.withus.app.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

// 요청
data class JoinCoupleRequest(
    @SerializedName("inviteCode") val inviteCode: String
)

// 실제 연결 응답 데이터
data class JoinCoupleData(
    @SerializedName("coupleId") val coupleId: Long
)

// 미리보기 응답 데이터
@Parcelize
data class JoinCouplePreviewData(
    val senderName: String,
    val receiverName: String,
    val inviteCode: String
) : Parcelable

