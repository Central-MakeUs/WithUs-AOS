package org.withus.app.model

import com.google.gson.annotations.SerializedName

data class InvitationCodeData(
    @SerializedName("invitationCode")
    val invitationCode: String
)
