package com.cmk.call.ui

import android.app.ActionBar.LayoutParams
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.cmk.call.BaseCallActivity
import com.cmk.call.IntentData
import com.cmk.call.R
import com.cmk.call.databinding.ActivityP2pVideoBinding
import com.cmk.call.databinding.LayoutReceiveBinding
import com.cmk.call.viewmodel.RtcViewModel
import com.cmk.core.ext.loge
import io.agora.rtm.LocalInvitation
import io.agora.rtm.RemoteInvitation
import org.json.JSONObject
import kotlin.math.log

class P2PVideoActivity : BaseCallActivity() {

    private val binding by lazy { ActivityP2pVideoBinding.inflate(layoutInflater) }
    private val bindingReceive by lazy { LayoutReceiveBinding.inflate(layoutInflater) }
    private val ringingPlayer by lazy { MediaPlayer.create(this, R.raw.video_request) }
    private val rtcViewModel by viewModels<RtcViewModel>()
    private var isCaller: Boolean = true // 是否主动呼叫
    private var intentData: IntentData? = null // 主动呼叫的用户数据
    private var callMode = 0 // 呼叫模式：0 视频 1 语音
    private var channelId = "" // 频道id

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        binding.root.addView(bindingReceive.root, 0, layoutParams)
        initReceiveLayout()
        startRing()
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
     * 最先初始化接听或呼叫的ui
     */
    private fun initReceiveLayout() {
        intent?.apply {
            isCaller = getBooleanExtra("IsCaller", false)
            intentData = getParcelableExtra("intent_data")
        }
        bindingReceive.apply {
            if (isCaller) { // 主动呼出
                tvCallState.text = "呼叫中"
                Glide.with(this@P2PVideoActivity).load(intentData?.CallerAvatar).into(sivAvatar)
                tvUserName.text = intentData?.CallerName
                callViewModel.sendLocalInvitation()
            } else {
                ivReceiveAccept.isVisible = true
                tvCallState.text = "对方等待接听中"
                callViewModel.currentRemoteInvitation?.let {
                    JSONObject(it.content).apply {
                        callMode = getInt("Mode")
                        Glide.with(this@P2PVideoActivity).load(getString("avatar")).into(sivAvatar)
                        tvUserName.text = getString("userName")
                    }
                }
            }
            ivReceiveAccept.setOnClickListener {
                callViewModel.acceptRemoteInvitation()
            }
            ivReceiveRefuse.setOnClickListener {
                if (isCaller) {
                    callViewModel.cancelLocalInvitation()
                } else {
                    callViewModel.refuseRemoteInvitation()
                }
                stopRing()
            }
        }
    }

    private fun subscribeLivedata() {
        rtcViewModel.apply {
            joinChannelState.observe(this@P2PVideoActivity) {
                if (it == 1) {

                }
            }
        }
    }

    /**
     * 返回给主叫的回调：被叫已接受呼叫邀请
     */
    override fun onLocalInvitationAccepted(localInvitation: LocalInvitation?, var1: String?) {
        super.onLocalInvitationAccepted(localInvitation, var1)
        val jsonObject = JSONObject(var1)
        val toString = localInvitation?.content.toString()
        toString.loge()
    }

    /**
     * 返回给被叫的回调：收到一条呼叫邀请
     */
    override fun onRemoteInvitationReceived(remoteInvitation: RemoteInvitation?) {
        "p2p--->',l".loge()
        remoteInvitation?.content.toString().loge()
    }

    private fun joinRTC() {

    }

    private fun intent() {
        intent?.apply {
            val isCalled = getBooleanExtra("IsCalled", false)
            val callerId = getStringExtra("CallerId")
            val callerAvatar = getStringExtra("CallerAvatar")
            val callerName = getStringExtra("CallerName")
        }
    }
}