package com.cmk.core

import android.app.Application
import kotlin.properties.Delegates

open class BaseApp : Application() {

    companion object {
        var application: Application by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        application = this
    }
}