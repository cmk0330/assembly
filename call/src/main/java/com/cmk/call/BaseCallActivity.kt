package com.cmk.call

import android.os.Bundle
import androidx.activity.viewModels
import com.cmk.call.viewmodel.CallViewModel
import com.cmk.core.BaseActivity

open class BaseCallActivity : BaseActivity() {

    protected val callViewModel by viewModels<CallViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}