package com.example.smsexpensetracker.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smsexpensetracker.R
import com.example.smsexpensetracker.SmsExpenseApp
import com.example.smsexpensetracker.parser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG          = "SmsReceiver"
        private const val CHANNEL_ID   = "expense_alerts"
        private const val CHANNEL_NAME = "Expense Alerts"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Group multi-part SMS by sender and concatenate
        messages.groupBy { it.originatingAddress ?: "" }.forEach { (sender, parts) ->
            val body = parts.joinToString("") { it.messageBody }
            processSms(context, sender, body)
        }
    }

    private fun processSms(context: Context, sender: String, body: String) {
        Log.d(TAG, "SMS from [$sender]: $body")

        val transaction = SmsParser.parse(sender, body)
        if (transaction == null) {
            Log.d(TAG, "Not a financial SMS — skipped")
            return
        }

        Log.d(TAG, "Parsed: ${transaction.category} | ${transaction.type} | ₹${transaction.amountPaise / 100.0}")

        val repository  = (context.applicationContext as SmsExpenseApp).repository
        val pendingResult = goAsync()   // gives ~10 s beyond onReceive for async work

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (repository.isDuplicate(transaction)) {
                    Log.d(TAG, "Duplicate SMS — skipped")
                    return@launch
                }
                repository.insertTransaction(transaction)
                Log.d(TAG, "Saved transaction ₹${transaction.amountPaise / 100.0}")
                showNotification(context, transaction.amountPaise, transaction.category.name)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transaction", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, amountPaise: Long, category: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Expense captured from SMS" }
        )

        val rupees = "₹${"%.2f".format(amountPaise / 100.0)}"
        nm.notify(
            System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Expense Captured")
                .setContentText("$category  $rupees recorded automatically")
                .setAutoCancel(true)
                .build()
        )
    }
}
