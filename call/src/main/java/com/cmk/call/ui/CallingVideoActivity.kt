package com.cmk.call.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import android.view.*
import androidx.activity.viewModels
import androidx.lifecycle.observe
import com.bumptech.glide.Glide
import com.cmk.call.BaseCallActivity
import com.cmk.call.Constant
import com.cmk.call.R
import com.cmk.call.databinding.*
import com.cmk.call.entity.IntentData
import com.cmk.call.service.FloatVideoWindowService
import com.cmk.call.ui.adapter.LocalInvitationAdapter
import com.cmk.call.util.isServiceWork
import com.cmk.call.viewmodel.RtcViewModel
import com.cmk.common.ext.loge
import com.drake.net.time.Interval
import com.drake.net.utils.TipUtils.toast
import io.agora.rtc2.Constants
import io.agora.rtm.LocalInvitation
import io.agora.rtm.RtmMessage
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.collections.set


class CallingVideoActivity : BaseCallActivity() {
    //https://toutiao.io/posts/t6jiaw/preview
    private val TAG = "CallingVideoActivity"
    private val binding by lazy { ActivityP2pVideoBinding.inflate(layoutInflater) }
    private val bindingCalling by lazy { LayoutCallingVideoBinding.inflate(layoutInflater) }
    private val bindingCallingGroup by lazy { LayoutCallingGroupVideoBinding.inflate(layoutInflater) }
    private val bindingVideo by lazy { LayoutVideoBinding.inflate(layoutInflater) }
    private val bindingAudio by lazy { LayoutAudioBinding.inflate(layoutInflater) }
    private val ringingPlayer by lazy { MediaPlayer.create(this, R.raw.video_request) }
    private val localAdapter by lazy { LocalInvitationAdapter() }
    private val rtcViewModel by viewModels<RtcViewModel>()

    // userOfflineInterval 收到对方异常离开 倒计30秒 30秒内对方还未恢复 则退出
    private val userOfflineInterval by lazy { Interval(10, 1, TimeUnit.SECONDS, 1) }
    private var callMode = 0 // 呼叫模式：0 视频 1 语音
    private var isGroupCall: Boolean? = false
    private val token =
        "006aaa58676e73f41a086237149d9da6bc4IACcUcKsQCFmaiBjzP/qi8c9WpWh50h7SlKAmI55pTWzP6Pg45sAAAAAEAD1+c6F0JiuYwEA6APQmK5j"
    private val videoMap = mutableMapOf<String, SurfaceView>()
    private val KEY_LOCAL = "local_key"
    private val KEY_REMOTE = "remote_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        isGroupCall = intent?.getBooleanExtra("isGroupCall", false)
        if (isGroupCall == true)
            initGroupReceiveLayout()
        else initReceiveLayout()
        initLivedata()
    }

    /**
     * 群体邀请时
     */
    private fun initGroupReceiveLayout() {
        binding.root.addView(bindingCallingGroup.root, 0)
        bindingCallingGroup.apply {
            val json = JSONObject(callViewModel.localInvitationList.first().content)
            tvUserName.text = json.getString("CalleeName")
            Glide.with(this@CallingVideoActivity)
                .load(json.getString("CalleeAvatar"))
                .into(sivCalleeAvatar)
            tvCallState.text = "呼叫中"
            recyclerView.adapter = localAdapter
            localAdapter.submitList(callViewModel.localInvitationList.toMutableList())
            rtcViewModel.initRtc(this@CallingVideoActivity, callMode)
            localAdapter.setOnCancelListener {
                callViewModel.cancelLocalInvitation()
            }
        }
        startRing()
    }

    /**
     * 一对一邀请时
     */
    private fun initReceiveLayout() {
        binding.root.addView(bindingCalling.root, 0)
        val intentData = intent?.getParcelableExtra<IntentData>("intent_data")
        bindingCalling.apply {
            Glide.with(this@CallingVideoActivity)
                .load(intentData?.callerAvatar)
                .into(sivCalleeAvatar)
            tvUserName.text = intentData?.callerName
            ivCallingCancel.setOnClickListener {
                callViewModel.cancelLocalInvitation()
            }
        }
        rtcViewModel.initRtc(this@CallingVideoActivity, callMode)
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
            JSONObject(callViewModel.currentLocalInvitation?.content.toString()).apply {
                tvUserName.text = getString("CalleeName")
                Glide.with(this@CallingVideoActivity)
                    .load(getString("CalleeAvatar"))
                    .into(sivCalleeAvatar)
            }
            ivCallingCancel.setOnClickListener { leave(true) }
        }
    }

    /**
     * 返回给主叫的回调：被叫已接受呼叫邀请
     */
    override fun onLocalInvitationAccepted(localInvitation: LocalInvitation?, var1: String?) {
        super.onLocalInvitationAccepted(localInvitation, var1)
        "onLocalInvitationAccepted()".loge(TAG)
        runOnUiThread { localJoinRTC(localInvitation) }
    }

    /**
     * 返回给主叫的回调：被叫已拒绝呼叫邀请。
     */
    override fun onLocalInvitationRefused(localInvitation: LocalInvitation?, var1: String?) {
        super.onLocalInvitationRefused(localInvitation, var1)
        if (callViewModel.remoteInvitationList.isEmpty()) {
            stopRing()
            finish()
        }
        toast("对方拒绝")
    }

    /**
     * 返回给被叫的回调：呼叫邀请已取消
     */
    override fun onLocalInvitationCanceled(localInvitation: LocalInvitation?) {
        super.onLocalInvitationCanceled(localInvitation)
        if (callViewModel.remoteInvitationList.isEmpty()) {
            stopRing()
            finish()
        }
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

    @SuppressLint("ClickableViewAccessibility")
    private fun localJoinRTC(localInvitation: LocalInvitation?) {
        if (callMode == Constant.VIDEO_MODE) {
            binding.root.removeViewAt(0)
            binding.root.addView(bindingVideo.root, 0)
            setupLocalVideo()
        } else {

        }
        rtcViewModel.joinChannel(
            channelToken = token,
            channelId = localInvitation?.channelId,
            userId = 1234
        )
        bindingVideo.apply {
            ivHangUp.setOnClickListener { leave(true) }
            ivSwitchCamera.setOnClickListener { rtcViewModel.switchCamera() }
            flMinScreenVideo.setOnClickListener { switchLocalRemoteVideo() }
            ivSwitchAudio.setOnClickListener {
                switchAudio()
//                callViewModel.sendMessage(
//                    userId = callViewModel.currentLocalInvitation?.calleeId.toString(),
//                    msg = JSONObject().apply {
//                        put(Constant.MESSAGE_TYPE, Constant.SWITCH_AUDIO)
//                    }.toString()
//                ) {
//                    if (it) {
//                        switchAudio()
//                    }
//                }
            }
            ivFullScreen.setOnClickListener {
                moveTaskToBack(true)
                if (!isServiceWork(FloatVideoWindowService::class.java.canonicalName)) {
                    val intent = Intent(
                        this@CallingVideoActivity,
                        FloatVideoWindowService::class.java
                    ) //开启服务显示悬浮框
                    bindService(intent, floatServiceConnection, BIND_AUTO_CREATE)
                }
            }

//            flFullScreenVideo.setOnClickListener { switchLocalRemoteVideo() }
        }
        stopRing()
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
                Constants.REMOTE_VIDEO_STATE_FROZEN -> { // 远端视频流卡顿 TODO 切换前后镜头也会触发
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
        rtcViewModel.remoteEnableVideoState.observe(this) {
            if (!it.second) {
                switchAudio()
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

    /**
     * 从最小化状态恢复
     */
    override fun onRestart() {
        super.onRestart()
        if (isServiceWork(FloatVideoWindowService::class.java.canonicalName)) {
            unbindService(floatServiceConnection)
        }
        bindingVideo.flMinScreenVideo.addView(videoMap[bindingVideo.flMinScreenVideo.tag])
        bindingVideo.flFullScreenVideo.addView(videoMap[bindingVideo.flFullScreenVideo.tag])
    }

    private var floatServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // 获取服务的操作对象
            val binder = service as FloatVideoWindowService.MyBinder
            binder.service.initSurfaceView(videoMap[bindingVideo.flFullScreenVideo.tag])
            bindingVideo.apply {
                flFullScreenVideo.removeAllViews()
                flMinScreenVideo.removeAllViews()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(floatServiceConnection)
    }
}