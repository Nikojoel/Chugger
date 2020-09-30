package com.example.chugger.fragments

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.*
import com.example.chugger.R
import kotlinx.android.synthetic.main.fragment_ble_scan.*
import kotlinx.android.synthetic.main.fragment_ble_scan.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception

class BleScanFragment : Fragment() {

    private val uuid = ParcelUuid.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private lateinit var bleScanner: BluetoothLeScanner
    private lateinit var callBack: BtLeScanCallback
    private lateinit var viewModel: BtLeViewModel
    private lateinit var macAddress: String
    private lateinit var deviceName: String
    private lateinit var helper: ScanFragmentHelper

    companion object {
        private lateinit var btAdapter: BluetoothAdapter
        fun newInstance(btAdapter: BluetoothAdapter): BleScanFragment {
            this.btAdapter = btAdapter
            return BleScanFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = BtLeViewModel()
        helper = context as ScanFragmentHelper

        viewModel.data.observe(this) {
            Log.d("DBG", it.device.name.toString())
            Log.d("DBG", it.rssi.toString())
            Log.d("DBG", it.device.toString())

            nameText.text = it.device.name.toString()
            deviceName = it.device.name.toString()
            macAddress = it.device.toString()


            if (it.rssi.toString() > getString(R.string.rssiOffSetHigh)) {
                signalImg.setImageResource(R.drawable.ic_signal3)
            }
            else if (it.rssi.toString() > getString(R.string.rssiOffSetLow)) {
                signalImg.setImageResource(R.drawable.ic_signal2)
            }
            else {
                signalImg.setImageResource(R.drawable.ic_signal1)
            }

            progBar.visibility = View.GONE
            scanText.visibility = View.GONE
            pairBtn.visibility = View.VISIBLE
            newScanText.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.fragment_ble_scan, container, false)
        view.scanBtn.setOnClickListener {
            startScan()
            introText.visibility = View.GONE
        }

        view.pairBtn.setOnClickListener {
            saveToPrefs()
        }
        return view
    }

    private fun saveToPrefs() {
        try {
            GlobalScope.launch(Dispatchers.IO) {
                val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return@launch
                with (sharedPref.edit()) {
                    putString(getString(R.string.macKey), macAddress)
                    apply()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(activity, getString(R.string.pairExceptionText), Toast.LENGTH_SHORT).show()
        }
        Toast.makeText(activity, getString(R.string.pairToastText, deviceName), Toast.LENGTH_SHORT).show()
        fragmentManager?.popBackStackImmediate()
        helper.startSlide()
    }

    private fun startScan() {
        progBar.visibility = View.VISIBLE
        scanText.visibility = View.VISIBLE

        callBack = BtLeScanCallback()
        bleScanner = btAdapter.bluetoothLeScanner

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        val filter = ScanFilter.Builder()
            .setServiceUuid(uuid)
            .build()

        val filters: List<ScanFilter>? = listOf(filter)
        bleScanner.startScan(filters, settings, callBack)
    }

    private inner class BtLeScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addScanResult(result)
            Log.d("DBG","Added result $result")
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("DBG", "BLE Scan Failed with code $errorCode")
        }

        private fun addScanResult(result: ScanResult) {
            bleScanner.stopScan(callBack)
            viewModel.changeValue(result)
        }
    }

    interface ScanFragmentHelper {
        fun startSlide()
    }
}

class BtLeViewModel: ViewModel() {
    private val result = MutableLiveData<ScanResult>()

    fun changeValue(value: ScanResult) {
        result.postValue(value)
    }

    val data = result.switchMap {
        liveData(Dispatchers.Main) {
            emit(it)
        }
    }
}

