package com.cmk.call.viewmodel

import android.content.Context
import android.view.SurfaceView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cmk.call.BuildConfig
import com.cmk.call.Constant
import com.cmk.call.ext.Preference
import com.cmk.common.ext.loge
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

class RtcViewModel : ViewModel() {
    private val TAG = "RtcViewModel"
    private var rtcEngine: RtcEngine? = null
    private var haveMemberJoin = false //是否有人加入？用来判断 加入频道后 对方可能因为某些原因 15秒内都未能加入通话 则退出本次通话
    val joinChannelState = MutableLiveData<Int>()
    val remoteUserOffline = MutableLiveData<Pair<Int, Int>>()
    val remoteUserJoin = MutableLiveData<Int>()
    val remoteVideoState = MutableLiveData<Pair<Int, Int>>()
    val remoteVideoDecode = MutableLiveData<Pair<Int, Int>>()

    fun initRtc(context: Context, callType: Int) {
        rtcEngine = RtcEngine.create(context, BuildConfig.AGORA_APPID, IRtcEngineEventHandlerImpl())
        if (callType == Constant.VIDEO_MODE) { // 视频模式
            rtcEngine?.setCameraCapturerConfiguration(
                CameraCapturerConfiguration(
                    CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT,
                    CameraCapturerConfiguration.CaptureFormat(720, 1280, 20)
                )
            )
            val videoEncoderConfiguration = VideoEncoderConfiguration()
            videoEncoderConfiguration.bitrate = 2000
            when (Preference.KEY_FRAME) {
                "24" -> videoEncoderConfiguration.frameRate = 24
                "30" -> videoEncoderConfiguration.frameRate = 30
                "60" -> videoEncoderConfiguration.frameRate = 60
            }
            when (Preference.KEY_DIMENS) {
                "480" -> videoEncoderConfiguration.dimensions =
                    VideoEncoderConfiguration.VD_480x360
                "720" -> videoEncoderConfiguration.dimensions =
                    VideoEncoderConfiguration.VD_1280x720
                "1080" -> videoEncoderConfiguration.dimensions =
                    VideoEncoderConfiguration.VD_1920x1080
            }
            rtcEngine?.let {
                it.setVideoEncoderConfiguration(videoEncoderConfiguration)
                it.enableDualStreamMode(true)
                it.setClientRole(Constants.CLIENT_ROLE_BROADCASTER) // 设置为主播
                it.enableVideo()
            }
        }
    }

    /**
     * 初始化本地视图
     */
    fun setupLocalVideo(uid: Int, surfaceView: SurfaceView) {
        rtcEngine?.let {
            it.setupLocalVideo(
                VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, uid)
            )
            it.startPreview()
        }
    }

    /**
     * 初始化远端试图
     */
    fun setupRemoveVideo(userId: Int, surfaceView: SurfaceView) {
        rtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, userId))
    }

    /**
     * 扬声器开关
     */
    fun setEnableSpeakerphone(open: Boolean) {
        rtcEngine?.setEnableSpeakerphone(open)
    }

    /**
     * 进入频道
     */
    fun joinChannel(channelToken: String?, channelId: String?, userId: Int) {
        val state = rtcEngine?.joinChannel(channelToken, channelId, userId, ChannelMediaOptions())
        "joinState-->$state".loge(TAG)
    }

    /**
     * 离开频道
     */
    fun leaveChannel() {
        rtcEngine?.leaveChannel()
    }

    /**
     * 本地主叫停止发送本地音频流
     */
    fun muteLocalAudioStream(mute: Boolean) {
        rtcEngine?.muteLocalAudioStream(mute)
    }

    /**
     * 本地主叫停止发送本地视频流
     */
    fun muteLocalVideoStream(mute: Boolean) {
        rtcEngine?.muteLocalVideoStream(mute)
    }

    /**
     * 本地主叫关闭视频流
     */
    fun disableVideo() {
        rtcEngine?.disableVideo()
    }

    /**
     * 切换摄像头
     */
    fun switchCamera() {
        rtcEngine?.switchCamera()
    }

    private inner class IRtcEngineEventHandlerImpl : IRtcEngineEventHandler() {

        /**
         * 成功加入频道回调
         */
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            joinChannelState.postValue(1)
            "onJoinChannelSuccess$uid".loge(TAG)
        }

        /**
         * 渲染器已接收首帧远端视频回调 TODO 这个回调不好使
         */
        override fun onFirstRemoteVideoFrame(uid: Int, width: Int, height: Int, elapsed: Int) {
            super.onFirstRemoteVideoFrame(uid, width, height, elapsed)
            remoteVideoDecode.postValue(Pair(uid, -1))
            "onFirstRemoteVideoFrame$uid".loge(TAG)
        }

        /**
         * 本地音频状态发生改变回调
         */
        override fun onLocalAudioStateChanged(state: Int, error: Int) {
            super.onLocalAudioStateChanged(state, error)
        }

        /**
         * 本地视频状态发生改变回调
         */
        override fun onLocalVideoStateChanged(
            source: Constants.VideoSourceType?,
            state: Int,
            error: Int
        ) {
            super.onLocalVideoStateChanged(source, state, error)
        }

        /**
         * 远端视频状态发生改变回调
         */
        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed)
            "onRemoteVideoStateChanged$state".loge(TAG)
            "onRemoteVideoStateChanged$reason".loge(TAG)
            remoteVideoState.postValue(Pair(uid, state))
//            if (reason == Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED ||
//                reason == Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED
//            ) {
//                remoteVideoState.postValue(Pair(uid, reason))
//            }
        }

        /**
         * 远端音频流状态发生改变回调
         */
        override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed)
        }

        /**
         * 远端用户（通信场景）/主播（直播场景）加入当前频道回调
         */
        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            remoteUserJoin.postValue(uid)
            "onUserJoined:$uid".loge(TAG)
        }

        /**
         * 远端用户（通信场景）/主播（直播场景）离开当前频道回调。
         * 0:用户主动离开
         * 1:因过长时间收不到对方数据包，SDK 判定该远端用户超时掉线
         * 2:用户的角色从主播切换为观众
         */
        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            remoteUserOffline.postValue(Pair(uid, reason))
            "onUserOffline$uid".loge(TAG)

//            remoteUserOffline.postValue(Pair(uid, reason))
//            if (reason == Constants.USER_OFFLINE_QUIT) {
//                remoteUserOffline.postValue(Pair(uid, reason))
//            } else
//            if (reason == Constants.USER_OFFLINE_DROPPED) { //对方网络异常
//                remoteUserOffline.postValue(Pair(uid, reason))
//                userOfflineInterval.subscribe {
//                    // 没10秒发送给livedata，在有livedata发送声网云信令检测是否有回执消息，30秒后判断对方是否离线
//                    if (it == 10L || it == 20L || it == 30L) {
//
//                    }
//                }
//                userOfflineInterval.finish {
//                    if (it == OFFONLINE_TIME) { // 异常 则继续等待30秒 30秒内它还未恢复（恢复会走onUserJoin）则离开
//                        remoteUserOffline.postValue(Pair(uid, -1))
//                    }
//                }.start()
//            }

        }
    }

    override fun onCleared() {
        super.onCleared()
        RtcEngine.destroy()
    }
}