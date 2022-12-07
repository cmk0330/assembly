package com.cmk.call.viewmodel

import android.util.Log
import android.view.Display.Mode
import androidx.lifecycle.ViewModel
import com.cmk.call.BuildConfig.AGORA_APPID
import com.cmk.call.event.RtmEvent
import com.cmk.core.BaseApp
import com.cmk.core.BuildConfig
import com.cmk.core.ext.loge
import io.agora.rtm.*
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CallViewModel : ViewModel() {
    private val TAG = "CallViewModel"

    private var isLogin = false
    private var rtmEvent: RtmEvent? = null
    private val rtmClient by lazy {
        RtmClient.createInstance(
            BaseApp.application.applicationContext,
            BuildConfig.AGORA_APPID,
            RtmClientListenerImpl()
        )
    }
    val rtmCallManager by lazy { rtmClient.rtmCallManager }
    private var localInvitation: LocalInvitation? = null

    private fun register(event: RtmEvent) {
        this.rtmEvent = event
    }

    private fun unRegister() {
        this.rtmEvent = null
    }

    /**
     * 声网登录
     */
    suspend fun login(token: String, userId: String) = suspendCoroutine<Boolean> {
        rtmClient.login(token, userId, object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                isLogin = true
                rtmCallManager.setEventListener(RtmCallEventListenerImpl())
                it.resume(true)
                "声网登录成功".loge(TAG)
            }

            override fun onFailure(p0: ErrorInfo?) {
                isLogin = false
                it.resume(false)
                "声网登录失败::$p0".loge(TAG)
            }
        })
    }

    /**
     * 查询是否在线
     */
    fun queryOnline(peerId: String, block: (Boolean) -> Unit) {
//        queryOnline(HashSet<String>().apply { add(peerId) }) { map ->
//            map?.let {
//                if (it.contains(peerId) && it[peerId]!!)
//                    block.invoke(true)
//                else
//                    block.invoke(false)
//
//            }
//        }
        rtmClient.queryPeersOnlineStatus(
            HashSet<String>().apply { add(peerId) },
            object : ResultCallback<MutableMap<String, Boolean>> {
                override fun onSuccess(map: MutableMap<String, Boolean>?) {
                    map?.let {
                        if (it.contains(peerId) && it[peerId]!!)
                            block.invoke(true)
                        else
                            block.invoke(false)
                    }
                }

                override fun onFailure(p0: ErrorInfo?) {

                }
            })
    }

    fun queryOnline(list: List<String>, block: (MutableList<String>) -> Unit) {
        val onlineList = mutableListOf<String>()
        rtmClient.queryPeersOnlineStatus(list.toSet(),
            object : ResultCallback<MutableMap<String, Boolean>> {
                override fun onSuccess(map: MutableMap<String, Boolean>?) {
                    map?.forEach {
                        if (it.value) onlineList.add(it.key)
                    }
                    block.invoke(onlineList)
                }

                override fun onFailure(p0: ErrorInfo?) {
                    block.invoke(onlineList)
                }
            })
    }

    /**
     * 创建邀请
     */
    fun createLocalInvitation(peerId: String, mode: Int, channelId: String, block: () -> Unit) {
        localInvitation = rtmCallManager.createLocalInvitation(peerId).apply {
            content = JSONObject().apply {
                put("Mode", mode) //// 音频 or 视频
                put("ChannelId", channelId) // 频道id
                put("Conference", false) // 是否是多人童话（会议）
                put("VidCodec", "[\"H264\",\"MJpeg\"]")//适配linux手表端
                put("AudCodec", "[\"Opus\",\"G711\"]")//适配linux手表端
            }.toString()
        }
        block.invoke()
    }

    private inner class RtmClientListenerImpl : RtmClientListener {

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            rtmEvent?.onConnectionStateChanged(state, reason)
            "onConnectionStateChanged()".loge(TAG)
        }

        override fun onMessageReceived(var1: RtmMessage?, var2: String?) {
            rtmEvent?.onMessageReceived(var1, var2)
            "onMessageReceived()".loge(TAG)
        }

        override fun onPeersOnlineStatusChanged(var1: MutableMap<String, Int>?) {
            rtmEvent?.onPeersOnlineStatusChanged(var1)
            "onPeersOnlineStatusChanged()".loge(TAG)
        }

        override fun onTokenExpired() {}

        override fun onTokenPrivilegeWillExpire() {}
    }

    private inner class RtmCallEventListenerImpl : RtmCallEventListener {
        override fun onLocalInvitationReceivedByPeer(p0: LocalInvitation?) {
            rtmEvent?.onLocalInvitationReceivedByPeer(p0)
            "onLocalInvitationReceivedByPeer()".loge(TAG)
        }

        override fun onLocalInvitationAccepted(p0: LocalInvitation?, p1: String?) {
            rtmEvent?.onLocalInvitationAccepted(p0, p1)
            "onLocalInvitationAccepted()".loge(TAG)
        }

        override fun onLocalInvitationRefused(p0: LocalInvitation?, p1: String?) {
            rtmEvent?.onLocalInvitationRefused(p0, p1)
            "onLocalInvitationRefused()".loge(TAG)
        }

        override fun onLocalInvitationCanceled(p0: LocalInvitation?) {
            rtmEvent?.onLocalInvitationCanceled(p0)
            "onLocalInvitationCanceled()".loge(TAG)
        }

        override fun onLocalInvitationFailure(p0: LocalInvitation?, p1: Int) {
            rtmEvent?.onLocalInvitationFailure(p0, p1)
            "onLocalInvitationFailure()".loge(TAG)
        }

        override fun onRemoteInvitationReceived(p0: RemoteInvitation?) {
            rtmEvent?.onRemoteInvitationReceived(p0)
            "onRemoteInvitationReceived()".loge(TAG)
        }

        override fun onRemoteInvitationAccepted(p0: RemoteInvitation?) {
            rtmEvent?.onRemoteInvitationAccepted(p0)
            "onRemoteInvitationAccepted()".loge(TAG)
        }

        override fun onRemoteInvitationRefused(p0: RemoteInvitation?) {
            rtmEvent?.onRemoteInvitationReceived(p0)
            "onRemoteInvitationRefused()".loge(TAG)
        }

        override fun onRemoteInvitationCanceled(p0: RemoteInvitation?) {
            rtmEvent?.onRemoteInvitationCanceled(p0)
            "onRemoteInvitationCanceled()".loge(TAG)
        }

        override fun onRemoteInvitationFailure(p0: RemoteInvitation?, p1: Int) {
            rtmEvent?.onRemoteInvitationFailure(p0, p1)
            "onRemoteInvitationFailure()".loge(TAG)
        }
    }
}