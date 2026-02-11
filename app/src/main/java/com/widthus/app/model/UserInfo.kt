package com.widthus.app.model

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue

data class UserInfo(
    val nickname: TextFieldValue,
    val birthday: String = "",
    val serverProfileUrl: String? = null,
    val selectedLocalUri: Uri? = null)