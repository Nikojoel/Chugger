package com.example.chugger.activity

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
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
        private const val url = "https://www.espruino.com/ide/"

        private const val xOffSet = 0.050
        private const val zOffSet = 1.050
        private const val zOffSetMax = 1.0

        private const val gravity = 9.81
        private const val zMax = -1.0F
        private const val zMaxP = 1

        fun calculateAngle(acc: Float) : Double {
            return if (acc < zMax) {
               kotlin.math.asin((gravity * (zMax * -1) / gravity))
           } else if (acc == zMax && acc < 0) {
                kotlin.math.asin((gravity * (acc * -1) / gravity))
           } else if (acc > zMaxP) {
                kotlin.math.asin((gravity * 1 / gravity))
            } else if (acc > zMax && acc < 0) {
                kotlin.math.asin((gravity * (acc * -1) / gravity))
            } else {
                kotlin.math.asin((gravity * acc / gravity))
            }
        }

        fun convertToDegree(rads: Double) : Double {
            return kotlin.math.round(Math.toDegrees(rads))
        }
    }

    private lateinit var btAdapter: BluetoothAdapter
    private lateinit var viewModel: BtViewModel
    private lateinit var device: BluetoothDevice
    private lateinit var btManager: BluetoothManager
    private lateinit var gatt: BluetoothGatt
    private lateinit var mainMenu: Menu
    private lateinit var nfcActivity: NfcActivity
    private lateinit var stopWatchfrag: StopWatchFragment

    private var connected = false
    private var start = false
    private var firstTime = true
    private var userDrinkTime: Long = 0
    private var negatives = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcActivity = NfcActivity()

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
        teksti.visibility = View.INVISIBLE
        viewModel.data.observe(this) {
            Timber.d(it)
            val accData = it.split(",")
            val accX = accData[0].toFloat() / 1000
            val accZ = accData[1].toFloat() / 1000
            if (accX > xOffSet && accZ < zOffSet && firstTime) {
                Timber.d("start")
                firstTime = false
                startStopWatchFragment()
                teksti.visibility = View.VISIBLE
            }
            val xAngle = calculateAngle(accX)
            val zAngle = calculateAngle(accZ)
            Timber.d("angle from X ${convertToDegree(xAngle) + 7}}")
            Timber.d("angle from Z ${convertToDegree(zAngle)}")
            val xDeg = convertToDegree(xAngle) + 7
            val zDeg = convertToDegree(zAngle)
            if (xDeg > 15) start = true
            if (zDeg < 5) negatives = true
            teksti.text = if (negatives) "${90 + zDeg.toInt()} degrees" else "${xDeg.toInt()} degrees"

            // End timer when sensor is placed back on the table
            if (accX < xOffSet && accZ > zOffSetMax && start) {
                destroyFragment()
            }
        }

        connBtn.setOnClickListener {
            connectDevice()
            mainMenu.getItem(0).isEnabled = false
            mainMenu.getItem(1).isEnabled = false
        }
    }

    override fun getTime(time: Long) {
        userDrinkTime = time
        Timber.d("time was $time in milliseconds")
    }

    private fun destroyFragment() {
        supportFragmentManager.popBackStack()
        mainMenu.getItem(0).isEnabled = true
        mainMenu.getItem(1).isEnabled = true
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mainMenu = menu!!
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun startStopWatchFragment() {
        stopWatchfrag = StopWatchFragment.newInstance(gatt)
        supportFragmentManager
            .beginTransaction()
            .add(R.id.main_layout, stopWatchfrag)
            .addToBackStack(null)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_nfc -> showNfcActivity()
            R.id.action_web -> openChrome()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openChrome() {
        try {
            val uri = Uri.parse("googlechrome://navigate?url=$url")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.d("exception in chrome $e")
            // Chrome is probably not installed
        }
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
            connBtn.isEnabled = false
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

    private fun showNfcActivity() {
        if (checkNfcSupport()) {
            val intent = Intent(this, NfcActivity::class.java)
            startActivity(intent)
        } else {
            showNfcAlert()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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