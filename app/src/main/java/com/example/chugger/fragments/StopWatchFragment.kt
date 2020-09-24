package com.example.chugger.fragments

import android.bluetooth.BluetoothGatt
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.chugger.R
import com.example.chugger.timer.Stopwatch
import kotlinx.android.synthetic.main.fragment_stop_watch.*
import kotlinx.android.synthetic.main.fragment_stop_watch.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class StopWatchFragment() : Fragment() {

    private val stopWatch = Stopwatch()
    private lateinit var helper: StopWatchHelper

    companion object {
        fun newInstance(): StopWatchFragment {
            return StopWatchFragment()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        helper = context as StopWatchHelper
    }

    override fun onDestroy() {
        stopWatch.stop()
        helper.getTime(stopWatch.getTotal())
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_stop_watch, container, false)

        val handler = Handler()
        stopWatch.start()
        view.milliSeconds.visibility = View.VISIBLE
        view.seconds.visibility = View.VISIBLE
        handler.postDelayed(object : Runnable {
            override fun run() {
                view.milliSeconds.text = stopWatch.elapsedMill()
                view.seconds.text = stopWatch.elapsedSec()
                handler.postDelayed(this, 100)
            }
        }, 100)

        return view
    }

    interface StopWatchHelper {
        fun getTime(time: Int)
    }
}