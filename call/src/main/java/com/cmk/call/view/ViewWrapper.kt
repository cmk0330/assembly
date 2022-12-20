package com.cmk.call.view

import android.view.View

class ViewWrapper(private val target: View) {

    fun setWidth(width: Int) {
        target.layoutParams.width = width
        target.requestLayout()
    }

    fun getWidth(): Int =
        target.layoutParams.width

    fun setHeight(height: Int) {
        target.layoutParams.height = height
        target.requestLayout()
    }

    fun getHeight(): Int = target.layoutParams.height
}