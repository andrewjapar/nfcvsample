package dev.andrewjap.nfcvsample

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcV
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.experimental.and


class MainActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        processIntent(intent)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        read_nfc.setOnClickListener {
            val pendingIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
            )
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(checkIntent: Intent?) {
        if (checkIntent == null) return
        if (checkIntent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag = checkIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as Tag
            val serial = reversingByteArray(tag.id).toHexString()

            val nfc = setupNfc(tag)
            val response = nfc.readSingleBlock(45, tag)
            Toast.makeText(this, getInt(response.toHexString()).toString(), Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun setupNfc(tag: Tag): NfcV {
        val nfc = NfcV.get(tag)
        if (nfc.isConnected) {
            Log.d("testo", "NFC has already Connected")
        } else {
            nfc.connect()
            Log.d("testo", "Connecting NFC")
        }
        return nfc
    }

    private fun NfcV.readSingleBlock(blocks: Int, tag: Tag): ByteArray {
        val cmd = byteArrayOf(
            0x20,
            0x20,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  // placeholder for tag UID
            (blocks.toByte() and 0x0ff.toByte())  // number of blocks (-1 as 0x00 means one block)
        )

        System.arraycopy(tag.id, 0, cmd, 2, 8)
        return transceive(cmd)
    }

    private fun NfcV.readMultipleBlock(offset: Int, blocks: Int): ByteArray {
        val cmd = byteArrayOf(
            0x60,
            0x23,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            (offset.toByte() and 0x0ff.toByte()),
            (blocks.toByte() and 0x0ff.toByte())
        )

        System.arraycopy(tag.id, 0, cmd, 2, 8)
        return transceive(cmd)
    }

    private fun reversingByteArray(bArr: ByteArray): ByteArray {
        val result = ByteArray(bArr.size)

        var index = bArr.size - 1
        for (b in bArr) {
            result[index] = b
            index--
        }

        return result
    }

    private fun ByteArray.toHexString(): String {
        return this.joinToString("") {
            java.lang.String.format("%02x", it)
        }
    }

    private fun clearText(b: ByteArray): String {
        var value = String(b)
        value = value.replace("[^ -~]".toRegex(), "");
        value = value.replace("[\\\\p{Cntrl}&&[^\\r\\n\\t]]".toRegex(), "");
        value = value.replace("\\p{C}".toRegex(), "");

        return if (value.isEmpty()) "NULL" else value
    }

    private fun getInt(str: String): Int {
        val result = str.toLong(16)
        return result.toInt()
    }
}

