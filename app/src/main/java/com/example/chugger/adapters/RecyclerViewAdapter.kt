package com.example.chugger.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chugger.R
import com.example.chugger.database.User
import kotlinx.android.synthetic.main.user_cell.view.*

class RecyclerViewAdapter(private val users: List<User>) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.user_cell, parent, false))

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.userName.text = users[position].userName
        holder.itemView.userTime.text = "${users[position].time} seconds"
        holder.itemView.cityText.text = users[position].city
    }
}

class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
}