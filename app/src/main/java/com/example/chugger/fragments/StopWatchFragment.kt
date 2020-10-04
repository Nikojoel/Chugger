package com.example.chugger.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.chugger.R
import com.example.chugger.timer.Stopwatch
import kotlinx.android.synthetic.main.fragment_stop_watch.*
import kotlinx.android.synthetic.main.fragment_stop_watch.view.*

/**
 * @author Nikojoel
 * StopWatchFragment
 * Fragment that holds a simple stopwatch, counts milliseconds and seconds
 */
class StopWatchFragment() : Fragment() {

    // Create new instance of StopWatch
    private val stopWatch = Stopwatch()

    // Interface variable
    private lateinit var helper: StopWatchHelper

    companion object {
        /**
         * Returns a new instance of StopWatchFragment
         * @return StopWatchFragment
         */
        fun newInstance(): StopWatchFragment {
            return StopWatchFragment()
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
        helper = context as StopWatchHelper
    }

    /**
     * Called when the fragment is destroyed
     * Stops the stopwatch and uses the helper interface
     * to get the users drinking time
     */
    override fun onDestroy() {
        stopWatch.stop()
        helper.getTime(stopWatch.getTotal())
        super.onDestroy()
    }

    /**
     * Inflates a layout and returns it
     * @param inflater Used to inflate the layout
     * @param container Used to determine where to inflate the layout
     * @param savedInstanceState A mapping from String keys to various parcelable values.
     * @return View?
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_stop_watch, container, false)

        // Create new handler
        val handler = Handler()
        // Start the stopwatch
        stopWatch.start()

        // Set views to visible
        view.milliSeconds.visibility = View.VISIBLE
        view.seconds.visibility = View.VISIBLE

        // Causes the Runnable object to be run after the specified amount of time elapses
        handler.postDelayed(object : Runnable {
            /**
             * Updates the stopwatch every 100 milliseconds
             * @see Stopwatch
             * @return View
             */
            override fun run() {

                // Set textviews
                view.milliSeconds.text = stopWatch.elapsedMill()
                view.seconds.text = stopWatch.elapsedSec()

                // Check if time is null (Null after 60 seconds elapsed)
                if (stopWatch.elapsedSec() == null) {
                    // Destroy the fragment
                    activity?.onBackPressed()
                }
                // Check if ProgressBar is not null
                if (progBar != null) {
                    // Set progress
                    progBar.progress = progBar.progress - 1
                }
                handler.postDelayed(this, 100)
            }
        }, 100)

        return view
    }

    /**
     * Interface used in MainActivity to get the
     * users drinking time
     */
    interface StopWatchHelper {
        /**
         * Implemented in MainActivity
         * @param time Users drinking time
         */
        fun getTime(time: Int)
    }
}
/* EOF */