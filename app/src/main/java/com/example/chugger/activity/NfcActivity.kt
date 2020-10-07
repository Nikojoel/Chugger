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
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chugger.R
import kotlinx.android.synthetic.main.activity_nfc.*
import kotlinx.android.synthetic.main.tool_bar.*
import timber.log.Timber

/**
 * @author Jalmares
 * NfcActivity
 * Activity that lets you scan RuuviTag with NFC and display data
 */

class NfcActivity: AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var nfcManager: NfcManager
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)

        setSupportActionBar(findViewById(R.id.tool_bar))
        toolBarText.visibility = View.GONE
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        nfcManager = getSystemService(Context.NFC_SERVICE) as NfcManager
        nfcAdapter = nfcManager.defaultAdapter
        /**
        Creates a generic PendingIntent that will be deliver to this activity.
        The NFC stack will fill in the intent with the details of the discovered tag before delivering to
        this activity.
        */
        pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

        Toast.makeText(this, getString(R.string.nfcTagString), Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        // Get all NDEF discovered intents
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch, as this activity is no longer in the foreground
        nfcAdapter.disableForegroundDispatch(this);
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("Found intent in onNewIntent ${intent?.action}")
        /**
        If we got an intent while the app is running, also check if it's a new NDEF message
        that was discovered
        */
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
        // Check if intent has the action of a discovered NFC tag with NDEF formatted contents
        else if (checkIntent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            // Retrieve the raw NDEF message from the tag
            val rawMessages = checkIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            Timber.d("Raw messages ${rawMessages?.size}")
            // Parsing NDEF messages
            if (rawMessages != null) {
                val messages =
                    arrayOfNulls<NdefMessage?>(rawMessages.size)
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
                // Print the number of records
                Timber.d("Records ${curMsg.records.size}")
                // Loop through all the records contained in the message
                for (curRecord in curMsg.records) {
                    if (curRecord.toUri() != null) {
                        // URI NDEF Tag
                        Timber.d("- URI ${curRecord.toUri()}")
                        val data = curRecord.toUri().toString().split(",")

                        battery.text = getString(R.string.nfcBatteryString, data[0])
                        temperature.text = getString(R.string.nfcTempString, data[1])
                        pressure.text= getString(R.string.nfcPressureString, data[2])
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
/* EOF */