package com.cmk.call

import io.agora.rtm.jni.MESSAGE_TYPE

object Constant {
    const val VIDEO_MODE = 0
    const val AUDIO_MODE = 1

    const val MESSAGE_TYPE = "MESSAGE_TYPE"
    const val END_CALL = 1 // 结束通话
    const val CALL_STATE_WAITING = 2
    const val CALL_STATE_RECEIVE = 3
    const val SWITCH_AUDIO = 4
}