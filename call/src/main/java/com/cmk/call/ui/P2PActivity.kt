package com.cmk.call.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.cmk.call.BaseCallActivity
import com.cmk.call.Constant
import com.cmk.call.IntentData
import com.cmk.call.databinding.ActivityP2pBinding
import com.cmk.core.ext.loge
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
        "006aaa58676e73f41a086237149d9da6bc4IAAUEYrnEIW5E7FW05YftEA30rB+k/gvUcUF86CRmOGCJqPg45sAAAAAEAC1T1eSSW2iYwEA6ANJbaJj"
    private val userId = "1234"
    private val token1 =
        "006aaa58676e73f41a086237149d9da6bc4IACGOwoCMxrtOWLKVcZkwh9BTnspykAoG78mFJiKws//4QdWUn4AAAAAEAB523awKW2iYwEA6AMpbaJj"
    private val userId1 = "5678"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        PermissionX.init(this).permissions(permissions)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList, "请同意所有权限", "好的", "取消")
            }.request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    Toast.makeText(this@P2PActivity, "已同意权限", Toast.LENGTH_SHORT).show()
                } else {
                    "为同一全部权限".loge()
                }
            }

        binding.btnMeeting.setOnClickListener {
            startActivity(Intent(this, MeetingCallingVideoActivity::class.java))
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
            callViewModel.queryOnline(userId1) {
                if (!it) {
                    Toast.makeText(this@P2PActivity, "对方不在线", Toast.LENGTH_SHORT).show()
                    return@queryOnline
                }
                callViewModel.createLocalInvitation(
                    calleeId = userId1,
                    callerId = userId,
                    channelToken = token,
                    Constant.VIDEO_MODE,
                    "123123123",
                    "小小"
                ) {
                    startActivity(Intent(this, CallingVideoActivity::class.java).apply {
                        val data = IntentData(callerId = 1234, calleeId = 5678)
                        val bundle = Bundle()
                        bundle.putParcelable("intent_data", data)
                        putExtras(bundle)
                    })
                }
            }
        }
    }
}