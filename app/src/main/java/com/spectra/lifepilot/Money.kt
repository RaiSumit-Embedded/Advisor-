package com.spectra.lifepilot

import android.app.Application
import android.provider.Telephony
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class TxnType { DEBIT, CREDIT }
data class Txn(val amount: Double, val type: TxnType, val note: String, val time: Long)

/** Parses Indian bank / UPI transaction SMS. */
object SmsParser {
    private val amount = Regex("""(?:rs|inr)\.?\s?([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val debitWords = listOf("debited", "spent", "withdrawn", "paid", "purchase", "debit", "sent")
    private val creditWords = listOf("credited", "received", "deposited", "credit", "refund")

    fun parse(body: String, time: Long): Txn? {
        val l = body.lowercase()
        val isDebit = debitWords.any { l.contains(it) }
        val isCredit = creditWords.any { l.contains(it) }
        if (!isDebit && !isCredit) return null
        val m = amount.find(body) ?: return null
        val amt = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (amt <= 0) return null
        val type = if (isCredit && !isDebit) TxnType.CREDIT else TxnType.DEBIT
        return Txn(amt, type, extractNote(body), time)
    }

    private val noteRegex = Regex("""(?:to|at|towards|VPA)\s+([A-Za-z0-9@._ ]{3,30})""", RegexOption.IGNORE_CASE)
    private fun extractNote(body: String): String =
        noteRegex.find(body)?.groupValues?.get(1)?.trim()?.take(24) ?: ""
}

data class MoneyUi(
    val loading: Boolean = false,
    val needsPermission: Boolean = true,
    val monthSpent: Double = 0.0,
    val monthReceived: Double = 0.0,
    val txns: List<Txn> = emptyList(),
)

class MoneyViewModel(app: Application) : AndroidViewModel(app) {
    private val _ui = MutableStateFlow(MoneyUi())
    val ui: StateFlow<MoneyUi> = _ui.asStateFlow()

    fun onPermission(granted: Boolean) {
        _ui.value = _ui.value.copy(needsPermission = !granted)
        if (granted) load()
    }

    fun load() {
        _ui.value = _ui.value.copy(loading = true, needsPermission = false)
        viewModelScope.launch {
            val txns = withContext(Dispatchers.IO) { readAndParse() }
            val zone = ZoneId.systemDefault()
            val now = java.time.LocalDate.now(zone)
            var spent = 0.0; var recv = 0.0
            txns.forEach {
                val d = Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate()
                if (d.month == now.month && d.year == now.year) {
                    if (it.type == TxnType.DEBIT) spent += it.amount else recv += it.amount
                }
            }
            _ui.value = MoneyUi(false, false, spent, recv, txns.take(60))
        }
    }

    private fun readAndParse(): List<Txn> {
        val out = mutableListOf<Txn>()
        val resolver = getApplication<Application>().contentResolver
        val cutoff = System.currentTimeMillis() - 60L * 24 * 3600 * 1000 // 60 days
        val cursor = resolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(cutoff.toString()),
            "${Telephony.Sms.DATE} DESC"
        )
        cursor?.use { c ->
            val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)
            var count = 0
            while (c.moveToNext() && count < 1000) {
                count++
                val body = c.getString(bodyIdx) ?: continue
                val date = c.getLong(dateIdx)
                SmsParser.parse(body, date)?.let { out.add(it) }
            }
        }
        return out
    }
}

@Composable
fun MoneyScreen(vm: MoneyViewModel, onRequestPermission: () -> Unit) {
    val ui by vm.ui.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (ui.needsPermission) {
            Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                Text("Kharche auto-track karo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("Bank/UPI ke transaction SMS padhkar app khud expense banayega.",
                    fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRequestPermission) { Text("SMS access do") }
            }
            return
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MoneyCard(Modifier.weight(1f), "Is mahine kharch", ui.monthSpent, false)
            MoneyCard(Modifier.weight(1f), "Is mahine aaya", ui.monthReceived, true)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent transactions", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.load() }) { Text("Refresh") }
        }
        if (ui.loading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() }
        } else if (ui.txns.isEmpty()) {
            Text("Koi transaction SMS nahi mila (ya abhi tak aaya nahi).", fontSize = 13.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.txns) { t -> TxnRow(t) }
            }
        }
    }
}

@Composable
private fun MoneyCard(modifier: Modifier, label: String, amount: Double, positive: Boolean) {
    Card(modifier, colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("\u20B9${"%,.0f".format(amount)}", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = if (positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Text(label, fontSize = 12.sp)
        }
    }
}

private val fmt = DateTimeFormatter.ofPattern("dd MMM")
@Composable
private fun TxnRow(t: Txn) {
    val d = Instant.ofEpochMilli(t.time).atZone(ZoneId.systemDefault()).toLocalDate()
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (t.note.isBlank()) (if (t.type == TxnType.DEBIT) "Kharch" else "Credit") else t.note,
                    fontWeight = FontWeight.Medium)
                Text(d.format(fmt), fontSize = 12.sp)
            }
            Text((if (t.type == TxnType.DEBIT) "-" else "+") + "\u20B9${"%,.0f".format(t.amount)}",
                fontWeight = FontWeight.Bold,
                color = if (t.type == TxnType.DEBIT) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary)
        }
    }
}
