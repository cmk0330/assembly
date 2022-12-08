package com.cmk.call

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.cmk.call.event.RtmEventListener
import com.cmk.call.ui.P2PVideoActivity
import com.cmk.call.viewmodel.CallViewModel
import com.cmk.core.BaseActivity
import io.agora.rtm.RemoteInvitation

open class BaseCallActivity : BaseActivity(), RtmEventListener {

    protected val callViewModel by viewModels<CallViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onRemoteInvitationAccepted(remoteInvitation: RemoteInvitation?) {
        super.onRemoteInvitationAccepted(remoteInvitation)
        startActivity(Intent(this, P2PVideoActivity::class.java))
    }
}