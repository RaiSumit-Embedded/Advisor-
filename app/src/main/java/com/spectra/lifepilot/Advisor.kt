package com.spectra.lifepilot

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface AdvisorState {
    data object Idle : AdvisorState
    data object NeedKey : AdvisorState
    data object Loading : AdvisorState
    data class Result(val text: String) : AdvisorState
    data class Error(val msg: String) : AdvisorState
}

class AdvisorViewModel(app: Application) : AndroidViewModel(app) {
    private val store = DayStore(app)
    private val settings = SettingsStore(app)

    var apiKey by mutableStateOf(settings.apiKey)
        private set
    var model by mutableStateOf(settings.model)
        private set

    fun saveKey(k: String) { settings.apiKey = k; apiKey = settings.apiKey }
    fun saveModel(m: String) { settings.model = m; model = settings.model }

    private val _state = MutableStateFlow<AdvisorState>(AdvisorState.Idle)
    val state: StateFlow<AdvisorState> = _state.asStateFlow()

    private val system = """
        Tu 'LifePilot', ek no-nonsense personal life coach hai. Hinglish me baat kar (casual Hindi-English mix).
        Tera kaam: banda ke aaj ke data ko dekh kar usse push karna — health, neend, paisa.
        Rules:
        - Seedhi, punchy baat. Ghumao mat. Motivational lekin real, thoda savage bhi chalega.
        - Pehle 2-3 sharp observation (data ke numbers use kar).
        - Phir aaj ke liye 1 clear ACTION de.
        - Total 120 words se kam. No disclaimers, no 'as an AI'.
    """.trimIndent()

    fun ask(monthSpent: Double, monthReceived: Double) {
        val key = settings.apiKey
        if (key.isBlank()) { _state.value = AdvisorState.NeedKey; return }

        val days = store.last7Days()
        val avgSteps = if (days.isNotEmpty()) days.map { it.steps }.average().toInt() else 0
        val todaySteps = days.lastOrNull()?.steps ?: 0
        val lastSleepMin = days.lastOrNull()?.sleepMinutes ?: 0
        val summary = buildString {
            append("Aaj ke steps: $todaySteps. ")
            append("7-din avg steps: $avgSteps. ")
            append("Aaj ki neend: ${lastSleepMin / 60}h ${lastSleepMin % 60}m. ")
            append("Is mahine kharch: Rs.${"%,.0f".format(monthSpent)}. ")
            append("Is mahine aaya: Rs.${"%,.0f".format(monthReceived)}. ")
            append("In numbers pe mera aaj ka coaching de.")
        }

        _state.value = AdvisorState.Loading
        viewModelScope.launch {
            _state.value = try {
                val text = withContext(Dispatchers.IO) { callClaude(key, settings.model, system, summary) }
                AdvisorState.Result(text)
            } catch (e: Exception) {
                AdvisorState.Error(e.message ?: "Kuch gadbad ho gayi")
            }
        }
    }

    private fun callClaude(key: String, model: String, sys: String, user: String): String {
        val conn = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 20000
        conn.readTimeout = 40000
        conn.setRequestProperty("x-api-key", key)
        conn.setRequestProperty("anthropic-version", "2023-06-01")
        conn.setRequestProperty("content-type", "application/json")
        conn.doOutput = true

        val body = JSONObject()
            .put("model", model)
            .put("max_tokens", 500)
            .put("system", sys)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", user)))

        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)
            .bufferedReader().use { it.readText() }
        if (code !in 200..299) throw Exception("API $code: ${resp.take(200)}")

        val content = JSONObject(resp).getJSONArray("content")
        for (i in 0 until content.length()) {
            val b = content.getJSONObject(i)
            if (b.optString("type") == "text") return b.getString("text").trim()
        }
        return "(no response)"
    }
}

@Composable
fun AdvisorScreen(vm: AdvisorViewModel, monthSpent: Double, monthReceived: Double) {
    val state by vm.state.collectAsState()
    var showSettings by remember { mutableStateOf(vm.apiKey.isBlank()) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AI Advisor", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = { showSettings = !showSettings }) { Text(if (showSettings) "Chhupao" else "Settings") }
        }

        if (showSettings) {
            KeySettings(vm)
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = { vm.ask(monthSpent, monthReceived) },
            enabled = state !is AdvisorState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("\uD83D\uDD25 Aaj ka insight lo") }

        Spacer(Modifier.height(16.dp))

        when (val s = state) {
            is AdvisorState.Idle -> Text("Button dabao — data ke hisaab se aaj ka coaching milega.", fontSize = 13.sp)
            is AdvisorState.NeedKey -> {
                LaunchedEffect(Unit) { showSettings = true }
                Text("Pehle apni Anthropic API key daalo (Settings me).",
                    color = MaterialTheme.colorScheme.error)
            }
            is AdvisorState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp)); Text("Soch raha hoon...")
            }
            is AdvisorState.Result -> Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) { Text(s.text, Modifier.padding(16.dp), fontSize = 15.sp) }
            is AdvisorState.Error -> Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) { Text(s.msg, Modifier.padding(16.dp), fontSize = 13.sp) }
        }
    }
}

@Composable
private fun KeySettings(vm: AdvisorViewModel) {
    var key by remember { mutableStateOf(vm.apiKey) }
    var model by remember { mutableStateOf(vm.model) }
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Anthropic API key", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = key, onValueChange = { key = it },
                placeholder = { Text("sk-ant-...") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = model, onValueChange = { model = it },
                label = { Text("Model") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.saveKey(key); vm.saveModel(model) }) { Text("Save") }
            Spacer(Modifier.height(6.dp))
            Text("Key console.anthropic.com se milegi. Phone me hi save hoti hai, kahin bheji nahi jaati (sirf API call me).",
                fontSize = 11.sp)
        }
    }
}
