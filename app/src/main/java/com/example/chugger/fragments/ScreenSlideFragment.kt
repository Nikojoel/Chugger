package com.example.chugger.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.example.chugger.R
import com.example.chugger.adapters.ScreenSlideAdapter
import kotlinx.android.synthetic.main.fragment_screen_slide.*
import kotlinx.android.synthetic.main.fragment_screen_slide.view.*

class ScreenSlideFragment : Fragment() {

    companion object {
        fun newInstance(): ScreenSlideFragment {
            return ScreenSlideFragment()
        }
    }
    private lateinit var adapter: ScreenSlideAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_screen_slide, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ScreenSlideAdapter(this)
        pager.adapter = adapter
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    3 -> {
                        view.skipBtn.text = getString(R.string.getStartedBtnString)
                    }
                }
                super.onPageSelected(position)
            }
        })
        view.skipBtn.setOnClickListener {
           fragmentManager?.popBackStackImmediate()
        }
    }
}