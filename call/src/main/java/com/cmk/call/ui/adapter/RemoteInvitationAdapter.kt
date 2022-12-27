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
import io.agora.rtm.RemoteInvitation
import org.json.JSONObject

class RemoteInvitationAdapter : RecyclerView.Adapter<RemoteInvitationAdapter.RemoteInvitationVH>() {

    private val dataList = ArrayList<RemoteInvitation>()
    private var onAcceptListener: ((RemoteInvitation) -> Unit)? = null
    private var onRefuseListener: ((RemoteInvitation) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteInvitationVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_remote_invitation, parent, false)
        return RemoteInvitationVH(view)
    }

    override fun onBindViewHolder(holder: RemoteInvitationVH, position: Int) {
        val item = dataList.get(position)
        JSONObject(item.content).apply {
            holder.tvCallerName.text = getString("CallerName")
            Glide.with(holder.ivCallerAvatar)
                .load(getString("CallerAvatar"))
                .into(holder.ivCallerAvatar)
        }
        holder.ivAccept.setOnClickListener {
            onAcceptListener?.invoke(dataList[position])
        }
        holder.ivRefuse.setOnClickListener {
            onRefuseListener?.invoke(dataList[position])
        }
    }

    override fun getItemCount() = dataList.size

    fun submitList(list: List<RemoteInvitation>) {
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }

    fun getCurrentList() = dataList

    fun setOnAcceptListener(listener: (RemoteInvitation?) -> Unit) {
        this.onAcceptListener = listener
    }

    fun setOnRefuseListener(listener: (RemoteInvitation?) -> Unit) {
        this.onRefuseListener = listener
    }

    class RemoteInvitationVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAccept: ImageView = itemView.findViewById(R.id.iv_receive_accept)
        val ivRefuse: ImageView = itemView.findViewById(R.id.iv_receive_refuse)
        val tvCallerName = itemView.findViewById<TextView>(R.id.tv_caller_user)
        val ivCallerAvatar = itemView.findViewById<ShapeableImageView>(R.id.iv_caller_avatar)
    }
}