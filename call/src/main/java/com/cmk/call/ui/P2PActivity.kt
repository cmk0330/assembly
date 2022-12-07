package com.cmk.call.ui

import android.content.Intent
import android.os.Bundle
import com.cmk.call.BaseCallActivity
import com.cmk.call.IntentData
import com.cmk.call.databinding.ActivityP2pBinding
import com.cmk.core.BaseActivity

class P2PActivity : BaseCallActivity() {

    private val binding by lazy { ActivityP2pBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.tvCreateLocal.setOnClickListener {
            callViewModel.createLocalInvitation("", 0, "") {
                startActivity(Intent(this, P2PVideoActivity::class.java).apply {
//                    putExtra("IsCalled", false) // 是否主动呼叫
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