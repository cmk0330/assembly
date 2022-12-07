package com.cmk.call.ui

import android.os.Bundle
import com.cmk.call.BaseCallActivity
import com.cmk.call.databinding.ActivityP2pVideoBinding

class P2PVideoActivity : BaseCallActivity() {

    private val binding by lazy { ActivityP2pVideoBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        intent()
        binding.tvCreateLocal.setOnClickListener {
            callViewModel.createLocalInvitation("", 0, "") {

            }
        }
    }

    private fun intent() {
        intent?.apply {
            val isCalled = getBooleanExtra("IsCalled", false)
            val callerId = getStringExtra("CallerId")
            val callerAvatar = getStringExtra("CallerAvatar")
            val callerName = getStringExtra("CallerName")
        }
    }
}