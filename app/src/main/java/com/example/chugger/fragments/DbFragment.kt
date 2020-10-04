package com.example.chugger.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chugger.R
import com.example.chugger.adapters.RecyclerViewAdapter
import com.example.chugger.database.DbUserModel
import kotlinx.android.synthetic.main.fragment_db.*
import kotlinx.android.synthetic.main.fragment_db.view.*

/**
 * @author Nikojoel
 * DbFragment
 * Fragment that holds a RecyclerView to display
 * users from the database
 */
class DbFragment : Fragment() {

    companion object {
        /**
         * Creates a new instance of DbFragment
         * @return DbFragment
         */
        fun newInstance() : DbFragment {
            return DbFragment()
        }
    }

    /**
     * Inflates a layout and returns it
     * @param inflater Used to inflate the layout
     * @param container Used to determine where to inflate the layout
     * @param savedInstanceState A mapping from String keys to various parcelable values
     * @return View?
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_db, container, false)

        // Get the activity context
        val activity = activity as Context

        // Set recyclerview layout manager
        view.recView.layoutManager = LinearLayoutManager(activity)

        // View model that holds logic for the database
        val ump = ViewModelProvider(this).get(DbUserModel::class.java)

        // Set an observer for the incoming data from the view model
        ump.getUsers().observe(viewLifecycleOwner, {
            // Set recycler view adapter with data from the database
            recView.adapter = RecyclerViewAdapter(it.sortedBy {that ->
                // Sort the data by users drinking time
                that.time
            })
        })

        return view
    }
}
/* EOF */