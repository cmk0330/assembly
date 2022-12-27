package com.cmk.call.ui

import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.bumptech.glide.Glide
import com.cmk.call.BaseCallActivity
import com.cmk.call.Constant
import com.cmk.call.R
import com.cmk.call.databinding.ActivityP2pVideoBinding
import com.cmk.call.databinding.LayoutAnswerVideoBinding
import com.cmk.call.databinding.LayoutAudioBinding
import com.cmk.call.databinding.LayoutVideoBinding
import com.cmk.call.ui.adapter.RemoteInvitationAdapter
import com.cmk.call.viewmodel.RtcViewModel
import com.cmk.common.ext.loge
import com.cmk.common.ext.toast
import com.drake.net.time.Interval
import com.drake.net.utils.TipUtils
import io.agora.rtc2.Constants
import io.agora.rtm.RemoteInvitation
import io.agora.rtm.RtmMessage
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AnswerVideoActivity : BaseCallActivity() {
    private val TAG = "AnswerVideoActivity"
    private val binding by lazy { ActivityP2pVideoBinding.inflate(layoutInflater) }
    private val bindingAnswer by lazy { LayoutAnswerVideoBinding.inflate(layoutInflater) }
    private val bindingVideo by lazy { LayoutVideoBinding.inflate(layoutInflater) }
    private val bindingAudio by lazy { LayoutAudioBinding.inflate(layoutInflater) }
    private val ringingPlayer by lazy { MediaPlayer.create(this, R.raw.video_request) }
    private val rtcViewModel by viewModels<RtcViewModel>()

    // userOfflineInterval 收到对方异常离开 倒计30秒 30秒内对方还未恢复 则退出
    private val userOfflineInterval by lazy { Interval(10, 1, TimeUnit.SECONDS, 1) }
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
    }

    override fun onStart() {
        super.onStart()
        initReceiveLayout()
        initLivedata()
    }

    private fun initReceiveLayout() {
        binding.root.addView(bindingAnswer.root, 0)
        bindingAnswer.apply {
            tvCallState.text = "对方等待接听中"
            bindingAnswer.recyclerView.adapter = remoteAdapter
            remoteAdapter.submitList(callViewModel.remoteInvitationList.toMutableList())
            if (callViewModel.remoteInvitationList.isEmpty()) {
                return
            }
            callViewModel.remoteInvitationList.first().let {
                JSONObject(it.content).apply {
                    callMode = getInt("Mode")
                    calleeId = getInt("CalleeId")
                    channelToken = getString("ChannelToken")
                    Glide.with(this@AnswerVideoActivity).load(getString("CallerAvatar"))
                        .into(sivAvatar)
                    tvUserName.text = getString("CallerName")
                    rtcViewModel.initRtc(this@AnswerVideoActivity, callMode)
                }
            }
            remoteAdapter.setOnAcceptListener {
                callViewModel.acceptRemoteInvitation(it)
            }
            remoteAdapter.setOnRefuseListener {
                callViewModel.refuseRemoteInvitation(it)
                stopRing()
                finish()
            }
        }
        startRing()
    }

    /**
     * 音频与视频切换
     */
    private fun switchAudio() {
        binding.root.removeViewAt(0)
        binding.root.addView(bindingAudio.root, 0)
        rtcViewModel.disableVideo()
        bindingAudio.apply {
            JSONObject(callViewModel.currentRemoteInvitation?.content.toString()).apply {
                tvUserName.text = getString("CallerName")
                Glide.with(this@AnswerVideoActivity)
                    .load(getString("CallerAvatar"))
                    .into(sivCalleeAvatar)
            }
            ivCallingCancel.setOnClickListener { leave(true) }
        }
    }

    /**
     * 返回给被叫的回调：接受呼叫邀请成功
     */
    override fun onRemoteInvitationAccepted(remoteInvitation: RemoteInvitation?) {
        super.onRemoteInvitationAccepted(remoteInvitation)
        "onRemoteInvitationAccepted".loge(TAG)
        runOnUiThread { localJoinRTC(remoteInvitation) }
    }

    /**
     * 返回给被叫的回调：拒绝呼叫邀请成功
     */
    override fun onRemoteInvitationRefused(remoteInvitation: RemoteInvitation?) {
        super.onRemoteInvitationRefused(remoteInvitation)
        "onRemoteInvitationRefused()".loge(TAG)
        stopRing()
        finish()
    }

    /**
     * 返回给被叫的回调：呼叫邀请已取消成功
     */
    override fun onRemoteInvitationCanceled(remoteInvitation: RemoteInvitation?) {
        super.onRemoteInvitationCanceled(remoteInvitation)
        "onRemoteInvitationCanceled()".loge(TAG)
        stopRing()
        finish()
        toast("对方已取消")
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
                        }.toString()) {}
                    }

                    Constant.CALL_STATE_RECEIVE -> { // 收到对方发送的回执消息
                        isCallReceive = true
                        toast("当前网络信号差")
                    }
                    Constant.SWITCH_AUDIO -> { // 切换到视频通话

                    }
                }
            }
        }
    }

    private fun localJoinRTC(remoteInvitation: RemoteInvitation?) {
        if (callMode == Constant.VIDEO_MODE) {
            binding.root.removeViewAt(0)
            binding.root.addView(bindingVideo.root, 0)
            setupLocalVideo()
        } else {

        }
        rtcViewModel.joinChannel(
            channelToken = channelToken,
            channelId = remoteInvitation?.channelId,
            userId = calleeId
        )
        bindingVideo.apply {
            ivHangUp.setOnClickListener { leave(true) }
            ivSwitchCamera.setOnClickListener { rtcViewModel.switchCamera() }
            flMinScreenVideo.setOnClickListener { switchLocalRemoteVideo() }
            flFullScreenVideo.setOnClickListener { switchLocalRemoteVideo() }
        }
        stopRing()
    }

    private fun setupLocalVideo() {
        val surfaceView = SurfaceView(this)
        videoMap[KEY_LOCAL] = surfaceView
        bindingVideo.apply {
            flMinScreenVideo.tag = KEY_LOCAL
            flMinScreenVideo.addView(surfaceView)
            rtcViewModel.setupLocalVideo(calleeId, surfaceView)
            ivSwitchAudio.setOnClickListener { switchAudio() }
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
                    TipUtils.toast("对方网络环境较差")
                }
                Constants.REMOTE_VIDEO_STATE_FAILED -> { // 远端视频流播放失败

                }
            }
        }
        rtcViewModel.remoteUserOffline.observe(this) { it ->
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
                }.toString()) {
                    if (it) {
                        userOfflineInterval.finish {
                            if (!isCallReceive) { // 判定为对方掉线
                                leave(false)
                                toast("对方网路异常")
                            }
                        }.start()
                    } else {
                        leave(false)
                        toast("对方网路异常")
                    }
                }
            }
        }
        rtcViewModel.remoteEnableVideoState.observe(this) {
            if (!it.second) {
                switchAudio()
            }
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
}