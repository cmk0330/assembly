package com.cmk.call.util

import android.app.ActivityManager
import android.content.Context

/**
 * 判断某个服务是否正在运行的方法
 *
 * @param mContext
 * @param className 是包名+服务的类名（例如：net.loonggg.testbackstage.TestService）
 * @return true代表正在运行，false代表服务没有正在运行
 */
fun Context.isServiceWork(className: String): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val serviceList = activityManager.getRunningServices(Int.MAX_VALUE)
    if (serviceList.size <= 0) {
        return false
    }
    for (i in serviceList.indices) {
        val serviceInfo = serviceList[i]
        val serviceName = serviceInfo.service
        if (serviceName.className == className) {
            return true
        }
    }
    return false
}