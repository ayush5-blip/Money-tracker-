package com.example.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.model.ParsedSmsCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InboxScanResult(
    val totalMessagesScanned: Int,
    val detectedTransactions: List<ParsedSmsCandidate>
)

object SmsInboxReader {

    suspend fun scanInbox(
        context: Context,
        defaultCurrency: String,
        existingHashes: Set<String>,
        maxMessages: Int = 500
    ): InboxScanResult = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("READ_SMS permission is required to scan the SMS inbox.")
        }

        val detected = mutableListOf<ParsedSmsCandidate>()
        var totalScanned = 0

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)

            while (cursor.moveToNext() && totalScanned < maxMessages) {
                totalScanned++
                val sender = if (addressIdx >= 0) cursor.getString(addressIdx).orEmpty() else ""
                val body = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
                val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()

                val parsed = SmsParserEngine.parseMessage(
                    rawMessage = body,
                    sender = sender,
                    timestampMillis = date,
                    defaultCurrency = defaultCurrency,
                    existingHashes = existingHashes
                )
                if (parsed != null) {
                    detected.add(parsed)
                }
            }
        }

        InboxScanResult(
            totalMessagesScanned = totalScanned,
            detectedTransactions = detected
        )
    }
}
