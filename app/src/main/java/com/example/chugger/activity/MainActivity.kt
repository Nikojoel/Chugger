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

// Location request code
private const val LOCATION_REQUEST = 200

/**
 * @author Nikojoel
 * MainActivity
 * Responsible for controlling fragments, UI elements, bluetooth and NFC related
 * permissions and functionality
 */
class MainActivity : AppCompatActivity(), StopWatchFragment.StopWatchHelper,
    AlertFragment.AlertHelper, BleScanFragment.ScanFragmentHelper {

    /**
     * Companion object that has logic for,
     * - calculating angles
     * - converting radians to degrees
     * - converting and formatting milliseconds to seconds
     * - using resource strings outside of an activity
     */
    companion object {

        // Context variable
        lateinit var instance: MainActivity private set

        // Sensor offset values
        private const val xOffSet = 0.050
        private const val zOffSet = 1.050
        private const val zOffSetMax = 0.950
        private const val gravity = 9.81
        private const val zMax = -1.0F
        private const val zMaxP = 1

        /**
         * Calculates sensor angle
         * @param acc Current acceleration in (x.xxx) format, (1.0 g max)
         * @return Double
         */
        private fun calculateAngle(acc: Float): Double {
            /* Check for negative angles and calculate
            eq. if X axis is positive, Z axis is negative and vice versa
             */
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

        /**
         * Converts radians to degrees
         * @param rads Current radians
         * @return Double
         */
        private fun convertToDegree(rads: Double): Double {
            return kotlin.math.round(Math.toDegrees(rads))
        }

        /**
         * Convert milliseconds to seconds and format it in (xx:xx)
         * @param length Time string length
         * @param total Time in milliseconds
         * @return String
         */
        private fun convertToSeconds(length: Int, total: String): String {
            // Format in 1234 = 1:23 and 123456 = 12:35
            return when (length) {
                4 -> total.dropLast(1).replace("${total[0]}", "${total[0]}:")
                5 -> total.dropLast(1).replace("${total[1]}", "${total[1]}:")
                else -> "0"
            }
        }
    }

    /**
     * Object that allows string resources to be used from anywhere in the application
     */
    object Strings {
        /**
         * Get resource by calling getString from the activity context
         * @param stringRes Resource id
         * @param formatArgs Optional parameter for format
         * @return String
         */
        fun get(@StringRes stringRes: Int, vararg formatArgs: Any = emptyArray()): String {
            return instance.getString(stringRes, *formatArgs)
        }
    }

    // Lateinit variables
    private lateinit var btAdapter: BluetoothAdapter
    private lateinit var viewModel: BtViewModel
    private lateinit var btManager: BluetoothManager
    private lateinit var gatt: BluetoothGatt
    private lateinit var userDrinkTime: String
    private lateinit var device: BluetoothDevice
    private lateinit var sharedPref: SharedPreferences

    // Standard variables
    private var mainMenu: Menu? = null
    private var connected = false
    private var start = false
    private var firstTime = true
    private var negatives = false
    private var toast = true
    private var deviceAddress: String? = ""
    private var city: String? = ""

    /**
     * Called when the activity is created
     * @param savedInstanceState A mapping from String keys to various parcelable values
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        instance = this // Set activity context
        setSupportActionBar(findViewById(R.id.tool_bar)) // Set custom toolbar
        hasPermissions() // Check for location permissions
        askBtPermission() // Ask for bluetooth permissions

        // Plant custom debugger, Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Init bluetooth adapter, manager and view model
        viewModel = ViewModelProvider(this).get(BtViewModel::class.java)
        btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = btManager.adapter

        // Get device mac address from shared preferences
        sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        deviceAddress = sharedPref.getString(getString(R.string.macKey), 0.toString())

        // Check if shared preferences was empty
        if (deviceAddress == 0.toString()) {
            // Start pairing fragment if empty
            startBleFragment()
            connBtn.visibility = View.GONE
        }

        degreeText.visibility = View.GONE
        readyText.visibility = View.GONE

        // Set back stack listener for fragments
        listenBackStack()

        // Observe incoming data from sensor
        viewModel.data.observe(this) {
            // Start receiving data from sensor
            startRunning(it)
        }


        connBtn.setOnClickListener {
            when (connected) {
                // Destroy stopwatch if connected
                true -> {
                    destroyFragment()
                }
                // Connect device
                false -> {
                    connectDevice()
                    enableMenu(false)
                }
            }
        }
    }

    /**
     * Called when the back button is pressed,
     * disabled in some cases
     */
    override fun onBackPressed() {

        // Find fragments
        val bleFrag = supportFragmentManager.findFragmentByTag(getString(R.string.bleTag))
        val watchFrag = supportFragmentManager.findFragmentByTag(getString(R.string.watchTag))

        // Disable back button if ble scan is ongoing
        if (bleFrag != null && bleFrag.isVisible) {
            // Back button not enabled
        } else if (watchFrag != null && watchFrag.isVisible) {
            // Destroy stop watch if running
            destroyFragment()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Called when the application is resumed
     */
    override fun onResume() {
        super.onResume()
        // Enable main menu
        enableMenu(true)
    }

    /**
     * Geo codes a city from users latitude and longitude
     */
    @SuppressLint("MissingPermission")
    private fun getCity() {

        // Location client
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val geoCoder = Geocoder(this, Locale.getDefault())

        // Get last known location
        fusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
            if (task.isSuccessful && task.result != null) {
                // Geo code a city
                val geocoded =
                    geoCoder.getFromLocation(task.result!!.latitude, task.result!!.longitude, 1)
                city = geocoded[0].locality
            }
        }
    }

    /**
     * Reading sensor data via GATT connection,
     * controlling UI elements with received data
     * @param data Sensor data (X- & Y-axis acceleration values)
     */
    private fun startRunning(data: String) {
        Timber.d(data)

        // Inform user of successful connection
        if (toast) {
            degreeText.visibility = View.INVISIBLE
            showToast(getString(R.string.connectToastString, device.name), Toast.LENGTH_SHORT)
            toast = false
            readyText.visibility = View.VISIBLE
            beercanImg.visibility = View.VISIBLE
            beercanImg.setImageResource(R.drawable.beercan1)
        }

        // Format the incoming data from (xxxx,xxxx) to two variables in (x.xxx) format
        val accData = data.split(",")
        val accX = accData[0].toFloat() / 1000
        val accZ = accData[1].toFloat() / 1000

        // Start stop watch when off set values are exceeded
        if (accX > xOffSet && accZ < zOffSet && firstTime) {
            Timber.d("start")
            firstTime = false
            startStopWatchFragment()
            degreeText.visibility = View.VISIBLE
            connBtn.visibility = View.INVISIBLE
            readyText.visibility = View.GONE
            beercanImg.visibility = View.VISIBLE
        }

        // Calculate angles
        val xAngle = calculateAngle(accX)
        val zAngle = calculateAngle(accZ)

        Timber.d("angle from X ${convertToDegree(xAngle) + 7}}")
        Timber.d("angle from Z ${convertToDegree(zAngle)}")

        // Convert to degrees, add +7 to X-axis due to irregular values
        val xDeg = convertToDegree(xAngle) + 7
        val zDeg = convertToDegree(zAngle)

        // Start stopwatch when X-axis exceeds 15 degrees
        if (xDeg > 15) start = true

        // Start handling negative sensor values
        if (zDeg < 5) negatives = true

        // When handling negatives (tilt angle exceeds 90 degrees)
        if (negatives) {
            // Change beer can image view based off of sensor values
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

        // When not handling negatives (tilt angle below 90 degrees)
        if (!negatives) {
            // Change beer can image view based off of sensor values
            when (xDeg.toInt()) {
                in 15..34 -> beercanImg.setImageResource(R.drawable.beercan2)
                in 34..53 -> beercanImg.setImageResource(R.drawable.beercan3)
                in 53..71 -> beercanImg.setImageResource(R.drawable.beercan4)
                in 71..90 -> beercanImg.setImageResource(R.drawable.beercan5)
            }
        }
        // End stop watch when sensor is placed back on the table
        if (accX < xOffSet && accZ > zOffSetMax && start) {
            destroyFragment()
        }
    }

    /**
     * Disable or enable main menu buttons
     * @param onOff Enable or disable
     */
    private fun enableMenu(onOff: Boolean) {
        when (onOff) {
            // Enable
            true -> {
                mainMenu?.getItem(0)?.isEnabled = true
                mainMenu?.getItem(1)?.isEnabled = true
                mainMenu?.getItem(2)?.isEnabled = true
            }
            // Disable
            false -> {
                mainMenu?.getItem(0)?.isEnabled = false
                mainMenu?.getItem(1)?.isEnabled = false
                mainMenu?.getItem(2)?.isEnabled = false
            }
        }
    }

    /**
     * Set boolean variables,
     * used when stop watch is stopped
     */
    private fun setBooleans() {
        connected = false
        firstTime = true
        start = false
        toast = true
        negatives = false
    }

    /**
     * Clears fragment back stack,
     * closes gatt connection and
     * sets default booleans
     */
    fun destroyFragment() {
        supportFragmentManager.popBackStack()
        gatt.close()
        setBooleans()
        degreeText.visibility = View.GONE
        connBtn.visibility = View.VISIBLE
        readyText.visibility = View.GONE
        beercanImg.visibility = View.GONE
    }

    /**
     * Connects mobile device to the sensor,
     * informs user when connecting
     */
    private fun connectDevice() {

        // Check for bt adapter
        when (btAdapter.isEnabled) {

            // Ask permissions if not on
            false -> askBtPermission()

            // Connect device
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

    /**
     * Called when main menu is created
     * @param menu Main menu
     * @return Boolean
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mainMenu = menu
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * Adds a fragment back stack listener,
     * controls UI element visibility in MainActivity and
     * gets device mac address after ble scan fragment is destroyed
     */
    private fun listenBackStack() {
        supportFragmentManager.addOnBackStackChangedListener {

            // Get device mac address after ble scan
            if (supportFragmentManager.backStackEntryCount == 0 && deviceAddress == 0.toString()) {
                deviceAddress = sharedPref.getString(getString(R.string.macKey), 0.toString())
            }

            // If back stack is empty
            if (supportFragmentManager.backStackEntryCount == 0) {
                connBtn.visibility = View.VISIBLE
                connBtn.isEnabled = true
                enableMenu(true)

                // If fragments exist
            } else if (supportFragmentManager.backStackEntryCount > 0) {
                connBtn.visibility = View.GONE
                connBtn.isEnabled = false
                enableMenu(false)
            }
        }
    }

    /**
     * Creates BleScanFragment, passes a bt adapter reference to it and
     * starts it
     * @see BleScanFragment
     */
    private fun startBleFragment() {
        val bleFragment = BleScanFragment.newInstance(btAdapter)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_layout, bleFragment, getString(R.string.bleTag))
            .addToBackStack(null)
            .commit()
    }

    /**
     * Creates a view pager fragment and starts it
     * @see ScreenSlideFragment
     */
    private fun startSlideFragment() {
        val slideFragment = ScreenSlideFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_layout, slideFragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Creates a user adding fragment,
     * passes drinking time and city location as a parameter to it
     * and starts it
     * @see EditUserFragment
     */
    private fun startAddUserFragment() {
        val userAddFrag =
            EditUserFragment.newInstance(userDrinkTime, city, getString(R.string.cityText))
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_layout, userAddFrag)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Creates a stop watch fragment and starts it
     * @see StopWatchFragment
     */
    private fun startStopWatchFragment() {
        val stopWatchFrag = StopWatchFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_layout, stopWatchFrag, getString(R.string.watchTag))
            .addToBackStack(null)
            .commit()
    }

    /**
     * Creates a alert fragment,
     * passes drinking time as a parameter and starts it
     * @see AlertFragment
     */
    private fun startAlertFragment() {
        val alertFrag = AlertFragment.newInstance(userDrinkTime)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_layout, alertFrag)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Creates a new fragment that gets data from a database,
     * checks if fragment already exists and replaces or creates a new one
     * @see DbFragment
     */
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

    /**
     * Called when main menu item is selected
     * @param item Main menu item
     * @return Boolean
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_nfc -> showNfcActivity() // NfcActivity
            R.id.action_web -> openChrome() // Web IDE
            R.id.action_db -> startDbFragment() // Database fragment
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Creates an intent to open up Chrome as a separate application
     * and starts it
     * @throws Exception
     */
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

    /**
     * Check for location permissions
     * @return Boolean
     */
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
        getCity() // Geo code the current city
        return true
    }

    /**
     * Creates an intent to enable bluetooth adapter
     * and starts it
     */
    fun askBtPermission() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivity(enableBtIntent)
    }

    /**
     * Check if the mobile device has support for NFC
     * @return Boolean
     */
    private fun checkNfcSupport(): Boolean {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (!nfcAdapter.isEnabled) {
            Timber.d("NFC disabled")
            return false
        }
        return true
    }

    /**
     * Creates a toast with message, toast length
     * and shows it
     * @param msg Wanted message
     * @param length Wanted toast length
     */
    private fun showToast(msg: String, length: Int) {
        Toast.makeText(this, msg, length).show()
    }

    /**
     * Creates and shows an alert to inform the user that location
     * permissions are needed to use the applcation
     * @param permissions Array of manifest permissions
     */
    private fun showAlert(permissions: Array<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setIcon(R.drawable.ic_block)
        builder.setCancelable(false) // Alert can't be canceled

        builder.apply {
            setMessage(getString(R.string.locationString))
            setTitle(getString(R.string.permissionNeededString))
            setPositiveButton(getString(R.string.turnOnString)) { _, _ ->
                ActivityCompat.requestPermissions(this@MainActivity, permissions, LOCATION_REQUEST)
            }
        }.create().show()
    }

    /**
     * Creates and shows an alert to inform the user that
     * NFC is needed to use the NFC activity
     */
    private fun showNfcAlert() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage(getString(R.string.nfcPermissionString))
            setTitle(getString(R.string.nfcUnableString))
            setPositiveButton(getString(R.string.okString)) { _, _ ->
            }
        }.create().show()
    }

    /**
     * Create and start an intent to start the NFC activity
     */
    private fun showNfcActivity() {
        // Check for NFC support
        if (checkNfcSupport()) {
            val intent = Intent(this, NfcActivity::class.java)
            startActivity(intent)
        } else {
            showNfcAlert()
        }
    }

    /**
     * Called when a result from requesting permissions is complete
     * @param requestCode Request identifier
     * @param permissions Array of manifest permissions
     * @param grantResults Permission grant results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Custom location request code
        if (requestCode == 200) {

            // Permissions OK
            when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> {
                    connBtn.isEnabled = true
                    getCity()
                }
                // If location is disabled, show alert
                PackageManager.PERMISSION_DENIED -> {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showAlert(permissions)
                    }
                }
            }
        }
    }

    /**
     * Implemented function from StopWatchHelper
     * Get users drinking time
     * @param time Drinking time
     * @see StopWatchFragment
     */
    override fun getTime(time: Int) {
        userDrinkTime = convertToSeconds(time.toString().length, time.toString())
        Timber.d("time was $time in milliseconds, size ${time.toString().length}")
        // Start alert fragment
        startAlertFragment()
    }

    /**
     * Implemented function from AlertHelper
     * Used to start a fragment within a fragment
     * @see startAddUserFragment
     * @see EditUserFragment
     */
    override fun startDbFrag() {
        startAddUserFragment()
    }

    /**
     * Implemented function from ScanFragmentHelper
     * Used to start a fragment within a fragment
     * @see startSlideFragment
     * @see ScreenSlideFragment
     */
    override fun startSlide() {
        startSlideFragment()
    }
}
/* EOF */