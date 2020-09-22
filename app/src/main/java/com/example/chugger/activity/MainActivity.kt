package com.example.chugger.activity

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.chugger.BuildConfig
import com.example.chugger.R
import com.example.chugger.bluetooth.BtViewModel
import com.example.chugger.bluetooth.GattCallBack
import com.example.chugger.fragments.StopWatchFragment
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity(), StopWatchFragment.StopWatchHelper {

    companion object {
        private const val LOCATION_REQUEST = 200
        private const val LOCATION_STRING = "Location"
        private const val DEVICE_ADDRESS = "D3:E0:2A:CB:0C:FE"

        private const val xOffSet = 50
        private const val zOffSet = 1050
    }

    private lateinit var btAdapter: BluetoothAdapter
    private lateinit var viewModel: BtViewModel
    private lateinit var device: BluetoothDevice
    private lateinit var btManager: BluetoothManager
    private lateinit var gatt: BluetoothGatt
    private lateinit var mainMenu: Menu
    private lateinit var frag: StopWatchFragment

    private var connected = false
    private var start = false
    private var firstTime = true
    private var negatives = false
    private var userDrinkTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.tool_bar))
        supportActionBar?.setTitle(R.string.app_name)

        hasPermissions()
        askBtPermission()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        viewModel = ViewModelProvider(this).get(BtViewModel::class.java)
        btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        btAdapter = btManager.adapter
        device = btAdapter.getRemoteDevice(DEVICE_ADDRESS)

        viewModel.data.observe(this) {
            Timber.d(it)
            val accData = it.split(",")
            val accX = accData[0].toInt()
            val accZ = accData[1].toInt()
            if (accX > xOffSet && accZ < zOffSet && firstTime) {
                Timber.d("start")
                firstTime = false
                startStopWatchFragment()
            }
            if (accX > 500 && accZ < 800 && accX < 750 && !negatives) {
                Timber.d("1")
                teksti.text = "1"
                start = true
            } else if (accX > 750 && accZ < 650 && accX < 950 && !negatives) {
                Timber.d("2")
                teksti.text = "2"
            } else if (accX > 950 && accZ < 500 && !negatives) {
                Timber.d("3")
                teksti.text = "3"
                negatives = true

                // End timer when sensor is placed back on the table
            } else if (accX < xOffSet && accZ > 1000 && start) {
                destroyFragment()
            }
        }

        connBtn.setOnClickListener {
            connectDevice()
        }
    }

    override fun getTime(time: Long) {
        userDrinkTime = time
        Timber.d("time was $time in milliseconds")
    }

    private fun destroyFragment() {
        supportFragmentManager.fragments[0].onDestroy()
    }

    private fun connectDevice() {
        if (!btAdapter.isEnabled) {
            askBtPermission()
        } else {
            gatt = device.connectGatt(this, false, GattCallBack(viewModel), BluetoothDevice.TRANSPORT_LE)
            connected = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mainMenu = menu!!
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun startStopWatchFragment() {
        frag = StopWatchFragment.newInstance(gatt)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_layout, frag)
            .addToBackStack(null)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_nfc -> Timber.d("NFC")
        }
        return super.onOptionsItemSelected(item)
    }

    private fun hasPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200)
            return false
        }
        connBtn.isEnabled = true
        return true
    }

    private fun askBtPermission() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivity(enableBtIntent)
    }

    private fun showAlert(permissions: Array<String>) {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage("$LOCATION_STRING is needed to use this app")
            setTitle("Permission needed")
            setPositiveButton("Turn on") { _, _ ->
                ActivityCompat.requestPermissions(this@MainActivity, permissions, LOCATION_REQUEST)
            }
        }.create().show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> connBtn.isEnabled = true
                PackageManager.PERMISSION_DENIED -> {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showAlert(permissions)
                    }
                }
            }
        }
    }
}