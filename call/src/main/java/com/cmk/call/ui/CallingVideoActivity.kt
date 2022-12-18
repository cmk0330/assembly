package com.cmk.call.ui

import android.media.MediaPlayer
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
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
import io.agora.rtm.LocalInvitation
import io.agora.rtm.RtmMessage
import org.json.JSONObject

class CallingVideoActivity : BaseCallActivity() {
    private val TAG = "LocalVideoActivity"
    private val binding by lazy { ActivityP2pVideoBinding.inflate(layoutInflater) }
    private val bindingCalling by lazy { LayoutCallingVideoBinding.inflate(layoutInflater) }
    private val bindingVideo by lazy { LayoutVideoBinding.inflate(layoutInflater) }
    private val ringingPlayer by lazy { MediaPlayer.create(this, R.raw.video_request) }
    private val rtcViewModel by viewModels<RtcViewModel>()
    private var intentData: IntentData? = null // 主动呼叫的用户数据
    private var callMode = 0 // 呼叫模式：0 视频 1 语音
    private val token =
        "006aaa58676e73f41a086237149d9da6bc4IABX8zkMG1pGP/EPGiaTyeUZdWtPTlHX+T9ZXg+D+k05+KPg45sAAAAAIgCErYiMtTOgYwQAAQC1M6BjAgC1M6BjAwC1M6BjBAC1M6Bj"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initReceiveLayout()
        startRing()
        initLivedata()
    }

    private fun initReceiveLayout() {
        binding.root.addView(bindingCalling.root, 1)
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
     * 远端发送的消息回调
     */
    override fun onMessageReceived(rtmMessage: RtmMessage?, uid: String?) {
        rtmMessage?.text?.let {
            if (JSONObject(it).has(Constant.MESSAGE_TYPE)) {
                when (JSONObject(it).get(Constant.MESSAGE_TYPE)) {
                    Constant.END_CALL -> {
                        Toast.makeText(this@CallingVideoActivity, "对方已挂断", Toast.LENGTH_SHORT)
                            .show()
                        leave(false)
                    }
                }
            }
        }
    }

    private fun localJoinRTC() {
        if (callMode == Constant.VIDEO_MODE) {
            binding.root.removeViewAt(1)
            binding.root.addView(bindingVideo.root, 1)
            setupLocalVideo()
            binding.chronometer.apply {
                visibility = View.VISIBLE
                start()
            }
        } else {

        }
        rtcViewModel.joinChannel(
            channelToken = token,
            channelId = callViewModel.currentLocalInvitation?.channelId,
            userId = 1234
        )
        stopRing()
        bindingVideo.apply {
            ivHangUp.setOnClickListener {
                leave(true)
            }
            ivSwitchCamera.setOnClickListener {
                rtcViewModel.switchCamera()
            }
        }
    }

    private fun setupLocalVideo() {
        "setupLocalVideo".loge(TAG)
        val surfaceView = SurfaceView(this)
        bindingVideo.apply {
            flMinScreenVideo.addView(surfaceView) // 默认第一次时远程画面满屏
            rtcViewModel.setupLocalVideo(1234, surfaceView)
        }
    }

    private fun setupRemoteVideo(remoteUid: Int) {
        val surfaceView = SurfaceView(this)
        bindingVideo.apply {
            flFullScreenVideo.addView(surfaceView)
            rtcViewModel.setupRemoveVideo(remoteUid, surfaceView)
        }
    }

    private fun initLivedata() {
        rtcViewModel.remoteVideoDecode.observe(this) {
            setupRemoteVideo(it)
        }
        rtcViewModel.remoteUserOffline.observe(this) {
            if (it.first == callViewModel.currentLocalInvitation?.calleeId?.toInt())
                leave(false)
        }
    }

    /**
     * 离开频道
     * isInitiative: 是否是主动离开
     * rtc的onUserOffline()回调方法可以监听到远程离开频道，
     * 主动发消息是为了防止一方离开而另一方可能没有离开的情况，
     */
    private fun leave(isInitiative: Boolean) {
        if (!isInitiative) {
            finish()
            return
        }
        callViewModel.sendMessage(
            callViewModel.currentLocalInvitation?.calleeId.toString(),
            JSONObject().apply { put(Constant.MESSAGE_TYPE, Constant.END_CALL) }
                .toString()
        ) {
            if (it) {
                rtcViewModel.leaveChannel()
                callViewModel.cancelLocalInvitation() // 这里本可以不调用 但如果是断网重连进来的就需要再取消一下 否则下次无法再呼叫
                finish()
            } else {
                leave(true)
            }
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
}