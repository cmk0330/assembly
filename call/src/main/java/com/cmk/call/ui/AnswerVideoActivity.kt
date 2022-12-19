package com.cmk.call.ui

import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.cmk.call.BaseCallActivity
import com.cmk.call.Constant
import com.cmk.call.R
import com.cmk.call.databinding.ActivityP2pVideoBinding
import com.cmk.call.databinding.LayoutAnswerVideoBinding
import com.cmk.call.databinding.LayoutVideoBinding
import com.cmk.call.ui.adapter.RemoteInvitationAdapter
import com.cmk.call.viewmodel.RtcViewModel
import com.cmk.core.ext.loge
import io.agora.rtm.RemoteInvitation
import io.agora.rtm.RtmMessage
import org.json.JSONObject

class AnswerVideoActivity : BaseCallActivity() {
    private val TAG = "AnswerVideoActivity"
    private val binding by lazy { ActivityP2pVideoBinding.inflate(layoutInflater) }
    private val bindingAnswer by lazy { LayoutAnswerVideoBinding.inflate(layoutInflater) }
    private val bindingVideo by lazy { LayoutVideoBinding.inflate(layoutInflater) }
    private val ringingPlayer by lazy { MediaPlayer.create(this, R.raw.video_request) }
    private val rtcViewModel by viewModels<RtcViewModel>()
    private val remoteAdapter by lazy { RemoteInvitationAdapter() }
    private var callMode = 0 // 呼叫模式：0 视频 1 语音
    private var calleeId = 0 // 被叫者id
    private var channelToken = ""
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
        binding.root.addView(bindingAnswer.root, 1)
        bindingAnswer.apply {
            tvCallState.text = "对方等待接听中"
            bindingAnswer.recyclerView.adapter = remoteAdapter
            remoteAdapter.submitList(callViewModel.remoteInvitationList.toMutableList())
            if (callViewModel.remoteInvitationList.isEmpty()) return
            callViewModel.remoteInvitationList.first().let {
                JSONObject(it.content).apply {
                    callMode = getInt("Mode")
                    calleeId = getInt("CalleeId")
                    channelToken = getString("ChannelToken")
                    Glide.with(this@AnswerVideoActivity).load(getString("Avatar")).into(sivAvatar)
                    tvUserName.text = getString("UserName")
                    rtcViewModel.initRtc(this@AnswerVideoActivity, callMode)
                }
            }
            remoteAdapter.setOnAcceptListener {
                callViewModel.acceptRemoteInvitation(it)
            }
            remoteAdapter.setOnRefuseListener {
                callViewModel.refuseRemoteInvitation()
                finish()
            }
        }
    }

    /**
     * 返回给被叫的回调：接受呼叫邀请成功
     */
    override fun onRemoteInvitationAccepted(remoteInvitation: RemoteInvitation?) {
        "onRemoteInvitationAccepted".loge(TAG)
        runOnUiThread { localJoinRTC() }
    }

    /**
     * 远端发送的消息回调
     */
    override fun onMessageReceived(rtmMessage: RtmMessage?, uid: String?) {
        "onMessageReceived".loge(TAG)
        rtmMessage?.text?.let {
            if (JSONObject(it).has(Constant.MESSAGE_TYPE)) {
                when (JSONObject(it).get(Constant.MESSAGE_TYPE)) {
                    Constant.END_CALL -> {
                        Toast.makeText(this@AnswerVideoActivity, "对方已挂断", Toast.LENGTH_SHORT)
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
            channelToken = channelToken,
            channelId = callViewModel.currentRemoteInvitation?.channelId,
            userId = calleeId
        )
        stopRing()
        bindingVideo.apply {
            ivHangUp.setOnClickListener { leave(true) }
            ivSwitchCamera.setOnClickListener { rtcViewModel.switchCamera() }
            flMinScreenVideo.setOnClickListener { switchLocalRemoteVideo() }
            flFullScreenVideo.setOnClickListener { switchLocalRemoteVideo() }
        }
    }

    private fun setupLocalVideo() {
        val surfaceView = SurfaceView(this)
        videoMap[KEY_LOCAL] = surfaceView
        bindingVideo.apply {
            flMinScreenVideo.tag = KEY_LOCAL
            flMinScreenVideo.addView(surfaceView)
            rtcViewModel.setupLocalVideo(calleeId, surfaceView)
        }
        setSurfaceViewLayer(true, surfaceView)
    }

    private fun setupRemoteVideo(remoteUid: Int) {
        val surfaceView = SurfaceView(this)
        videoMap[KEY_REMOTE] = surfaceView
        bindingVideo.apply {
            flFullScreenVideo.tag = KEY_REMOTE
            flFullScreenVideo.addView(surfaceView)
            rtcViewModel.setupRemoveVideo(remoteUid, surfaceView)
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
        rtcViewModel.remoteVideoDecode.observe(this) {
            setupRemoteVideo(it)
        }
        rtcViewModel.remoteUserOffline.observe(this) {
            if (it.first == callViewModel.currentRemoteInvitation?.callerId?.toInt())
                leave(false)
        }
    }

    /**
     * 离开频道
     * isInitiative: 是否是主动离开
     */
    private fun leave(isInitiative: Boolean) {
        if (isFinishing) return
        if (!isInitiative) {
            finish()
            return
        }
        callViewModel.sendMessage(
            callViewModel.currentRemoteInvitation?.callerId.toString(),
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
}