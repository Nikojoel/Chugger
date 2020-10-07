package com.example.chugger.bluetooth

import android.bluetooth.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.util.*

/**
 * @author Nikojoel
 * GattCallBack
 * Class for bluetooth GATT call back
 * @param viewModel Custom view model
 */
class GattCallBack(private val viewModel: BtViewModel): BluetoothGattCallback() {

    // UUID's for service and descriptor
    private val service = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val rxDescriptor = convertFromInteger(0x2902)

    /**
     * Converts an integer to UUID
     * @param i Convertible integer
     * @return UUID
     */
    private fun convertFromInteger(i: Int): UUID {
        val MSB = 0x0000000000001000L
        val LSB = -0x7fffff7fa064cb05L
        val value = (i and -0x1).toLong()
        return UUID(MSB or (value shl 32), LSB)
    }

    /**
     * Called when connection state is changed
     * @param gatt Bluetooth gatt
     * @param status Previous connection state
     * @param newState New connection state
     */
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)

        // If statements of status codes
        if (status == BluetoothGatt.GATT_FAILURE) {
            Timber.d("GATT connection failure")
            return

        } else if (status != BluetoothGatt.GATT_SUCCESS) {
            Timber.d("GATT connection success")
            return
        }

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Timber.d( "Connected GATT service")
            // Discover gatt services if connected
            gatt.discoverServices()

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Timber.d("GATT disconnected")
        }
    }

    /**
     * Called when services are found
     * @param gatt Bluetooth gatt
     * @param status Current gatt status
     */
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        Timber.d("onServicesDiscovered()")
        // Return if status is not success
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return
        }

        Timber.d("onServicesDiscovered()")
        gatt.services?.forEach {
            Timber.d("Service ${it.uuid}")

            // UUID match
            if (it.uuid == service) {
                Timber.d("Found service")
                for (gattCharacteristic in it.characteristics)
                    Timber.d("Characteristic ${gattCharacteristic.uuid}")

                // Enable GATT notify characteristic
                gatt.setCharacteristicNotification(it.characteristics[0], true)
                val descriptor = it.characteristics[0].getDescriptor(rxDescriptor).apply {
                    value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                }
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    /**
     * Called when GATT characteristic is changed
     * @param gatt Bluetooth gatt
     * @param characteristic Gatt characteristic
     */
    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {

        // Get sensor data as a string
        val data = characteristic.getStringValue(0)
        // Viewmodel
        viewModel.changeValue(data)
    }
}

/**
 * @author Nikojoel
 * BtViewModel
 * View model class for incoming sensor data
 */
class BtViewModel: ViewModel() {

    // Observable live data
    private val acc = MutableLiveData<String>()

    /**
     * Changes the observed live data value
     * @param data Sensor data as a string
     */
    fun changeValue(data: String) {
        acc.postValue(data)
    }

    // Emit the data for observer
    val data = acc.switchMap {
        // Live data scope
        liveData(Dispatchers.Main) {
            emit(it)
        }
    }
}
/* EOF */