package com.cmk.business.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.cmk.business.R
import com.cmk.business.RouterPath

@Route(path = RouterPath.BUSINESS)
class BusinessActivity : AppCompatActivity() {
    @Autowired
    val key: String = ""

    @Autowired
    val key1: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ARouter.getInstance().inject(this)
        Log.e("key-->", key)
        Log.e("key1-->", key1)
    }
}