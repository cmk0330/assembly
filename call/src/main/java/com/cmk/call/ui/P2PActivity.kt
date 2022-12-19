package com.cmk.call.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.cmk.call.BaseCallActivity
import com.cmk.call.Constant
import com.cmk.call.IntentData
import com.cmk.call.databinding.ActivityP2pBinding
import com.cmk.core.BaseActivity
import com.cmk.core.ext.loge
import com.permissionx.guolindev.PermissionX
import io.agora.rtm.RemoteInvitation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class P2PActivity : BaseCallActivity() {
    private val permissions = arrayListOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val binding by lazy { ActivityP2pBinding.inflate(layoutInflater) }

    private val token =
        "006aaa58676e73f41a086237149d9da6bc4IAAytZ6pU1s3gOXwuzztyY2dYQRKqFv5jNzpzbQeLOUv9aPg45sAAAAAEACFEd6ky1ShYwEA6APLVKFj"
    private val userId = "1234"
    private val token1 =
        "006aaa58676e73f41a086237149d9da6bc4IACp6UQEaF50w7tYYQSAUfu1D7DvBeLtcUJCA9IqkNyPNgdWUn4AAAAAEACG0Qzu8VShYwEA6APxVKFj"
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
            callViewModel.createLocalInvitation(
                calleeId = userId1,
                callerId = userId,
                channelToken = token,
                Constant.VIDEO_MODE,
                "abcd",
                "123123123",
                "小小"
            ) {
                startActivity(Intent(this, CallingVideoActivity::class.java).apply {
                    putExtra("IsCaller", true) // 是否主动呼叫
                    putExtra("CallerId", "") // 呼叫者id
//                    putExtra("CallerAvatar", "") // 呼叫者头像
//                    putExtra("CallerName", "") // 呼叫着名称
                    val data = IntentData(callerId = 1234, calleeId = 5678)
                    val bundle = Bundle()
                    bundle.putParcelable("intent_data", data)
                    putExtras(bundle)
                })
            }
        }
    }
}