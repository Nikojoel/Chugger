package com.example.chugger.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chugger.R
import com.example.chugger.database.User
import kotlinx.android.synthetic.main.user_cell.view.*

/**
 * @author Nikojoel
 * RecyclerViewAdapter
 * Recyclerview adapter used to display users from the database
 * @param users List of User objects
 */
class RecyclerViewAdapter(private val users: List<User>) : RecyclerView.Adapter<ViewHolder>() {

    /**
     * Called when a new view holder is needed
     * @param parent Parent view group that can contain children views
     * @param viewType View type of a new view
     * @return ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.user_cell, parent, false))

    // Return the size of the User list
    override fun getItemCount() = users.size

    /**
     * Called when the view holder is bound
     * @param holder View holder
     * @param position Current view holder position
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Set view data
        holder.itemView.userName.text = users[position].userName
        holder.itemView.userTime.text = "${users[position].time} seconds"
        holder.itemView.cityText.text = users[position].city
    }
}

/**
 * @author Nikojoel
 * ViewHolder
 * Custom view holder to be used with the adapter
 */
class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
}
/* EOF */