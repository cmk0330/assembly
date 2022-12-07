package com.cmk.call

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IntentData(
    val IsCalled: Boolean? = false,// 是否主动呼叫
    val CallerId: String? = null, // 呼叫者id
    val CallerAvatar: String? = null, // 呼叫者头像
    val CallerName: String? = null // 呼叫着名称
) : Parcelable