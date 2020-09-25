package com.example.chugger.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.chugger.R
import kotlinx.android.synthetic.main.fragment_alert.view.*


class AlertFragment : Fragment() {

    private lateinit var helper: AlertHelper

    companion object {
        private lateinit var time: String
        fun newInstance(time: String): AlertFragment {
            this.time = time
            return AlertFragment()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        helper = context as AlertHelper
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_alert, container, false)
        view.desc.text = getString(R.string.drinkTimeString, time)
        view.saveBtn.setOnClickListener {
            fragmentManager?.popBackStackImmediate()
            helper.startDbFrag()
        }
        view.cancelBtn.setOnClickListener {
            fragmentManager?.popBackStackImmediate()
        }
        return view
    }

    interface AlertHelper {
        fun startDbFrag()
    }
}