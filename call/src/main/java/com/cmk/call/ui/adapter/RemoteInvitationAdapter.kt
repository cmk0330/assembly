package com.cmk.call.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cmk.call.R
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
        val callerName = JSONObject(item.content).getString("UserName")
        holder.tvCallername.text = callerName
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
        val tvCallername = itemView.findViewById<TextView>(R.id.tv_caller_user)
    }
}