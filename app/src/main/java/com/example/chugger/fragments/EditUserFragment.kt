package com.example.chugger.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.chugger.R
import com.example.chugger.database.DbUserModel
import com.example.chugger.database.User
import kotlinx.android.synthetic.main.fragment_edit_user.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Nikojoel
 * EditUserFragment
 * Fragment that has functionality to insert an user to a database
 */
class EditUserFragment : Fragment() {

    companion object {

        // Variables for users drinking time and location
        private lateinit var time: String
        private lateinit var city: String

        /**
         * Creates a new instance of EditUserFragment
         * @param time Users drinking time
         * @param city Users current city location
         * @return EditUserFragment
         */
        fun newInstance(time: String, city: String?, alt: String): EditUserFragment {
            this.time = time
            // Check if city is null (location service not available)
            if (city != null) {
                this.city = city
            } else {
                // Set city to "Not available"
                this.city = alt
            }
            return EditUserFragment()
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
        val view = inflater.inflate(R.layout.fragment_edit_user, container, false)

        // Viewmodel that holds logic for the database
        val ump = ViewModelProvider(this).get(DbUserModel::class.java)

        // Register a click listener
        view.addUserButton.setOnClickListener {
            // Launch a coroutine
            GlobalScope.launch {
                // Users input
                var name = view.editUserName.text.toString()

                if (name.isEmpty()) {
                    name = getString(R.string.noNameText)
                }

                // Insert a new user to the database
                val user = User(0, name, time, city)
                ump.insertNew(user)

                // Calls the specified suspending block with a given coroutine context
                withContext(Dispatchers.Main) {
                    // Make a toast and destroy the fragment
                    Toast.makeText(activity, getString(R.string.savedString), Toast.LENGTH_SHORT).show()
                    activity?.onBackPressed()
                }
            }
        }
        return view
    }
}
/* EOF */