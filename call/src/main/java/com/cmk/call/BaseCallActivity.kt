package com.cmk.call

import android.content.Intent
import android.os.Bundle
import com.cmk.call.event.RtmEventListener
import com.cmk.call.ext.shareViewModels
import com.cmk.call.ui.AnswerVideoActivity
import com.cmk.call.viewmodel.CallViewModel
import com.cmk.common.ext.loge
import com.cmk.core.BaseActivity
import io.agora.rtm.LocalInvitation
import io.agora.rtm.RemoteInvitation
import io.agora.rtm.RtmMessage

open class BaseCallActivity : BaseActivity(), RtmEventListener {

    private val TAG = "BaseCallActivity"
    protected val callViewModel by shareViewModels<CallViewModel>("CallViewModel")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        callViewModel.register(this)
    }

    /**
     * 返回给被叫的回调：收到一条呼叫邀请
     */
    override fun onRemoteInvitationReceived(remoteInvitation: RemoteInvitation?) {
        super.onRemoteInvitationReceived(remoteInvitation)
        startActivity(Intent(this, AnswerVideoActivity::class.java))
    }

    override fun onMessageReceived(rtmMessage: RtmMessage?, uid: String?) {
        super.onMessageReceived(rtmMessage, uid)
        "onMessageReceived".loge(TAG)
    }

    override fun onLocalInvitationAccepted(localInvitation: LocalInvitation?, var1: String?) {
//        super.onLocalInvitationAccepted(localInvitation, var1)
        "onLocalInvitationAccepted".loge("BaseActivity")
    }
}