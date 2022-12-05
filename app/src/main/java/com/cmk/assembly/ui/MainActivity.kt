package com.cmk.assembly.ui

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.cmk.assembly.R
import com.cmk.business.RouterPath

@Route(path = "/ui/MainActivity")
class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ARouter.getInstance().inject(this)
        findViewById<TextView>(R.id.text).setOnClickListener {
            ARouter.getInstance().build(RouterPath.BUSINESS)
                .withString("key", "11111")
                .withString("key1", "222222")
                .navigation()
        }
    }
}

class viewMo : ViewModel() {
    fun aa() {
        viewModelScope
    }
}