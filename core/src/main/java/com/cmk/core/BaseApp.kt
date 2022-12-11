package com.cmk.core

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.properties.Delegates

open class BaseApp : Application(), ViewModelStoreOwner {

    companion object {
        var application: Application by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        application = this
    }

    override fun getViewModelStore(): ViewModelStore {
        return ViewModelStore()
    }
}