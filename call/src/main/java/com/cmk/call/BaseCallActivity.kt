package com.cmk.call

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.*
import com.cmk.call.event.RtmEventListener
import com.cmk.call.ext.shareViewModels
import com.cmk.call.ui.P2PVideoActivity
import com.cmk.call.viewmodel.CallViewModel
import com.cmk.core.BaseActivity
import com.cmk.core.BaseApp
import com.cmk.core.ext.loge
import io.agora.rtm.RemoteInvitation
import kotlinx.coroutines.launch

open class BaseCallActivity : BaseActivity(), RtmEventListener {

    protected val callViewModel by shareViewModels<CallViewModel>("CallViewModel")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callViewModel.register(this)
    }

    /**
     * 返回给被叫的回调：收到一条呼叫邀请
     */
    override fun onRemoteInvitationReceived(remoteInvitation: RemoteInvitation?) {
        startActivity(Intent(this, P2PVideoActivity::class.java)
            .putExtra("IsCaller", false))
    }
}