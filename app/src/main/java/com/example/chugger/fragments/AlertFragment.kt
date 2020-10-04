package com.example.chugger.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.chugger.R
import kotlinx.android.synthetic.main.fragment_alert.view.*

/**
 * @author Nikojoel
 * AlertFragment
 * Fragment that informs the user of their drinking time
 */
class AlertFragment : Fragment() {

    // Interface variable
    private lateinit var helper: AlertHelper

    companion object {
        // Users drinking time
        private lateinit var time: String

        /**
         * Creates a new instance of AlertFragment
         * @param time Users drinking time
         * @return AlertFragment
         */
        fun newInstance(time: String): AlertFragment {
            this.time = time
            return AlertFragment()
        }
    }

    /**
     * Called when the fragment is attached
     * Sets a custom interface to be used
     * @param context Interface to global information about an application environment
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Initialize the helper interface
        helper = context as AlertHelper
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
        val view = inflater.inflate(R.layout.fragment_alert, container, false)

        // Set view text
        view.desc.text = getString(R.string.drinkTimeString, time)

        // Register a click listener
        view.saveBtn.setOnClickListener {
            // Destroy current fragment and start EditUserFragment
            fragmentManager?.popBackStackImmediate()
            helper.startDbFrag()
        }

        // Register a click listener
        view.cancelBtn.setOnClickListener {
            // Destroy the fragment
            fragmentManager?.popBackStackImmediate()
        }
        return view
    }
    /**
     * Interface used in MainActivity to start the
     * EditUserFragment
     */
    interface AlertHelper {
        /**
         * Implemented in MainActivity
         */
        fun startDbFrag()
    }
}
/* EOF */