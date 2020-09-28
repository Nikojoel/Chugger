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
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.*
import com.example.chugger.R
import kotlinx.android.synthetic.main.fragment_ble_scan.*
import kotlinx.android.synthetic.main.fragment_ble_scan.view.*
import kotlinx.coroutines.Dispatchers

class BleScanFragment : Fragment() {

    private val uuid = ParcelUuid.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private lateinit var bleScanner: BluetoothLeScanner
    private lateinit var callBack: BtLeScanCallback
    private lateinit var viewModel: BtLeViewModel

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

        viewModel.data.observe(this) {
            Log.d("DBG", it.device.name.toString())
            Log.d("DBG", it.rssi.toString())

            nameText.text = it.device.name.toString()

            if(it.rssi.toString() > (-86).toString()) {
                signalImg.setImageResource(R.drawable.ic_signal_low)
            }
            else if(it.rssi.toString() > (-70).toString()) {
                signalImg.setImageResource(R.drawable.ic_signal_half)
            }
            else {
                signalImg.setImageResource(R.drawable.ic_signal_full)
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

        view.nameText.setOnClickListener {
            // TODO Save to prefs
        }

        view.pairBtn.setOnClickListener {
            // TODO Pair Ruuvi
        }
        return view
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

