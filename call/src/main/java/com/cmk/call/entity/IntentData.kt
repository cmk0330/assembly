package com.cmk.call.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IntentData(
    val IsCalled: Boolean? = false,// 是否主动呼叫
    val calleeId: Int? = null, // 被叫者id
    val callerAvatar: String? = null, // 呼叫者头像
    val callerName: String? = null, // 呼叫着名称
) : Parcelable