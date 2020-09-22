package com.example.chugger.bluetooth

import android.bluetooth.*
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.util.*

class GattCallBack(private val viewModel: BtViewModel): BluetoothGattCallback() {

    private val service = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val rxDescriptor = convertFromInteger(0x2902)

    private fun convertFromInteger(i: Int): UUID {
        val MSB = 0x0000000000001000L
        val LSB = -0x7fffff7fa064cb05L
        val value = (i and -0x1).toLong()
        return UUID(MSB or (value shl 32), LSB)
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        if (status == BluetoothGatt.GATT_FAILURE) {
            Timber.d("GATT connection failure")
            return
        } else if (status != BluetoothGatt.GATT_SUCCESS) {
            Timber.d("GATT connection success")
            return
        }
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Timber.d( "Connected GATT service")
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Timber.d("GATT disconnected")
        }
    }
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        Timber.d("onServicesDiscovered()")
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return
        }

        Timber.d("onServicesDiscovered()")
        gatt.services?.forEach {

            Timber.d("Service ${it.uuid}")
            if (it.uuid == service) {
                Timber.d("Found service")
                for (gattCharacteristic in it.characteristics)
                    Timber.d("Characteristic ${gattCharacteristic.uuid}")

                gatt.setCharacteristicNotification(it.characteristics[0], true)
                val descriptor = it.characteristics[0].getDescriptor(rxDescriptor).apply {
                    value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                }
                gatt.writeDescriptor(descriptor)
            }
        }
    }
    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        Timber.d("onDescriptorWrite")
    }
    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val data = characteristic.getStringValue(0)
        viewModel.changeValue(data)
    }
}

class BtViewModel: ViewModel() {
    private val acc = MutableLiveData<String>()

    fun changeValue(data: String) {
        acc.postValue(data)
    }

    val data = acc.switchMap {
        liveData(Dispatchers.Main) {
            emit(it)
        }
    }
}