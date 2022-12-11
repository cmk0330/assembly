package com.cmk.call.ui

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.cmk.call.BaseCallActivity
import com.cmk.call.IntentData
import com.cmk.call.databinding.ActivityP2pBinding
import com.cmk.core.BaseActivity
import com.cmk.core.ext.loge
import io.agora.rtm.RemoteInvitation
import kotlinx.coroutines.launch

class P2PActivity : BaseCallActivity() {

    private val binding by lazy { ActivityP2pBinding.inflate(layoutInflater) }

    private val token =
        "006aaa58676e73f41a086237149d9da6bc4IABSlrkzfam/ez8LzMeZYiu8lZfEXb6afU3JsC3atc37PaPg45sAAAAAIgBfxYuoaLSWYwQAAQBotJZjAgBotJZjAwBotJZjBABotJZj"
    private val userId = "1234"
    private val token1 =
        "006aaa58676e73f41a086237149d9da6bc4IABroEwJ340+qDrcgcsRdSETnuzvJxXQYkC5Q9BfeZWriwdWUn4AAAAAIgCPnCP0W7SWYwQAAQBbtJZjAgBbtJZjAwBbtJZjBABbtJZj"
    private val userId1 = "5678"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            lifecycleScope.launch {
                if (callViewModel.login(token1, userId1)) {
                    "声网登录成功".loge()
                } else {
                    "声网登录失败".loge()
                }
            }
        }
        binding.tvCreateLocal.setOnClickListener {
            callViewModel.createLocalInvitation(userId1, 0, "abcd", "123123123", "小小") {
                startActivity(Intent(this, P2PVideoActivity::class.java).apply {
                    putExtra("IsCaller", true) // 是否主动呼叫
//                    putExtra("CallerId", "") // 呼叫者id
//                    putExtra("CallerAvatar", "") // 呼叫者头像
//                    putExtra("CallerName", "") // 呼叫着名称
                    val data = IntentData()
                    val bundle = Bundle()
                    bundle.putParcelable("intent_data", data)
                    putExtras(bundle)
                })
            }
        }
    }
}