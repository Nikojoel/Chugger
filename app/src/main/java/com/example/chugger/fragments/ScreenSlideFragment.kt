package com.example.chugger.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.example.chugger.R
import com.example.chugger.adapters.ScreenSlideAdapter
import kotlinx.android.synthetic.main.fragment_screen_slide.*
import kotlinx.android.synthetic.main.fragment_screen_slide.view.*

/**
 * @author Nikojoel
 * ScreenSlideFragment
 * Fragment that holds a ViewPager, contains the adapter for it
 */
class ScreenSlideFragment : Fragment() {

    companion object {
        /**
         * Creates a new instance of ScreenSlideFragment
         * @return ScreenSlideFragment
         */
        fun newInstance(): ScreenSlideFragment {
            return ScreenSlideFragment()
        }
    }

    // Adapter for the ScreenSlider
    private lateinit var adapter: ScreenSlideAdapter

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
        return inflater.inflate(R.layout.fragment_screen_slide, container, false)
    }

    /**
     * Sets an adapter for the layout that displays all the data
     * @param view View returned from onCreateView
     * @param savedInstanceState A mapping from String keys to various parcelable values.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize and set the adapter
        adapter = ScreenSlideAdapter(this)
        pager.adapter = adapter

        // Register a callback for the ViewPager
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            /**
             * Called when a ViewPager slide is selected
             * Sets a text view when last slide is selected
             * @param position Current position of a slide
             */
            override fun onPageSelected(position: Int) {
                when (position) {
                    3 -> {
                        view.skipBtn.text = getString(R.string.getStartedBtnString)
                    }
                }
                super.onPageSelected(position)
            }
        })

        // Sets a click listener that removes the fragment from the back stack
        view.skipBtn.setOnClickListener {
            fragmentManager?.popBackStackImmediate()
        }
    }
}
/* EOF */