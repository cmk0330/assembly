package com.cmk.call.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cmk.call.R
import com.google.android.material.imageview.ShapeableImageView
import io.agora.rtm.LocalInvitation
import org.json.JSONObject

class LocalInvitationAdapter : RecyclerView.Adapter<LocalInvitationAdapter.LocalInvitationVH>() {

    private val dataList = ArrayList<LocalInvitation>()
    private var onCancelListener: ((LocalInvitation) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalInvitationVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_local_invitation, parent, false)
        return LocalInvitationVH(view)
    }

    override fun onBindViewHolder(holder: LocalInvitationVH, position: Int) {
        val item = dataList.get(position)
        JSONObject(item.content).apply {
            holder.tvCalleeName.text = getString("CalleeName")
            Glide.with(holder.calleeAvatar)
                .load(getString("CalleeAvatar"))
                .into(holder.calleeAvatar)
        }
        holder.ivCancel.setOnClickListener { onCancelListener?.invoke(item) }
    }

    override fun getItemCount() = dataList.size

    fun setOnCancelListener(listener: (LocalInvitation) -> Unit) {
        this.onCancelListener = listener
    }

    fun submitList(list: List<LocalInvitation>) {
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }

    class LocalInvitationVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val calleeAvatar = itemView.findViewById<ShapeableImageView>(R.id.iv_callee_avatar)
        val tvCalleeName = itemView.findViewById<TextView>(R.id.tv_callee_user)
        val ivCancel = itemView.findViewById<ImageView>(R.id.iv_cancel)
    }

}