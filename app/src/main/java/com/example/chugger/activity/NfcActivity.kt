package com.example.chugger.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chugger.R
import kotlinx.android.synthetic.main.activity_nfc.*
import timber.log.Timber

class NfcActivity: AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var nfcManager: NfcManager
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)

        setSupportActionBar(findViewById(R.id.tool_bar))
        supportActionBar?.setTitle(R.string.app_name)

        nfcManager = getSystemService(Context.NFC_SERVICE) as NfcManager
        nfcAdapter = nfcManager.defaultAdapter


        // Read all tags when app is running and in the foreground (FLAG_ACTIVITY_SINGLE_TOP)
        // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
        // will fill in the intent with the details of the discovered tag before delivering to
        // this activity.
        pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

        Toast.makeText(this, "Scan your tag to receive board data", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
        // Get all NDEF discovered intents
        // Makes sure the app gets all discovered NDEF messages as long as it's in the foreground.
    }

    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch, as this activity is no longer in the foreground
        nfcAdapter.disableForegroundDispatch(this);
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("Found intent in onNewIntent ${intent?.action}")
        // If we got an intent while the app is running, also check if it's a new NDEF message
        // that was discovered
        if (intent != null) processIntent(intent)
    }

    private fun processIntent(checkIntent: Intent) {
        Timber.d("process intent $intent")
        if (checkIntent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag: Tag? = checkIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            Timber.d("Raw tag $tag")
            val c =
                IsoDep.get(tag) // https://developer.android.com/reference/android/nfc/tech/IsoDep
            Timber.d("IsoDep $c")
        }
        // Check if intent has the action of a discovered NFC tag
        // with NDEF formatted contents
        else if (checkIntent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            //Timber.d("New NDEF intent $checkIntent")
            // Retrieve the raw NDEF message from the tag
            val rawMessages = checkIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            Timber.d("Raw messages ${rawMessages?.size}")
            // Complete variant: parse NDEF messages
            if (rawMessages != null) {
                val messages =
                    arrayOfNulls<NdefMessage?>(rawMessages.size)// Array<NdefMessage>(rawMessages.size, {})
                for (i in rawMessages.indices) {
                    messages[i] = rawMessages[i] as NdefMessage;
                }
                // Process the messages array.
                processNdefMessages(messages)
            }
        }
    }
    private fun processNdefMessages(ndefMessages: Array<NdefMessage?>) {
        // Go through all NDEF messages found on the NFC tag
        for (curMsg in ndefMessages) {
            if (curMsg != null) {
                // Print generic information about the NDEF message
                Timber.d("Message $curMsg")
                // The NDEF message usually contains 1+ records - print the number of records
                Timber.d("Records ${curMsg.records.size}")
                // Loop through all the records contained in the message
                for (curRecord in curMsg.records) {
                    if (curRecord.toUri() != null) {
                        // URI NDEF Tag
                        Timber.d("- URI ${curRecord.toUri()}")
                        val data = curRecord.toUri().toString().split(",")

                        battery.text = "${data[0]} V"
                        temperature.text = "${data[1]} °C"
                        pressure.text= "${data[2]} hPa"
                        humidity.text = "${data[3]} %"
                    } else {
                        // Other NDEF Tags - simply print the payload
                        Timber.d("- Contents ${curRecord.payload}")
                    }
                }
            }
        }
    }
}