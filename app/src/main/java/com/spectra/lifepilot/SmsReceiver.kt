package com.spectra.lifepilot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val body = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            ?.joinToString("") { it.messageBody ?: "" } ?: return
        SmsParser.parse(body, System.currentTimeMillis())?.let { Notifier.show(context, it) }
    }
}
