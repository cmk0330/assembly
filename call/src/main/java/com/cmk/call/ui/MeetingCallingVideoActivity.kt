package com.cmk.call.ui

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.cmk.call.BaseCallActivity
import com.cmk.call.databinding.ActivityMeetingCallingVideoBinding
import com.cmk.call.ui.adapter.ScaleAdapter

class MeetingCallingVideoActivity : BaseCallActivity() {

    private val binding by lazy { ActivityMeetingCallingVideoBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val list = ArrayList<Int>()
        for (i in 0..16) {
            list.add(i)
        }
        val scaleAdapter = ScaleAdapter(list)
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MeetingCallingVideoActivity, 2)
            adapter = scaleAdapter
        }
    }

    private fun init() {

    }
}