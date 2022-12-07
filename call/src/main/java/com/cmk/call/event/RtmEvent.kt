package com.cmk.call.event

import io.agora.rtm.LocalInvitation
import io.agora.rtm.RemoteInvitation
import io.agora.rtm.RtmChannelMember
import io.agora.rtm.RtmMessage

interface RtmEvent {
    fun onConnectionStateChanged(state: Int, reason: Int) {}
    fun onMessageReceived(rtmMessage: RtmMessage?, uid: String?) {}
    fun onPeersOnlineStatusChanged(map: MutableMap<String, Int>?) {}
    fun onMemberJoined(rtmChannelMember: RtmChannelMember?) {}
    fun onMemberLeft(rtmChannelMember: RtmChannelMember?) {}
    fun onLocalInvitationReceivedByPeer(localInvitation: LocalInvitation?) {}
    fun onLocalInvitationAccepted(localInvitation: LocalInvitation?, var1: String?) {}
    fun onLocalInvitationRefused(localInvitation: LocalInvitation?, var1: String?) {}
    fun onLocalInvitationCanceled(localInvitation: LocalInvitation?) {}
    fun onLocalInvitationFailure(localInvitation: LocalInvitation?, var1: Int) {}
    fun onRemoteInvitationReceived(remoteInvitation: RemoteInvitation?) {}
    fun onRemoteInvitationAccepted(remoteInvitation: RemoteInvitation?) {}
    fun onRemoteInvitationRefused(remoteInvitation: RemoteInvitation?) {}
    fun onRemoteInvitationCanceled(remoteInvitation: RemoteInvitation?) {}
    fun onRemoteInvitationFailure(remoteInvitation: RemoteInvitation?, var1: Int) {}
}