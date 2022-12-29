package com.cmk.call.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import com.cmk.call.BaseCallActivity
import com.cmk.call.Constant
import com.cmk.call.entity.IntentData
import com.cmk.call.databinding.ActivityP2pBinding
import com.cmk.call.service.FloatVideoWindowService
import com.cmk.common.ext.loge
import com.cmk.common.ext.toast
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.launch

class P2PActivity : BaseCallActivity() {
    private val permissions = arrayListOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val binding by lazy { ActivityP2pBinding.inflate(layoutInflater) }

    private val token =
        "006aaa58676e73f41a086237149d9da6bc4IACcUcKsQCFmaiBjzP/qi8c9WpWh50h7SlKAmI55pTWzP6Pg45sAAAAAEAD1+c6F0JiuYwEA6APQmK5j"
    private val userId = "1234"
    private val token1 =
        "006aaa58676e73f41a086237149d9da6bc4IAA6fMC3zSp82r5zLbFKAhEzgO0VYtHJJYqBBslnT1lpcwdWUn4AAAAAEADRtmedt5iuYwEA6AO3mK5j"
    private val userId1 = "5678"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        PermissionX.init(this).permissions(permissions)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList, "请同意所有权限", "好的", "取消")
            }.request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    toast("已同意权限")
                } else {
                    "为同一全部权限".loge()
                }
            }

        binding.btnMeeting.setOnClickListener {
            moveTaskToBack(true)
            val intent = Intent(this, FloatVideoWindowService::class.java) //开启服务显示悬浮框
            bindService(intent, mVideoServiceConnection, Context.BIND_AUTO_CREATE)
//            startActivity(Intent(this, MeetingCallingVideoActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            lifecycleScope.launch {
                if (callViewModel.login(token, userId)) {
                    "声网登录成功".loge()
                } else {
                    "声网登录失败".loge()
                }
            }
        }
        binding.tvCreateLocal.setOnClickListener {
//            val callList = listOf(
//                CallEntity(
//                    "5678",
//                    "刘德华",
//                    "https://pic2.zhimg.com/v2-c3feed2aaa447b2b6cd204f0b316d0d9_b.jpg"
//                ),
//                CallEntity(
//                    "8765",
//                    "周芷若",
//                    "https://pic1.zhimg.com/v2-35842aaf816bba47d3c0ea7b49ebc6ac_r.jpg"
//                ),
//            )
//
//            callViewModel.queryOnline(callList.map { it.calleeId }) { ids ->
//                if (ids.isEmpty()) {
//                    toast("对方不在线")
//                    return@queryOnline
//                }
//                val onlineList = mutableListOf<CallEntity>()
//                callList.forEach { entity ->
//                    ids.forEach { id ->
//                        if (id == entity.calleeId)
//                            onlineList.add(entity)
//                    }
//                }
//                callViewModel.createLocalInvitationList(
//                    list = onlineList,
//                    channelToken = token,
//                    channelId = userId,
//                    mode = Constant.VIDEO_MODE,
//                    callerName = "测试通话",
//                    callerAvatar = "12312313",
//                ) {
//                    startActivity(Intent(this, CallingVideoActivity::class.java))
//                }
//            }

            callViewModel.queryOnline(userId1) {
                if (!it) {
                    toast("对方不在线")
                    return@queryOnline
                }
                callViewModel.createLocalInvitation(
                    mode = Constant.VIDEO_MODE,
                    calleeId = userId1,
                    channelToken = token,
                    channelId = userId,
                    callerAvatar = "https://pic1.zhimg.com/v2-febba844562e3c685eafa86f2da275b8_r.jpg",
                    callerName = "刘德华",
                    calleeName = "周芷若",
                    calleeAvatar = "https://pic1.zhimg.com/v2-35842aaf816bba47d3c0ea7b49ebc6ac_r.jpg",
                ) {
                    startActivity(Intent(this, CallingVideoActivity::class.java).apply {
                        putExtra("isGroupCall", false)
                        val data = IntentData(
                            calleeId = 5678,
                            callerAvatar = "https://pic1.zhimg.com/v2-febba844562e3c685eafa86f2da275b8_r.jpg",
                            callerName = "刘德华"
                        )
                        val bundle = Bundle()
                        bundle.putParcelable("intent_data", data)
                        putExtras(bundle)
                    })
                }
            }
        }
    }


    private var mVideoServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // 获取服务的操作对象
            val binder = service as FloatVideoWindowService.MyBinder
            binder.service
        }

        override fun onServiceDisconnected(name: ComponentName) {

        }
    }
}