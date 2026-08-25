package com.spectra.lifepilot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun HomeScreen(vm: DashboardViewModel) {
    val ctx = LocalContext.current
    val days by vm.days.collectAsState()
    val stepSensor = remember { StepSensor(ctx) }

    val needsPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var hasPerm by remember {
        mutableStateOf(
            !needsPerm || ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPerm = granted }

    LaunchedEffect(Unit) { if (!hasPerm) permLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }

    LifecycleStartEffect(hasPerm) {
        if (hasPerm) {
            stepSensor.onSteps = { total -> vm.onCumulativeSteps(total) }
            stepSensor.start()
        }
        onStopOrDispose { stepSensor.stop() }
    }

    var showSleep by remember { mutableStateOf(false) }
    if (showSleep) SleepDialog(
        onDismiss = { showSleep = false },
        onSave = { m -> vm.logSleep(m); showSleep = false }
    )

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            val today = days.lastOrNull()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), Icons.Filled.DirectionsWalk, "Aaj ke steps",
                    (today?.steps ?: 0).toString())
                StatCard(Modifier.weight(1f), Icons.Filled.Bedtime, "Aaj ki neend",
                    fmtSleep(today?.sleepMinutes ?: 0))
            }
            if (!stepSensor.available) {
                Spacer(Modifier.height(12.dp)); InfoChip("Is phone me step sensor nahi mila.")
            } else if (!hasPerm) {
                Spacer(Modifier.height(12.dp)); InfoChip("Steps ke liye Physical Activity permission do.")
            }
            Spacer(Modifier.height(20.dp))
            Text("Pichhle 7 din", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days.reversed()) { d ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(d.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM")))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("${d.steps} \uD83D\uDC5F"); Text("${fmtSleep(d.sleepMinutes)} \uD83D\uDE34")
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showSleep = true },
            icon = { Icon(Icons.Filled.Bedtime, null) },
            text = { Text("Neend log") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}

@Composable
private fun SleepDialog(onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var hours by remember { mutableFloatStateOf(7f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kitne ghante soye?") },
        text = {
            Column {
                Text(fmtSleep((hours * 60).roundToInt().toLong()), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Slider(value = hours, onValueChange = { hours = it }, valueRange = 0f..12f, steps = 47)
            }
        },
        confirmButton = { TextButton(onClick = { onSave((hours * 60).roundToInt().toLong()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun StatCard(modifier: Modifier, icon: ImageVector, label: String, value: String) {
    Card(modifier, colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp)
        }
    }
}

@Composable
private fun InfoChip(msg: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Text(msg, Modifier.padding(12.dp), fontSize = 13.sp)
    }
}

private fun fmtSleep(min: Long): String = if (min <= 0) "--" else "${min / 60}h ${min % 60}m"
