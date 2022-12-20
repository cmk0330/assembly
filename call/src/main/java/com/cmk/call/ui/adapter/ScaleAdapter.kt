package com.cmk.call.ui.adapter

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.cmk.call.R
import com.cmk.call.view.ViewWrapper


class ScaleAdapter(val dataList: List<Int>) : RecyclerView.Adapter<ScaleAdapter.ScaleVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScaleVH {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.adapter_scale, parent, false)
        return ScaleVH(view)
    }

    override fun onBindViewHolder(holder: ScaleVH, position: Int) {
        holder.image.setOnClickListener {
            scaleUp(it)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    private fun scaleUp(view: View) {
        val wrapper = ViewWrapper(view)
        ObjectAnimator.ofInt(wrapper, "width", 400)
            .setDuration(200)
            .start()
        ObjectAnimator.ofInt(wrapper, "height", 400)
            .setDuration(200)
            .start()
    }

    private fun scaleDown(view: View) {
        ViewCompat.animate(view)
            .setDuration(200)
            .scaleX(1f)
            .scaleY(1f)
            .start()
    }


    class ScaleVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image = itemView.findViewById<ImageView>(R.id.image)
    }
}