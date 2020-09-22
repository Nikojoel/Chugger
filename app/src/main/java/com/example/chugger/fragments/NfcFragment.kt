package com.example.chugger.fragments

import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.chugger.R
import timber.log.Timber


class NfcFragment : Fragment() {

    companion object {
        fun newInstance(): NfcFragment {
            return NfcFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_nfc, container, false)
    }
}