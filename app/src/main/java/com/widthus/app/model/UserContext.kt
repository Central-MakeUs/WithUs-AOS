package com.widthus.app.model

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.input.TextFieldValue

val LocalUserNickname = compositionLocalOf { UserInfo(nickname = TextFieldValue("Me")) }
val LocalPartnerNickname = compositionLocalOf { UserInfo(nickname = TextFieldValue("Partner")) }
