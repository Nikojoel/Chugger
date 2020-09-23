package com.example.chugger.fragments

import android.bluetooth.BluetoothGatt
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.chugger.R
import kotlinx.android.synthetic.main.fragment_web.*

class WebFragment : Fragment() {

    companion object {
        private const val url = "https://www.espruino.com/ide/"
        fun newInstance(): WebFragment {
            return WebFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_web, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        webIde.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String) : Boolean {
                view!!.loadUrl(url)
                return true
            }
        }
        webIde.settings.builtInZoomControls = true
        webIde.settings.displayZoomControls = false
        webIde.settings.javaScriptEnabled = true
        webIde.loadUrl(url)
    }
}