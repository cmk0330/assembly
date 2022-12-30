package com.cmk.call.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import com.cmk.call.R
import com.cmk.call.ui.CallingVideoActivity
import com.cmk.common.ext.dp2px
import com.cmk.common.ext.loge
import com.cmk.core.BaseApp
import kotlin.math.abs

class FloatVideoWindowService : Service() {
    private val windowManager by lazy { BaseApp.application.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private val layoutInflater by lazy { LayoutInflater.from(BaseApp.application) }
    private val floatView = layoutInflater.inflate(R.layout.layout_call_float_video, null)
    private val smallPreviewLayout = floatView.findViewById<FrameLayout>(R.id.small_size_preview)
    private var mParams = WindowManager.LayoutParams()

    override fun onBind(intent: Intent?): IBinder? {
        return MyBinder()
    }

    inner class MyBinder : Binder() {
        val service get() = this@FloatVideoWindowService
    }

    override fun onCreate() {
        super.onCreate()
        initWindow()
        initFloating()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        this.windowManager.removeView(floatView)
    }

    fun initSurfaceView(surfaceView: SurfaceView?) {
        if (surfaceView?.parent != null) {
            (surfaceView.parent as ViewGroup).removeView(surfaceView)
            smallPreviewLayout.addView(surfaceView)
            surfaceView.setZOrderMediaOverlay(true)
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun initWindow() {
        mParams = params
        // 悬浮窗默认显示以左上角为起始坐标
        mParams.gravity = Gravity.LEFT or Gravity.TOP
        mParams.x = dp2px(16)
        mParams.y = dp2px(56)
        windowManager.addView(floatView, mParams)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initFloating() {
        smallPreviewLayout.setOnClickListener {
            smallPreviewLayout.removeAllViews()
            val intent = Intent(
                this@FloatVideoWindowService,
                CallingVideoActivity::class.java
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            PendingIntent.getActivity(
                applicationContext, 0, intent, 0
            ).send()
        }
        smallPreviewLayout.setOnTouchListener(floatingListener)
    }

    private val params: WindowManager.LayoutParams
        get() {
            val params = WindowManager.LayoutParams()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                params.type = WindowManager.LayoutParams.TYPE_PHONE
            }
            params.flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            return params
        }

    private var isMove = false

    //开始触控的坐标，移动时的坐标（相对于屏幕左上角的坐标）
    private var mTouchStartX = 0
    private var mTouchStartY = 0
    private var mTouchCurrentX = 0
    private var mTouchCurrentY = 0

    //开始时的坐标和结束时的坐标（相对于自身控件的坐标）
    private var mStartX = 0
    private var mStartY = 0
    private var mStopX = 0
    private var mStopY = 0

    @SuppressLint("ClickableViewAccessibility")
    private val floatingListener = View.OnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isMove = false
                mTouchStartX = event.rawX.toInt()
                mTouchStartY = event.rawY.toInt()
                mStartX = event.x.toInt()
                mStartY = event.y.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                mTouchCurrentX = event.rawX.toInt()
                mTouchCurrentY = event.rawY.toInt()
                mParams.x += mTouchCurrentX - mTouchStartX
                mParams.y += mTouchCurrentY - mTouchStartY
                windowManager.updateViewLayout(floatView, mParams)
                mTouchStartX = mTouchCurrentX
                mTouchStartY = mTouchCurrentY
            }
            MotionEvent.ACTION_UP -> {
                mStopX = event.x.toInt()
                mStopY = event.y.toInt()
                if (abs(mStartX - mStopX) >= 1 || abs(mStartY - mStopY) >= 1) {
                    isMove = true
                }
            }
        }

        //如果是移动事件不触发OnClick事件，防止移动的时候一放手形成点击事件
        isMove
    }
}