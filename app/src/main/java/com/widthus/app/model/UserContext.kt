package com.widthus.app.model

import androidx.compose.runtime.compositionLocalOf

val LocalUserNickname = compositionLocalOf { UserInfo("Me") }
val LocalPartnerNickname = compositionLocalOf { UserInfo("Partner") }
