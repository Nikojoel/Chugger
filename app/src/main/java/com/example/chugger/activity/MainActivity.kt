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
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.example.chugger.BuildConfig
import com.example.chugger.R
import com.example.chugger.bluetooth.BtViewModel
import com.example.chugger.bluetooth.GattCallBack
import com.example.chugger.fragments.NfcFragment
import com.example.chugger.fragments.StopWatchFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_nfc.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_REQUEST = 200
        private const val LOCATION_STRING = "Location"
        private const val DEVICE_ADDRESS = "D3:E0:2A:CB:0C:FE"
    }

    private lateinit var btAdapter: BluetoothAdapter
    private lateinit var viewModel: BtViewModel
    private lateinit var device: BluetoothDevice
    private lateinit var btManager: BluetoothManager
    private lateinit var gatt: BluetoothGatt
    private lateinit var mainMenu: Menu
    private var connected = false

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
        }

        connBtn.setOnClickListener {
            getStopWatchFragment()
        }
    }

    private fun connectDevice() {
        if (!btAdapter.isEnabled) {
            askBtPermission()
        } else {
            gatt = device.connectGatt(
                this,
                false,
                GattCallBack(viewModel),
                BluetoothDevice.TRANSPORT_LE
            )
            connected = true
        }
    }

    private fun disconnectDevice() {
        gatt.disconnect()
        connected = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mainMenu = menu!!
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun getStopWatchFragment() {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.main_layout, StopWatchFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_bluetooth -> {
                if (!connected) {
                    connectDevice()
                    mainMenu.getItem(0).icon = ContextCompat.getDrawable(this, R.drawable.ic_bt_off)
                } else {
                    disconnectDevice()
                    mainMenu.getItem(0).icon = ContextCompat.getDrawable(this, R.drawable.ic_bt)
                }
            }
              R.id.action_nfc -> showNfcFragment()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun hasPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                200
            )
            return false
        }
        connBtn.isEnabled = true
        return true
    }

    private fun askBtPermission() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivity(enableBtIntent)
    }

    private fun checkNfcSupport(): Boolean {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (!nfcAdapter.isEnabled) {
            Timber.d("NFC disabled")
        return false
        }
        return true
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

    private fun showNfcAlert() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage("Nfc is needed to use this app")
            setTitle("Unable to use NFC")
            setPositiveButton("OK") { _, _ ->
            }
        }.create().show()
    }

    private fun showNfcFragment() {
        if (checkNfcSupport()) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.main_layout, NfcFragment.newInstance())
                .addToBackStack(null)
                .commit()
        } else {
            showNfcAlert()
        }
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