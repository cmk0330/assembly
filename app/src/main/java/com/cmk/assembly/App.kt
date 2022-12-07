package com.cmk.assembly

import android.app.Application
import com.alibaba.android.arouter.BuildConfig
import com.alibaba.android.arouter.launcher.ARouter
import com.cmk.business.BaseApp

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        initARouter()
    }

    private fun initARouter() {
        if (BuildConfig.DEBUG) {
            ARouter.openLog()
            ARouter.openDebug()
        }
        ARouter.init(this)
    }
}