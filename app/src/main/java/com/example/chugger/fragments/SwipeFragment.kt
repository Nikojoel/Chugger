package com.example.chugger.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.chugger.R
import com.example.chugger.models.FragmentData
import com.example.chugger.models.SlideHelper
import kotlinx.android.synthetic.main.fragment_swipe.view.*

/**
 * @author Nikojoel
 * SwipeFragment
 * Fragment that holds a single ViewPager page
 * and inflates it with the right data
 */
class SwipeFragment : Fragment() {
    companion object {

        // Variable used to save the right data
        private lateinit var data: FragmentData

        /**
         * Returns a new instance of SwipeFragment
         * with the right data
         * @param index Used to determine which data needs to be displayed
         * @return SwipeFragment
         */
        fun newInstance(index: Int): SwipeFragment {

            // Set data based on index, 4 available pages
            when (index) {
                0 -> this.data = SlideHelper.slide0
                1 -> this.data = SlideHelper.slide1
                2 -> this.data = SlideHelper.slide2
                3 -> this.data = SlideHelper.slide3
            }
            return SwipeFragment()
        }
    }

    /**
     * Inflates a layout and returns it
     * @param inflater Used to inflate the layout
     * @param container Used to determine where to inflate the layout
     * @param savedInstanceState A mapping from String keys to various parcelable values.
     * @return View?
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_swipe, container, false)

        // Set correct slide data
        view.title.text = data.title
        view.desc.text = data.desc
        view.image.setImageResource(data.imgResId)

        return view
    }
}
/* EOF */