package com.example.chugger.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chugger.R
import com.example.chugger.adapters.RecyclerViewAdapter
import com.example.chugger.database.DbUserModel
import com.example.chugger.database.User
import kotlinx.android.synthetic.main.fragment_db.*
import kotlinx.android.synthetic.main.fragment_db.view.*

class DbFragment : Fragment() {
    companion object {
        fun newInstance() : DbFragment {
            return DbFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_db, container, false)
        val activity = activity as Context
        view.recView.layoutManager = LinearLayoutManager(activity)

        val ump = ViewModelProvider(this).get(DbUserModel::class.java)
        ump.getUsers().observe(viewLifecycleOwner, {
            recView.adapter = RecyclerViewAdapter(it.sortedBy {that ->
                that.time
            })
            Log.d("DBG", it.toString())
        })

        return view
    }
}