package com.example.chugger.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.example.chugger.BuildConfig
import com.example.chugger.R
import com.example.chugger.bluetooth.BtViewModel
import com.example.chugger.bluetooth.GattCallBack
import com.example.chugger.fragments.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.util.*

private const val LOCATION_REQUEST = 200

class MainActivity : AppCompatActivity(), StopWatchFragment.StopWatchHelper,
    AlertFragment.AlertHelper, BleScanFragment.ScanFragmentHelper {

    companion object {
        lateinit var instance: MainActivity private set

        private const val xOffSet = 0.050
        private const val zOffSet = 1.050
        private const val zOffSetMax = 0.950

        private const val gravity = 9.81
        private const val zMax = -1.0F
        private const val zMaxP = 1

        private fun calculateAngle(acc: Float): Double {
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

        private fun convertToDegree(rads: Double): Double {
            return kotlin.math.round(Math.toDegrees(rads))
        }

        private fun convertToSeconds(length: Int, total: String): String {
            return when (length) {
                4 -> total.dropLast(1).replace("${total[0]}", "${total[0]}:")
                5 -> total.dropLast(1).replace("${total[1]}", "${total[1]}:")
                else -> "0"
            }
        }
    }

    object Strings {
        fun get(@StringRes stringRes: Int, vararg formatArgs: Any = emptyArray()): String {
            return instance.getString(stringRes, *formatArgs)
        }
    }

    private lateinit var btAdapter: BluetoothAdapter
    private lateinit var viewModel: BtViewModel
    private lateinit var btManager: BluetoothManager
    private lateinit var gatt: BluetoothGatt
    private lateinit var slideFragment: ScreenSlideFragment
    private lateinit var stopWatchFrag: StopWatchFragment
    private lateinit var userAddFrag: EditUserFragment
    private lateinit var userDrinkTime: String
    private lateinit var alertFrag: AlertFragment
    private lateinit var device: BluetoothDevice
    private lateinit var sharedPref: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geoCoder: Geocoder

    private var mainMenu: Menu? = null
    private var connected = false
    private var start = false
    private var firstTime = true
    private var negatives = false
    private var toast = true
    private var deviceAddress: String? = ""
    private var city: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        instance = this
        setSupportActionBar(findViewById(R.id.tool_bar))
        hasPermissions()
        askBtPermission()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        viewModel = ViewModelProvider(this).get(BtViewModel::class.java)
        btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        btAdapter = btManager.adapter

        sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        deviceAddress = sharedPref.getString(getString(R.string.macKey), 0.toString())


        if (deviceAddress == 0.toString()) {
            startBleFragment()
            connBtn.visibility = View.GONE
        }

        degreeText.visibility = View.GONE
        readyText.visibility = View.GONE

        listenBackStack()

        viewModel.data.observe(this) {
            startRunning(it)
        }

        connBtn.setOnClickListener {
            when (connected) {
                true -> {
                    destroyFragment()
                }
                false -> {
                    connectDevice()
                    enableMenu(false)
                }
            }
        }
    }

    override fun onBackPressed() {
        val bleFrag = supportFragmentManager.findFragmentByTag(getString(R.string.bleTag))
        val watchFrag = supportFragmentManager.findFragmentByTag(getString(R.string.watchTag))
        if (bleFrag != null && bleFrag.isVisible) {
            // Back button not enabled
        } else if (watchFrag != null && watchFrag.isVisible) {
            destroyFragment()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        enableMenu(true)
    }

    @SuppressLint("MissingPermission")
    private fun getCity() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geoCoder = Geocoder(this, Locale.getDefault())
        fusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
            if (task.isSuccessful && task.result != null) {
                val geocoded =
                    geoCoder.getFromLocation(task.result!!.latitude, task.result!!.longitude, 1)
                city = geocoded[0].locality
            }
        }
    }

    private fun startRunning(data: String) {
        Timber.d(data)
        if (toast) {
            degreeText.visibility = View.INVISIBLE
            showToast(getString(R.string.connectToastString, device.name), Toast.LENGTH_SHORT)
            toast = false
            readyText.visibility = View.VISIBLE
            beercanImg.setImageResource(R.drawable.beercan1)
        }
        val accData = data.split(",")
        val accX = accData[0].toFloat() / 1000
        val accZ = accData[1].toFloat() / 1000

        // Start timer when off set values are exceeded
        if (accX > xOffSet && accZ < zOffSet && firstTime) {
            Timber.d("start")
            firstTime = false
            startStopWatchFragment()
            degreeText.visibility = View.VISIBLE
            connBtn.visibility = View.INVISIBLE
            readyText.visibility = View.GONE
            beercanImg.visibility = View.VISIBLE
        }
        val xAngle = calculateAngle(accX)
        val zAngle = calculateAngle(accZ)
        Timber.d("angle from X ${convertToDegree(xAngle) + 7}}")
        Timber.d("angle from Z ${convertToDegree(zAngle)}")
        val xDeg = convertToDegree(xAngle) + 7
        val zDeg = convertToDegree(zAngle)
        if (xDeg > 15) start = true
        if (zDeg < 5) negatives = true

        if (negatives) {
            when (90 + zDeg.toInt()) {
                in 90..113 -> beercanImg.setImageResource(R.drawable.beercan6)
                in 113..125 -> beercanImg.setImageResource(R.drawable.beercan7)
                in 136..159 -> beercanImg.setImageResource(R.drawable.beercan8)
                in 159..180 -> beercanImg.setImageResource(R.drawable.beercan9)
            }
            degreeText.text = getString(R.string.degreesTextString, 90 + zDeg.toInt())
        } else {
            degreeText.text = getString(R.string.degreesTextString, xDeg.toInt())
        }
        if (!negatives) {
            when (xDeg.toInt()) {
                in 15..34 -> beercanImg.setImageResource(R.drawable.beercan2)
                in 34..53 -> beercanImg.setImageResource(R.drawable.beercan3)
                in 53..71 -> beercanImg.setImageResource(R.drawable.beercan4)
                in 71..90 -> beercanImg.setImageResource(R.drawable.beercan5)
            }
        }
        // End timer when sensor is placed back on the table
        if (accX < xOffSet && accZ > zOffSetMax && start) {
            destroyFragment()
        }
    }

    private fun enableMenu(onOff: Boolean) {
        when (onOff) {
            true -> {
                mainMenu?.getItem(0)?.isEnabled = true
                mainMenu?.getItem(1)?.isEnabled = true
                mainMenu?.getItem(2)?.isEnabled = true
            }
            false -> {
                mainMenu?.getItem(0)?.isEnabled = false
                mainMenu?.getItem(1)?.isEnabled = false
                mainMenu?.getItem(2)?.isEnabled = false
            }
        }
    }

    private fun setBooleans() {
        connected = false
        firstTime = true
        start = false
        toast = true
        negatives = false
    }

    override fun getTime(time: Int) {
        userDrinkTime = convertToSeconds(time.toString().length, time.toString())
        Timber.d("time was $time in milliseconds, size ${time.toString().length}")
        startAlertFragment()
    }

    fun destroyFragment() {
        supportFragmentManager.popBackStack()
        gatt.close()
        setBooleans()
        degreeText.visibility = View.GONE
        connBtn.visibility = View.VISIBLE
        readyText.visibility = View.GONE
        beercanImg.visibility = View.GONE
    }

    private fun connectDevice() {
        when (btAdapter.isEnabled) {
            false -> askBtPermission()
            true -> {
                device = btAdapter.getRemoteDevice(deviceAddress)
                showToast(
                    getString(R.string.connectingToastString, device.name),
                    Toast.LENGTH_SHORT
                )
                gatt = device.connectGatt(
                    this,
                    false,
                    GattCallBack(viewModel),
                    BluetoothDevice.TRANSPORT_LE
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mainMenu = menu
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun listenBackStack() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0 && deviceAddress == 0.toString()) {
                deviceAddress = sharedPref.getString(getString(R.string.macKey), 0.toString())
            }
            if (supportFragmentManager.backStackEntryCount == 0) {
                connBtn.visibility = View.VISIBLE
                connBtn.isEnabled = true
                enableMenu(true)
            } else if (supportFragmentManager.backStackEntryCount > 0) {
                connBtn.visibility = View.GONE
                connBtn.isEnabled = false
                enableMenu(false)
            }
        }
    }

    private fun startBleFragment() {
        val bleFragment = BleScanFragment.newInstance(btAdapter)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_layout, bleFragment, getString(R.string.bleTag))
            .addToBackStack(null)
            .commit()
    }

    private fun startSlideFragment() {
        slideFragment = ScreenSlideFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_layout, slideFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun startAddUserFragment() {
        userAddFrag =
            EditUserFragment.newInstance(userDrinkTime, city, getString(R.string.cityText))
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_layout, userAddFrag)
            .addToBackStack(null)
            .commit()
    }

    private fun startStopWatchFragment() {
        stopWatchFrag = StopWatchFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_layout, stopWatchFrag, getString(R.string.watchTag))
            .addToBackStack(null)
            .commit()
    }

    private fun startAlertFragment() {
        alertFrag = AlertFragment.newInstance(userDrinkTime)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_layout, alertFrag)
            .addToBackStack(null)
            .commit()
    }

    private fun startDbFragment() {
        val frag = supportFragmentManager.findFragmentByTag(getString(R.string.dbTag))
        val manager = supportFragmentManager.beginTransaction()
        if (frag != null && frag.isVisible) {
            manager.replace(R.id.main_layout, frag, getString(R.string.dbTag)).commit()
        } else {
            manager
                .replace(R.id.main_layout, DbFragment.newInstance(), getString(R.string.dbTag))
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_nfc -> showNfcActivity()
            R.id.action_web -> openChrome()
            R.id.action_db -> startDbFragment()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openChrome() {
        try {
            val uri = Uri.parse(getString(R.string.chromeUrlString))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.d("exception in chrome $e")
            showToast(getString(R.string.chromeErrorString), Toast.LENGTH_SHORT)
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
        getCity()
        return true
    }

    fun askBtPermission() {
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

    private fun showToast(msg: String, length: Int) {
        Toast.makeText(this, msg, length).show()
    }


    private fun showAlert(permissions: Array<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setIcon(R.drawable.ic_block)
        builder.setCancelable(false)
        builder.apply {
            setMessage(getString(R.string.locationString))
            setTitle(getString(R.string.permissionNeededString))
            setPositiveButton(getString(R.string.turnOnString)) { _, _ ->
                ActivityCompat.requestPermissions(this@MainActivity, permissions, LOCATION_REQUEST)
            }
        }.create().show()
    }

    private fun showNfcAlert() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage(getString(R.string.nfcPermissionString))
            setTitle(getString(R.string.nfcUnableString))
            setPositiveButton(getString(R.string.okString)) { _, _ ->
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
                PackageManager.PERMISSION_GRANTED -> {
                    connBtn.isEnabled = true
                    getCity()
                }
                PackageManager.PERMISSION_DENIED -> {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showAlert(permissions)
                    }
                }
            }
        }
    }

    override fun startDbFrag() {
        startAddUserFragment()
    }

    override fun startSlide() {
        startSlideFragment()
    }
}