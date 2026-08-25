@file:OptIn(ExperimentalMaterial3Api::class)

package com.spectra.lifepilot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spectra.lifepilot.ui.theme.BrandBlue
import com.spectra.lifepilot.ui.theme.BrandCyan
import com.spectra.lifepilot.ui.theme.LifePilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LifePilotTheme { AppRoot() } }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    MONEY("Paisa", Icons.Filled.Payments),
    ADVISOR("Advisor", Icons.Filled.Insights),
}

@Composable
private fun AppRoot() {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(Tab.HOME) }

    val homeVm: DashboardViewModel = viewModel()
    val moneyVm: MoneyViewModel = viewModel()
    val advisorVm: AdvisorViewModel = viewModel()

    // SMS permission (for Money tab)
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> moneyVm.onPermission(granted) }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        moneyVm.onPermission(granted)
    }

    val moneyUi by moneyVm.ui.collectAsState()

    Scaffold(
        topBar = { BrandHeader() },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.label) },
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.HOME -> HomeScreen(homeVm)
                Tab.MONEY -> MoneyScreen(moneyVm) {
                    smsLauncher.launch(Manifest.permission.READ_SMS)
                }
                Tab.ADVISOR -> AdvisorScreen(advisorVm, moneyUi.monthSpent, moneyUi.monthReceived)
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(BrandCyan, BrandBlue)))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text("LifePilot", color = androidx.compose.ui.graphics.Color.White,
            fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}
