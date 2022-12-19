package com.cmk.call.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.customview.widget.ViewDragHelper


class DragViewLayout(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {
    private val viewDragHelper: ViewDragHelper
    var lastLeft = -1
    var lastTop = -1

    init {
        viewDragHelper = ViewDragHelper.create(this, 1.0f, object : ViewDragHelper.Callback() {
            override fun tryCaptureView(child: View, pointerId: Int): Boolean {
                return if (null != child.tag) {
                    child.tag.toString() == "local_key" || child.tag.toString() == "remote_key"
                } else false
            }

            override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
                if (left < paddingLeft) {
                    return paddingLeft
                }
                val pos = width - child.width - paddingRight
                return left.coerceAtMost(pos)
            }

            override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
                //控制子view拖曳不能超过最顶部
                if (top < paddingTop) {
                    return paddingTop
                }

                //控制子view不能越出底部的边界。
                val pos = height - child.height - paddingBottom
                return top.coerceAtMost(pos)

                //其他情况正常，直接返回Android系统计算的top即可。
            }

            override fun onViewPositionChanged(
                changedView: View,
                left: Int,
                top: Int,
                dx: Int,
                dy: Int
            ) {
                super.onViewPositionChanged(changedView, left, top, dx, dy)
                Log.d(
                    "onViewPositionChanged",
                    "left=" + left + "==top:" + top + "dx==" + dx + "==dy==" + dy
                )
                lastLeft = left
                lastTop = top
            }

            //当手指释放的时候回调
            override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {}
            override fun getViewVerticalDragRange(child: View): Int {
                return measuredHeight - child.measuredHeight
            }

            override fun getViewHorizontalDragRange(child: View): Int {
                return measuredWidth - child.measuredWidth
            }
        })
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return viewDragHelper.shouldInterceptTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        viewDragHelper.processTouchEvent(event)
        return true
    }

    override fun computeScroll() {
        if (viewDragHelper.continueSettling(true)) {
            invalidate()
        }
    }
}