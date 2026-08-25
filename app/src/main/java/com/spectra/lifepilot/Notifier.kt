package com.spectra.lifepilot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    private const val CHANNEL = "txn"

    fun show(ctx: Context, t: Txn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Transactions", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val sign = if (t.type == TxnType.DEBIT) "-" else "+"
        val title = if (t.type == TxnType.DEBIT) "Kharcha logged" else "Credit logged"
        val text = "$sign\u20B9${"%,.0f".format(t.amount)}" +
                if (t.note.isNotBlank()) "  \u2022  ${t.note}" else ""

        val notif = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(t.time.toInt(), notif)
        } catch (e: SecurityException) { /* notif permission off - ignore */ }
    }
}
