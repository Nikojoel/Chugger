package com.example.chugger.fragments

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Context
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.*
import com.example.chugger.R
import kotlinx.android.synthetic.main.fragment_ble_scan.*
import kotlinx.android.synthetic.main.fragment_ble_scan.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.Exception

/**
 * @author Nikojoel
 * BleScanFragment
 * Fragment that has functionality to scan for ble devices
 */
class BleScanFragment : Fragment() {

    // Custom uuid that is found from every Ruuvi with the correct firmware
    private val uuid = ParcelUuid.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")

    // Lateinit variables
    private lateinit var bleScanner: BluetoothLeScanner
    private lateinit var callBack: BtLeScanCallback
    private lateinit var viewModel: BtLeViewModel
    private lateinit var macAddress: String
    private lateinit var deviceName: String
    private lateinit var helper: ScanFragmentHelper

    companion object {
        private lateinit var btAdapter: BluetoothAdapter

        /**
         * Creates a new instance of BleScanFragment
         * @param btAdapter Bluetooth adapter of the device
         * @return BleScanFragment
         */
        fun newInstance(btAdapter: BluetoothAdapter): BleScanFragment {
            // Set bluetooth adapter
            this.btAdapter = btAdapter
            return BleScanFragment()
        }
    }

    /**
     * Called when the fragment is created
     * @param savedInstanceState A mapping from String keys to various parcelable values
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create new instance of BtLeViewModel
        viewModel = BtLeViewModel()

        // Initialize the helper interface
        helper = context as ScanFragmentHelper

        // Observe data from the view model
        viewModel.data.observe(this) {

            // Set views from the observed data
            nameText.text = it.device.name.toString()
            deviceName = it.device.name.toString()
            macAddress = it.device.toString()

            // Set a signal strength icon
            if (it.rssi.toString() > getString(R.string.rssiOffSetHigh)) {
                signalImg.setImageResource(R.drawable.ic_signal3)
            }
            else if (it.rssi.toString() > getString(R.string.rssiOffSetLow)) {
                signalImg.setImageResource(R.drawable.ic_signal2)
            }
            else {
                signalImg.setImageResource(R.drawable.ic_signal1)
            }

            // Set view visibilities
            progBar.visibility = View.GONE
            scanText.visibility = View.GONE
            pairBtn.visibility = View.VISIBLE
            newScanText.visibility = View.VISIBLE
        }
    }
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
        val view = inflater.inflate(R.layout.fragment_ble_scan, container, false)

        // Register a click listener
        view.scanBtn.setOnClickListener {
            // Start the ble scan
            startScan()
            introText.visibility = View.GONE
        }

        // Register a click listener
        view.pairBtn.setOnClickListener {
            // Save Ruuvi mac address to the shared preferences
            saveToPrefs()
        }
        return view
    }

    /**
     * Launches a coroutine in IO scope that saves
     * the Ruuvi mac address to saved preferences
     * @throws Exception When there is a problem in the shared preferences
     */
    private fun saveToPrefs() {
        try {
            // Launch coroutine
            GlobalScope.launch(Dispatchers.IO) {
                // Open shared preferences for editing
                val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return@launch
                with (sharedPref.edit()) {
                    // Save the mac address
                    putString(getString(R.string.macKey), macAddress)
                    apply()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(activity, getString(R.string.pairExceptionText), Toast.LENGTH_SHORT).show()
        }
        // Make a toast and destroy the BleScanFragment
        Toast.makeText(activity, getString(R.string.pairToastText, deviceName), Toast.LENGTH_SHORT).show()
        fragmentManager?.popBackStackImmediate()

        // Start the ScreenSlideFragment
        helper.startSlide()
    }

    /**
     * Starts the ble scan with a Ruuvi specific filter, settings
     * and a callback that is called when a result is found
     */
    private fun startScan() {
        // Set view visibilities
        progBar.visibility = View.VISIBLE
        scanText.visibility = View.VISIBLE

        // Create new instance of BtLeScanCallBack
        callBack = BtLeScanCallback()

        // Initialize the ble scanner
        bleScanner = btAdapter.bluetoothLeScanner

        // Create scan settings
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        // Create scan filter
        val filter = ScanFilter.Builder()
            .setServiceUuid(uuid)
            .build()
        val filters: List<ScanFilter>? = listOf(filter)

        // Start the ble scan
        bleScanner.startScan(filters, settings, callBack)
    }

    /**
     * @author Nikojoel
     * BtLeScanCallBack
     * Inner class that has the scan callback functionality
     */
    private inner class BtLeScanCallback : ScanCallback() {
        /**
         * Called when a scan result is found
         * @param callbackType Result callback type
         * @param result The found result
         */
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addScanResult(result)
            Timber.d("Added result: $result")
        }

        /**
         * Called when the scan fails
         * @param errorCode Tells which error occurred
         */
        override fun onScanFailed(errorCode: Int) {
            Timber.d("BLE Scan Failed with code $errorCode")
        }

        /**
         * Changes the view model data that is being observed
         * and stops the ble scan
         * @param result Found scan result
         */
        private fun addScanResult(result: ScanResult) {
            bleScanner.stopScan(callBack)
            viewModel.changeValue(result)
        }
    }

    /**
     * Interface used in MainActivity to start the
     * ScreenSlideFragment
     */
    interface ScanFragmentHelper {
        /**
         * Implemented in MainActivity
         */
        fun startSlide()
    }
}

/**
 * @author Nikojoel
 * BtLeViewModel
 * View model class to observe the scan results
 */
class BtLeViewModel: ViewModel() {

    // Live data variable
    private val result = MutableLiveData<ScanResult>()

    /**
     * Changes the live data that is being observed
     * @param value Found scan result
     */
    fun changeValue(value: ScanResult) {
        result.postValue(value)
    }

    // Emit the found data via live data
    val data = result.switchMap {
        liveData(Dispatchers.Main) {
            emit(it)
        }
    }
}
/* EOF */

