package com.example.chugger.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.chugger.R
import com.example.chugger.models.FragmentData
import com.example.chugger.models.SlideHelper
import kotlinx.android.synthetic.main.fragment_swipe.view.*

class SwipeFragment : Fragment() {
    companion object {
        private lateinit var data: FragmentData
        fun newInstance(index: Int): SwipeFragment {
            when (index) {
                0 -> this.data = SlideHelper.slide0
                1 -> this.data = SlideHelper.slide1
                2 -> this.data = SlideHelper.slide2
            }
            return SwipeFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_swipe, container, false)
        view.title.text = data.title
        view.desc.text = data.desc
        //view.image.setImageResource(data.imgResId)

        return view
    }

}