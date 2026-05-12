package com.pingshield.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val autoStart by viewModel.autoStart.collectAsState()
    val autoFlushDns by viewModel.autoFlushDns.collectAsState()
    val preResolve by viewModel.preResolve.collectAsState()
    val wifiLowLatency by viewModel.wifiLowLatency.collectAsState()
    val wakeLock by viewModel.wakeLock.collectAsState()
    val perfMode by viewModel.perfMode.collectAsState()
    val autoSwitchMobile by viewModel.autoSwitchMobile.collectAsState()
    val dscpMarking by viewModel.dscpMarking.collectAsState()
    val tcpPsh by viewModel.tcpPsh.collectAsState()
    val autoReconnectBurst by viewModel.autoReconnectBurst.collectAsState()
    val notifPing by viewModel.notifPing.collectAsState()
    val notifSpike by viewModel.notifSpike.collectAsState()
    val notifScore by viewModel.notifScore.collectAsState()
    val notifBlocker by viewModel.notifBlocker.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", color = Color(0xFF00E5CC), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // General
        SectionTitle("General")
        SettingsToggle("Auto-start on Boot", autoStart) { viewModel.toggleAutoStart(it) }
        SettingsToggle("Pre-resolve DNS on launch", preResolve) { viewModel.togglePreResolve(it) }

        Spacer(Modifier.height(16.dp))
        SectionTitle("DNS")
        SettingsToggle("Auto-flush DNS on spike", autoFlushDns) { viewModel.toggleAutoFlushDns(it) }
        SettingsToggle("Auto-reconnect on burst loss", autoReconnectBurst) { viewModel.toggleAutoReconnectBurst(it) }

        Spacer(Modifier.height(16.dp))
        SectionTitle("WiFi")
        SettingsToggle("WiFi Low Latency Lock", wifiLowLatency) { viewModel.toggleWifiLowLatency(it) }
        SettingsToggle("CPU Wake Lock", wakeLock) { viewModel.toggleWakeLock(it) }
        SettingsToggle("Performance Mode (Android 12+)", perfMode) { viewModel.togglePerfMode(it) }
        SettingsToggle("Auto-switch to Mobile Data", autoSwitchMobile) { viewModel.toggleAutoSwitchMobile(it) }

        Spacer(Modifier.height(16.dp))
        SectionTitle("VPN")
        SettingsToggle("DSCP Packet Marking", dscpMarking) { viewModel.toggleDscpMarking(it) }
        SettingsToggle("TCP PSH Flag Forcing", tcpPsh) { viewModel.toggleTcpPsh(it) }

        Spacer(Modifier.height(16.dp))
        SectionTitle("Notifications")
        SettingsToggle("Show ping in notification", notifPing) { viewModel.toggleNotifPing(it) }
        SettingsToggle("Spike alerts", notifSpike) { viewModel.toggleNotifSpike(it) }
        SettingsToggle("Show score in notification", notifScore) { viewModel.toggleNotifScore(it) }
        SettingsToggle("Block notifications during game", notifBlocker) { viewModel.toggleNotifBlocker(it) }

        Spacer(Modifier.height(24.dp))
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("About", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("PingShield v2.0.0", color = Color(0xFFB0B0B0), fontSize = 13.sp)
                Text("Real-time network optimizer for PUBG Mobile", color = Color(0xFFB0B0B0), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color(0xFF00E5CC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00E5CC),
                    checkedTrackColor = Color(0xFF008380)
                )
            )
        }
    }
}
