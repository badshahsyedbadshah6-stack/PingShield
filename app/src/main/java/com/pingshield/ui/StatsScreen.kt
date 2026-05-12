package com.pingshield.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val ping by viewModel.ping.collectAsState()
    val jitter by viewModel.jitter.collectAsState()
    val loss by viewModel.loss.collectAsState()
    val lossType by viewModel.lossType.collectAsState()
    val burstCount by viewModel.burstCount.collectAsState()
    val killedApps by viewModel.killedApps.collectAsState()
    val networkScore by viewModel.networkScore.collectAsState()
    val smoothedPing by viewModel.smoothedPing.collectAsState()
    val spikeThreshold by viewModel.spikeThreshold.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Session Stats", color = Color(0xFF00E5CC), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        StatRow("Current Ping", "${ping}ms")
        StatRow("Smoothed Ping", "${"%.1f".format(smoothedPing)}ms")
        StatRow("Spike Threshold", "${"%.1f".format(spikeThreshold)}ms")
        StatRow("Jitter (IPDV)", "${"%.1f".format(jitter)}ms")
        StatRow("Packet Loss", "${"%.1f".format(loss)}%")
        StatRow("Loss Type", lossType)
        StatRow("Burst Events", "$burstCount")
        StatRow("Network Score", "$networkScore/100")
        StatRow("Apps Killed", "${killedApps.size}")
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFB0B0B0), fontSize = 14.sp)
            Text(value, color = Color(0xFFE0E0E0), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}
