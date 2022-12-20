package com.cmk.call.ui

import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.bumptech.glide.Glide
import com.cmk.call.BaseCallActivity
import com.cmk.call.Constant
import com.cmk.call.IntentData
import com.cmk.call.R
import com.cmk.call.databinding.ActivityP2pVideoBinding
import com.cmk.call.databinding.LayoutCallingVideoBinding
import com.cmk.call.databinding.LayoutVideoBinding
import com.cmk.call.viewmodel.RtcViewModel
import com.cmk.core.ext.loge
import com.drake.net.time.Interval
import com.drake.net.utils.TipUtils.toast
import io.agora.rtc2.Constants
import io.agora.rtm.LocalInvitation
import io.agora.rtm.RtmMessage
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.collections.mutableMapOf
import kotlin.collections.set


class CallingVideoActivity : BaseCallActivity() {
    private val TAG = "CallingVideoActivity"
    private val binding by lazy { ActivityP2pVideoBinding.inflate(layoutInflater) }
    private val bindingCalling by lazy { LayoutCallingVideoBinding.inflate(layoutInflater) }
    private val bindingVideo by lazy { LayoutVideoBinding.inflate(layoutInflater) }
    private val ringingPlayer by lazy { MediaPlayer.create(this, R.raw.video_request) }
    private val rtcViewModel by viewModels<RtcViewModel>()

    // userOfflineInterval 收到对方异常离开 倒计30秒 30秒内对方还未恢复 则退出
    private val userOfflineInterval by lazy { Interval(10, 1, TimeUnit.SECONDS, 1) }
    private var intentData: IntentData? = null // 主动呼叫的用户数据
    private var callMode = 0 // 呼叫模式：0 视频 1 语音
    private val token =
        "006aaa58676e73f41a086237149d9da6bc4IAAUEYrnEIW5E7FW05YftEA30rB+k/gvUcUF86CRmOGCJqPg45sAAAAAEAC1T1eSSW2iYwEA6ANJbaJj"
    private val videoMap = mutableMapOf<String, SurfaceView>()
    private val KEY_LOCAL = "local_key"
    private val KEY_REMOTE = "remote_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initReceiveLayout()
        startRing()
        initLivedata()
    }

    private fun initReceiveLayout() {
        binding.root.addView(bindingCalling.root, 0)
        intentData = intent?.getParcelableExtra("intent_data")
        bindingCalling.apply {
            tvCallState.text = "呼叫中"
            Glide.with(this@CallingVideoActivity).load(intentData?.callerAvatar).into(sivAvatar)
            tvUserName.text = intentData?.callerName
            rtcViewModel.initRtc(this@CallingVideoActivity, callMode)
            callViewModel.sendLocalInvitation(intentData?.callerId?.toString())
            ivCallingCancel.setOnClickListener {
                callViewModel.cancelLocalInvitation()
                finish()
            }
        }
    }

    /**
     * 返回给主叫的回调：被叫已接受呼叫邀请
     */
    override fun onLocalInvitationAccepted(localInvitation: LocalInvitation?, var1: String?) {
        "onLocalInvitationAccepted()".loge(TAG)
        val toString = localInvitation?.content.toString()
        toString.loge(TAG)
        runOnUiThread { localJoinRTC() }
    }

    /**
     * 返回给主叫的回调：被叫已拒绝呼叫邀请。
     */
    override fun onLocalInvitationRefused(localInvitation: LocalInvitation?, var1: String?) {
        super.onLocalInvitationRefused(localInvitation, var1)
        finish()
    }

    /**
     * 远端发送的消息回调
     */
    private var isCallReceive = false // 标记对方是否收到云信令并回执
    override fun onMessageReceived(rtmMessage: RtmMessage?, uid: String?) {
        "onMessageReceived".loge(TAG)
        rtmMessage?.text?.let {
            if (JSONObject(it).has(Constant.MESSAGE_TYPE)) {
                when (JSONObject(it).get(Constant.MESSAGE_TYPE)) {
                    Constant.END_CALL -> {
                        toast("对方已挂断")
                        leave(false)
                    }
                    Constant.CALL_STATE_WAITING -> { // 收到对方等待回执的消息
                        callViewModel.sendMessage(uid, JSONObject().apply {
                            put(Constant.MESSAGE_TYPE, Constant.CALL_STATE_RECEIVE)
                        }.toString())
                    }

                    Constant.CALL_STATE_RECEIVE -> { // 收到对方发送的回执消息
                        isCallReceive = true
                        toast("当前网络信号差")
                    }
                }
            }
        }
    }

    private fun localJoinRTC() {
        if (callMode == Constant.VIDEO_MODE) {
            binding.root.removeViewAt(0)
            binding.root.addView(bindingVideo.root, 0)
            setupLocalVideo()
        } else {

        }
        rtcViewModel.joinChannel(
            channelToken = token,
            channelId = callViewModel.currentLocalInvitation?.channelId,
            userId = 1234
        )
        stopRing()
        bindingVideo.apply {
            ivHangUp.setOnClickListener { leave(true) }
            ivSwitchCamera.setOnClickListener { rtcViewModel.switchCamera() }
            flMinScreenVideo.setOnClickListener { switchLocalRemoteVideo() }
//            flFullScreenVideo.setOnClickListener { switchLocalRemoteVideo() }
        }
    }

    private fun setupLocalVideo() {
        val surfaceView = SurfaceView(this)
        videoMap[KEY_LOCAL] = surfaceView
        bindingVideo.apply {
            flMinScreenVideo.tag = KEY_LOCAL
            flMinScreenVideo.addView(surfaceView)
            rtcViewModel.setupLocalVideo(1234, surfaceView)
        }
        setSurfaceViewLayer(true, surfaceView)
        if (bindingVideo.dragLayout.lastLeft != -1) {
            val marginLayoutParams =
                bindingVideo.flMinScreenVideo.layoutParams as ViewGroup.MarginLayoutParams
            marginLayoutParams.leftMargin = bindingVideo.dragLayout.lastLeft
            marginLayoutParams.topMargin = bindingVideo.dragLayout.lastTop
            bindingVideo.flMinScreenVideo.layoutParams = marginLayoutParams
        }
    }

    private fun setupRemoteVideo(remoteUid: Int) {
        val surfaceView = SurfaceView(this)
        videoMap[KEY_REMOTE] = surfaceView
        bindingVideo.apply {
            flFullScreenVideo.tag = KEY_REMOTE
            flFullScreenVideo.addView(surfaceView) // 默认第一次时远程画面满屏
            rtcViewModel.setupRemoveVideo(remoteUid, surfaceView)
            chronometer.apply {
                visibility = View.VISIBLE
                start()
            }
        }
        setSurfaceViewLayer(false, surfaceView)
    }

    /**
     * 本地画面与远程画面切换
     */
    private fun switchLocalRemoteVideo() {
        if (bindingVideo.flMinScreenVideo.tag == KEY_LOCAL
            && bindingVideo.flFullScreenVideo.tag == KEY_REMOTE
        ) {
            bindingVideo.apply {
                flMinScreenVideo.removeAllViews()
                flFullScreenVideo.removeAllViews()
                flMinScreenVideo.addView(videoMap[KEY_REMOTE])
                flFullScreenVideo.addView(videoMap[KEY_LOCAL])
                bindingVideo.flMinScreenVideo.tag = KEY_REMOTE
                bindingVideo.flFullScreenVideo.tag = KEY_LOCAL
            }
            setSurfaceViewLayer(true, videoMap[KEY_REMOTE])
            setSurfaceViewLayer(false, videoMap[KEY_LOCAL])
        } else if (bindingVideo.flMinScreenVideo.tag == KEY_REMOTE
            && bindingVideo.flFullScreenVideo.tag == KEY_LOCAL
        ) {
            bindingVideo.apply {
                flMinScreenVideo.removeAllViews()
                flFullScreenVideo.removeAllViews()
                flMinScreenVideo.addView(videoMap[KEY_LOCAL])
                flFullScreenVideo.addView(videoMap[KEY_REMOTE])
                bindingVideo.flMinScreenVideo.tag = KEY_LOCAL
                bindingVideo.flFullScreenVideo.tag = KEY_REMOTE
            }
            setSurfaceViewLayer(true, videoMap[KEY_LOCAL])
            setSurfaceViewLayer(false, videoMap[KEY_REMOTE])
        }
    }

    private fun initLivedata() {
        rtcViewModel.remoteVideoState.observe(this) {
            when (it.second) {
                Constants.REMOTE_VIDEO_STATE_STARTING -> { // 本地用户已接收远端视频首包
                    setupRemoteVideo(it.first)
                }
                Constants.REMOTE_VIDEO_STATE_PLAYING -> { // 远端视频流正常解码播放

                }
                Constants.REMOTE_VIDEO_STATE_FROZEN -> { // 远端视频流卡顿
                    toast("对方网络环境较差")
                }
                Constants.REMOTE_VIDEO_STATE_FAILED -> { // 远端视频流播放失败

                }
            }
        }
        rtcViewModel.remoteUserOffline.observe(this) {
            if (it.second == Constants.USER_OFFLINE_QUIT) {
                leave(false)
            } else if (it.second == Constants.USER_OFFLINE_DROPPED) {
                // 这里稍微复杂些，收到对方rtcChannel异常消息后，先发送声网云信令给对方，等待对方回执
                // 如果收到对方的消息回执，则当前网络较差
                // 如果30秒还未收到对方回执，则判断对方掉线，然后离开频道finish
                // 可以理解为三次握手
                // 这里是第一次：发送
                callViewModel.sendMessage(it.first.toString(), JSONObject().apply {
                    put(Constant.MESSAGE_TYPE, Constant.CALL_STATE_WAITING)
                }.toString()) {}
                userOfflineInterval.finish {
                    if (!isCallReceive) { // 判定为对方掉线
                        leave(false)
                        toast("对方网络异常")
                    }
                }.start()
            }
        }
    }

    /**
     * 离开频道
     * isInitiative: 是否是主动离开
     * rtc的onUserOffline()回调方法可以监听到远程离开频道，
     * 主动发消息是为了防止一方离开而另一方可能没有离开的情况，
     */
    private fun leave(isInitiative: Boolean) {
        if (isFinishing) return
        if (!isInitiative) {
            finish()
            return
        }
        callViewModel.sendMessage(
            callViewModel.currentLocalInvitation?.calleeId.toString(),
            JSONObject().apply { put(Constant.MESSAGE_TYPE, Constant.END_CALL) }
                .toString()
        ) {
            rtcViewModel.leaveChannel()
            callViewModel.cancelLocalInvitation() // 这里本可以不调用 但如果是断网重连进来的就需要再取消一下 否则下次无法再呼叫
            finish()
//            if (it) {
//                rtcViewModel.leaveChannel()
//                callViewModel.cancelLocalInvitation() // 这里本可以不调用 但如果是断网重连进来的就需要再取消一下 否则下次无法再呼叫
//                finish()
//            } else {
//                leave(true)
//            }
        }
    }

    private fun startRing() {
        ringingPlayer.apply {
            isLooping = true
            start()
        }
    }

    private fun stopRing() {
        ringingPlayer.apply {
            stop()
            release()
        }
    }

    /**
     * 解决两个surfaceView 覆盖的问题
     */
    private fun setSurfaceViewLayer(isOnTop: Boolean, surfaceView: SurfaceView?) {
        val holder = surfaceView?.holder ?: return
        holder.setKeepScreenOn(true)
        holder.setFormat(PixelFormat.TRANSPARENT)
        surfaceView.setZOrderOnTop(isOnTop)
//        surfaceView.setZOrderMediaOverlay(true)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}