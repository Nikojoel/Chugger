# Chugger - Android Sensor Application with RuuviTag

### Idea & Description

The idea is to create an application which can be used in any events where drinking is included. With the help of Chugger there will be no more cheating in any drinking games and you don't need to worry about stopping the timer when your drink is empty because Chugger will do it for you. People who use this application are able to see and set accurate times and save the data to highscores so you can compete who is actually the fastest Chugger!


The intent of this project is to use an external sensor and read data from it with an Android device. Used sensor is called [Ruuvi](https://ruuvi.com/) running the [Espruino Firmware](https://lab.ruuvi.com/espruino/). The sensor is programmed with JavaScript to broadcast acceleration data in `X- and Z-axis` every 1000 milliseconds via GATT connection and to broadcast board data `temperature`, `battery voltage`, `humidity` and `atmospheric pressure` with NFC. The Android application is implemented with Kotlin.

### Feature specification
* [BluetoothGatt](https://developer.android.com/reference/android/bluetooth/BluetoothGatt)
    * [BLE](https://developer.android.com/guide/topics/connectivity/bluetooth-le)
* [NFC](https://developer.android.com/guide/topics/connectivity/nfc/nfc)
* [Fragments](https://developer.android.com/guide/components/fragments)
* [Room DB](https://developer.android.com/topic/libraries/architecture/room)
* [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel)
    * [Observer](https://developer.android.com/reference/java/util/Observer)
    * [LiveData](https://developer.android.com/topic/libraries/architecture/livedata)
* [Coroutines](https://developer.android.com/kotlin/coroutines)
* [Adapters](https://developer.android.com/reference/android/widget/Adapter)
    * [RecyclerView](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.Adapter)
    * [FragmentState](https://developer.android.com/reference/androidx/viewpager2/adapter/FragmentStateAdapter)
    

### Sensor configuration
#### By default, the Ruuvi sensor is not broadcasting any GATT data nor is connectable. Therefore, it's needed to update the firmware and flash the correct JavaScript code to it.

***Note: To use Web Bluetooth features in Chrome, experimental Web Platform features need to be enabled in chrome://flags***

1. Install the Espruino firmware [here](http://www.espruino.com/binaries/travis/ffd96ae3ed8c7a3af9994539278e18c0313cd12c/espruino_1v93_ruuvitag.zip)

***Note: The application was developed with the v1.93 firmware and support for newer versions might not be supported***

2. Follow the Ruuvi documentation
```
1. Open Google Play on your mobile
2. Install nRF Connect for Mobile by Nordic Semiconductor ASA
3. Set RuuviTag to bootloader mode by holding down button B and pressing reset button R. Red indicator LED light will light up and stay on.
4. Open internet browser on your mobile. Find and download the desired firmware package (step 1)
5. Open nRF Connect. Tap "Scan" and allow permissions
6. Tap "Scan" at the top right corner of the screen and find sensor "RuuviBoot" from the list. Select "Connect"
7. Tap "DFU" and select "Distribution packet ZIP" from the list, then locate the previously downloaded ZIP file on your mobile
8. Pick the file from the list and tap "Open". The firmware update process will begin. Wait while the process has completed successfully.
```
***Note: Following part has to be done every time the Ruuvitag is reseted (R button is pressed)***

3. Open up the [Espruino Web IDE](https://www.espruino.com/ide/) and insert the [JavaScript](https://github.com/Nikojoel/Chugger/tree/master/app/src/main/java/com/example/chugger/js/Espruino.js) code to the editor.
4. Click the connect button (yellow button, found from upper left corner), press the first option `Web Bluetooth` and select your Ruuvitag from the list.
5. After connection, the left hand side of the IDE should display a message,

```
Connected to Web Bluetooth, RuuviTag XXXX
```

***Note: Installing the code to Flash memory isn't recommended since it will drain the Ruuvitag battery***

6. Press the RAM button (last item in center of the IDE) and wait for the code to be uploaded. Again, the left hand side of the IDE should display a message,
```
 _____                 _
|   __|___ ___ ___ _ _|_|___ ___
|   __|_ -| . |  _| | | |   | . |
|_____|___|  _|_| |___|_|_|_|___|
          |_| http://espruino.com
 1v93 Copyright 2016 G.Williams
Espruino is Open Source. Our work is supported
only by sales of official boards and donations:
http://espruino.com/Donate
```

7. Disconnect your Ruuvitag from the Web Bluetooth (green button, found from upper left corner). IDE message,
```
Disonnected from Web Bluetooth, RuuviTag XXXX
```

### Application Deployment
1. Clone or download the repository.
`git clone https://github.com/Nikojoel/Chugger.git`

2. Launch it on an Android device and follow the in depth instructions found from the application.
