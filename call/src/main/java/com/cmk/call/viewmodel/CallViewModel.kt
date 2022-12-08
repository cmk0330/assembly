package com.cmk.call.viewmodel

import androidx.lifecycle.ViewModel
import com.cmk.call.BuildConfig.AGORA_APPID
import com.cmk.call.event.RtmEventListener
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
    private var rtmEventListener: RtmEventListener? = null
    private val rtmClient by lazy {
        RtmClient.createInstance(
            BaseApp.application.applicationContext,
            BuildConfig.AGORA_APPID,
            RtmClientListenerImpl()
        )
    }
    val rtmCallManager by lazy { rtmClient.rtmCallManager }
    private var rtmChannel: RtmChannel? = null
    private var currentLocalInvitation: LocalInvitation? = null
    private val localInvitationList = mutableSetOf<LocalInvitation>() // 同时发送多人邀请
    private var remoteInvitation: RemoteInvitation? = null
    private val remoteInvitationList = mutableSetOf<RemoteInvitation>() // 收到多人的呼叫邀请

    private fun register(event: RtmEventListener) {
        this.rtmEventListener = event
    }

    private fun unRegister() {
        this.rtmEventListener = null
    }

    fun release() {
        rtmEventListener = null
        rtmClient.logout(null)
        rtmClient.release()
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
     * 创建单个邀请
     */
    fun createLocalInvitation(
        peerId: String,
        mode: Int,
        channelId: String,
        block: (() -> Unit)? = null
    ) {
        currentLocalInvitation = rtmCallManager.createLocalInvitation(peerId).apply {
            content = JSONObject().apply {
                put("Mode", mode) //// 音频 or 视频
                put("ChannelId", channelId) // 频道id
                put("Conference", false) // 是否是多人童话（会议）

            }.toString()
        }
        block?.invoke()
        val aa: String? = null
    }

    /**
     * 创建多人邀请，一人接听后取消其他邀请
     */
    fun createLocalInvitationList(
        peerIds: List<String>,
        mode: Int,
        channelId: String,
        block: (() -> Unit)? = null
    ) {
        peerIds.forEach {
            rtmCallManager.createLocalInvitation(it).apply {
                content = JSONObject().apply {
                    put("Mode", mode) //// 音频 or 视频
                    put("ChannelId", channelId) // 频道id
                    put("Conference", false) // 是否是多人童话（会议）
                }.toString()
            }
            block?.invoke()
        }
    }

    /**
     * 给单人发送邀请
     */
    fun sendLocalInvitation() {
        rtmCallManager.sendLocalInvitation(currentLocalInvitation, null)
    }

    /**
     * 给多人发送邀请
     */
    fun sendLocalInvitationList() {
        localInvitationList.forEach {
            rtmCallManager.sendLocalInvitation(it, null)
        }
    }

    /**
     * 进入频道
     */
    fun joinRtmChannel(channelId: String) {
        rtmChannel = rtmClient.createChannel(channelId, RtmChannelListenerImpl())
        rtmChannel?.join(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                "${channelId}已进入频道".loge()
            }

            override fun onFailure(p0: ErrorInfo?) {
                "${channelId}进入频道失败:$p0".loge()
            }
        })
    }

    /**
     * 离开频道
     */
    fun leaveRtmChannel() {
        rtmChannel?.let {
            it.leave(null)
            it.release()
        }
    }

    /**
     * rtm im事件
     */
    private inner class RtmClientListenerImpl : RtmClientListener {

        /**
         * SDK 与 Agora RTM 系统的连接状态发生改变回调
         */
        override fun onConnectionStateChanged(state: Int, reason: Int) {
            rtmEventListener?.onConnectionStateChanged(state, reason)
            "onConnectionStateChanged()".loge(TAG)
        }

        /**
         * 收到点对点消息回调。
         */
        override fun onMessageReceived(var1: RtmMessage?, var2: String?) {
            rtmEventListener?.onMessageReceived(var1, var2)
            "onMessageReceived()".loge(TAG)
        }

        /**
         * 	被订阅用户在线状态改变回调。
         */
        override fun onPeersOnlineStatusChanged(var1: MutableMap<String, Int>?) {
            rtmEventListener?.onPeersOnlineStatusChanged(var1)
            "onPeersOnlineStatusChanged()".loge(TAG)
        }

        /**
         * 当前使用的 RTM Token 已超过签发有效期。
         */
        override fun onTokenExpired() {}

        override fun onTokenPrivilegeWillExpire() {}
    }

    /**
     * rtm呼叫事件
     */
    private inner class RtmCallEventListenerImpl : RtmCallEventListener {
        /**
         * 返回给主叫的回调：被叫已收到呼叫邀请。
         */
        override fun onLocalInvitationReceivedByPeer(p0: LocalInvitation?) {
            rtmEventListener?.onLocalInvitationReceivedByPeer(p0)
            p0?.let { localInvitationList.add(it) }
            "onLocalInvitationReceivedByPeer()".loge(TAG)
        }

        /**
         * 返回给主叫的回调：被叫已接受呼叫邀请
         */
        override fun onLocalInvitationAccepted(p0: LocalInvitation?, p1: String?) {
            rtmEventListener?.onLocalInvitationAccepted(p0, p1)
            currentLocalInvitation = p0
            // 当呼叫多人是，其中一者接受后其余将取消呼叫
            localInvitationList.filter { it != p0 }.forEach {
                rtmEventListener?.onLocalInvitationCanceled(it)
            }

            "onLocalInvitationAccepted()".loge(TAG)
        }

        /**
         * 返回给主叫的回调：被叫已拒绝呼叫邀请。
         */
        override fun onLocalInvitationRefused(p0: LocalInvitation?, p1: String?) {
            rtmEventListener?.onLocalInvitationRefused(p0, p1)
            localInvitationList.find { it.calleeId == p0?.calleeId }
                .also { localInvitationList.remove(it) }
            "onLocalInvitationRefused()".loge(TAG)
        }

        /**
         * 返回给主叫的回调：呼叫邀请已被取消。
         */
        override fun onLocalInvitationCanceled(p0: LocalInvitation?) {
            rtmEventListener?.onLocalInvitationCanceled(p0)
            localInvitationList.find { it.calleeId == p0?.calleeId }
                .also { localInvitationList.remove(it) }
            "onLocalInvitationCanceled()".loge(TAG)
        }

        /**
         * 返回给主叫的回调：发出的呼叫邀请失败。可能对方一直没有接听
         */
        override fun onLocalInvitationFailure(p0: LocalInvitation?, p1: Int) {
            rtmEventListener?.onLocalInvitationFailure(p0, p1)
            localInvitationList.find { it.calleeId == p0?.calleeId }
                .also { localInvitationList.remove(it) }
            "onLocalInvitationFailure()".loge(TAG)
        }

        /**
         * 返回给被叫的回调：收到一条呼叫邀请。SDK 会同时返回一个 RemoteInvitation 对象供被叫管理。
         */
        override fun onRemoteInvitationReceived(p0: RemoteInvitation?) {
            rtmEventListener?.onRemoteInvitationReceived(p0)
            "onRemoteInvitationReceived()".loge(TAG)
            // TODO 通知栏或者声音提示
        }

        /**
         * 返回给被叫的回调：接受呼叫邀请。
         */
        override fun onRemoteInvitationAccepted(p0: RemoteInvitation?) {
            rtmEventListener?.onRemoteInvitationAccepted(p0)
            remoteInvitation = p0
            remoteInvitationList.find { it.callerId == p0?.callerId }
                .also { remoteInvitationList.remove(it) }
            "onRemoteInvitationAccepted()".loge(TAG)
        }

        /**
         * 返回给被叫的回调：拒绝呼叫邀请成功
         */
        override fun onRemoteInvitationRefused(p0: RemoteInvitation?) {
            rtmEventListener?.onRemoteInvitationReceived(p0)
            remoteInvitationList.find { it.callerId == p0?.callerId }
                .also { remoteInvitationList.remove(it) }
            "onRemoteInvitationRefused()".loge(TAG)
        }

        /**
         * 返回给被叫的回调：呼叫邀请已取消
         */
        override fun onRemoteInvitationCanceled(p0: RemoteInvitation?) {
            rtmEventListener?.onRemoteInvitationCanceled(p0)
            remoteInvitationList.find { it.callerId == p0?.callerId }
                .also { remoteInvitationList.remove(it) }
            "onRemoteInvitationCanceled()".loge(TAG)
        }

        /**
         * 返回给被叫的回调：发出的呼叫邀请失败
         */
        override fun onRemoteInvitationFailure(p0: RemoteInvitation?, p1: Int) {
            rtmEventListener?.onRemoteInvitationFailure(p0, p1)
            remoteInvitationList.find { it.callerId == p0?.callerId }
                .also { remoteInvitationList.remove(it) }
            "onRemoteInvitationFailure()".loge(TAG)
        }
    }

    /**
     * rtm频道事件
     */
    private inner class RtmChannelListenerImpl : RtmChannelListener {
        /**
         * 频道成员人数更新回调。返回最新频道成员人数。
         */
        override fun onMemberCountUpdated(p0: Int) {
            "onMemberCountUpdated()".loge(TAG)
        }

        /**
         * 	当频道属性更新时返回当前频道的所有属性。
         */
        override fun onAttributesUpdated(p0: MutableList<RtmChannelAttribute>?) {
            "onAttributesUpdated()".loge(TAG)
        }

        /**
         * 收到频道消息。
         */
        override fun onMessageReceived(p0: RtmMessage?, p1: RtmChannelMember?) {
            "onMessageReceived()".loge(TAG)
        }

        /**
         * 远端用户加入频道回调。
         */
        override fun onMemberJoined(p0: RtmChannelMember?) {
            "onMemberJoined()".loge(TAG)
        }

        /**
         * 远端频道成员离开频道回调。
         */
        override fun onMemberLeft(p0: RtmChannelMember?) {
            "onMemberLeft()".loge(TAG)
        }
    }
}