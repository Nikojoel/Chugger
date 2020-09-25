package com.example.chugger.adapters

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.chugger.fragments.SwipeFragment

class ScreenSlideAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        Log.d("DBG", "create fragment position: ${position}")
        return SwipeFragment.newInstance(position)
    }

}