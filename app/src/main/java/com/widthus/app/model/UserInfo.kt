package com.widthus.app.model

import android.net.Uri
data class UserInfo(val nickname: String, val birthday: String = "", val profileUrl: Uri? = null)