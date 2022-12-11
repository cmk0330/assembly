package com.cmk.call.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cmk.call.BuildConfig
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

class RtcViewModel : ViewModel() {

    val joinChannelState = MutableLiveData<Int>()
    val remoteVideoState = MutableLiveData<Int>()

    fun initRtc(context: Context) {
        RtcEngine.create(context, BuildConfig.AGORA_APPID, IRtcEngineEventHandlerImpl())
    }

    private inner class IRtcEngineEventHandlerImpl : IRtcEngineEventHandler() {

        /**
         * 成功加入频道回调
         */
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            joinChannelState.value = 1
        }

        /**
         * 渲染器已接收首帧远端视频回调
         */
        override fun onFirstRemoteVideoFrame(uid: Int, width: Int, height: Int, elapsed: Int) {
            super.onFirstRemoteVideoFrame(uid, width, height, elapsed)
            remoteVideoState.value = uid
        }

        override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
            super.onFirstRemoteVideoDecoded(uid, width, height, elapsed)
        }
    }
}