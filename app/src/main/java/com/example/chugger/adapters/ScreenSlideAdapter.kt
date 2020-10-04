package com.example.chugger.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.chugger.fragments.SwipeFragment

/**
 * @author Nikojoel
 * ScreenSlideAdapter
 * Adapter for the view pager component
 * @param fragment Fragment
 */
class ScreenSlideAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {
    // Wanted amount of fragments
    override fun getItemCount(): Int = 4

    /**
     * Creates a new fragment and passes a position reference
     * that is used to determine which data needs to be displayed
     * @param position Current fragment index
     * @return Fragment
     */
    override fun createFragment(position: Int): Fragment = SwipeFragment.newInstance(position)
}