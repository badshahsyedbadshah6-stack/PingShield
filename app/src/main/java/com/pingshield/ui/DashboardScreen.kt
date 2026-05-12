package com.pingshield.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pingshield.utils.Constants
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val ping by viewModel.ping.collectAsState()
    val spike by viewModel.spike.collectAsState()
    val action by viewModel.currentAction.collectAsState()
    val dns by viewModel.dns.collectAsState()
    val loss by viewModel.loss.collectAsState()
    val lossType by viewModel.lossType.collectAsState()
    val rssi by viewModel.rssi.collectAsState()
    val linkSpeed by viewModel.linkSpeed.collectAsState()
    val blockedCount by viewModel.blockedCount.collectAsState()
    val killedApps by viewModel.killedApps.collectAsState()
    val isVpnActive by viewModel.isVpnActive.collectAsState()
    val jitter by viewModel.jitter.collectAsState()
    val stabilityLabel by viewModel.stabilityLabel.collectAsState()
    val networkScore by viewModel.networkScore.collectAsState()
    val scoreBreakdown by viewModel.scoreBreakdown.collectAsState()
    val channelReport by viewModel.channelReport.collectAsState()
    val wifiWarning by viewModel.wifiWarning.collectAsState()
    val graphData = viewModel.pingHistory

    val pingColor = when {
        ping <= 50 -> Color(0xFF00E5CC)
        ping <= 100 -> Color(0xFFFFB800)
        else -> Color(0xFFFF3B3B)
    }

    val animatedPingScale by animateFloatAsState(
        targetValue = if (isVpnActive) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pingScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ROW 1 - Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PingShield", color = Color(0xFF00E5CC), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (isVpnActive) Color(0xFF00E5CC) else Color.Gray,
                            CircleShape
                        )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isVpnActive) "ON" else "OFF",
                    color = if (isVpnActive) Color(0xFF00E5CC) else Color.Gray,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ROW 2 - Giant ping + jitter
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isVpnActive && ping > 0) "${ping}" else "---",
                color = pingColor,
                fontSize = (64 * animatedPingScale).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text("ms", color = pingColor.copy(alpha = 0.6f), fontSize = 16.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(4.dp))
            Text(
                "Jitter: ${"%.1f".format(jitter)}ms",
                color = Color(0xFFB0B0B0),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                stabilityLabel,
                color = pingColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(16.dp))

        // ROW 3 - Network score ring
        ScoreRing(score = networkScore)

        Spacer(Modifier.height(16.dp))

        // ROW 4 - Live ping graph
        PingGraph(
            data = graphData.value,
            isSpike = spike,
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )

        Spacer(Modifier.height(12.dp))

        // ROW 5 - Stats grid (2x3)
        StatsGrid(
            dns = dns,
            loss = loss,
            lossType = lossType,
            rssi = rssi,
            linkSpeed = linkSpeed,
            jitter = jitter,
            blockedCount = blockedCount,
            channel = channelReport.myChannel,
            interference = channelReport.recommendation
        )

        Spacer(Modifier.height(12.dp))

        // ROW 6 - Score breakdown bars
        ScoreBreakdown(breakdown = scoreBreakdown)

        Spacer(Modifier.height(12.dp))

        // ROW 7 - Current action
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (isVpnActive) action else "Tap LAUNCH PUBG to start",
                color = Color(0xFFB0B0B0),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // ROW 8 - Channel warning (only when interference)
        if (channelReport.interferingAPs > 2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF332200)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "WiFi: ${channelReport.recommendation}",
                    color = Color(0xFFFFB800),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // ROW 9 - Buttons
        Button(
            onClick = { viewModel.onLaunchPubg(context) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(if (isVpnActive) "LAUNCH PUBG" else "START VPN", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = { viewModel.onStop(context) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("STOP", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ScoreRing(score: Int) {
    val color = when {
        score >= 80 -> Color(0xFF00E5CC)
        score >= 50 -> Color(0xFFFFB800)
        else -> Color(0xFFFF3B3B)
    }
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "score"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val sweep = (animatedScore / 100f) * 360f
            drawArc(color = Color(0xFF1A1A24), startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = color, startAngle = -90f, sweepAngle = sweep, useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${score}", color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("Score", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun PingGraph(data: List<Long>, isSpike: Boolean, modifier: Modifier = Modifier) {
    val lineColor = if (isSpike) Color(0xFFFF3B3B) else Color(0xFF00E5CC)
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24)), shape = RoundedCornerShape(12.dp)) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val w = size.width; val h = size.height
            val gridColor = Color(0xFF2A2A2A)
            for (i in 0..4) drawLine(gridColor, Offset(0f, h * i / 4), Offset(w, h * i / 4), 1f)

            if (data.size < 2) {
                drawLine(Color(0xFF555555), Offset(0f, h / 2), Offset(w, h / 2), 2f, StrokeCap.Round)
                return@Canvas
            }

            val maxV = maxOf(data.maxOrNull()?.toFloat() ?: 100f, 100f)
            val minV = minOf(data.minOrNull()?.toFloat() ?: 0f, 0f)
            val range = maxOf(maxV - minV, 50f)
            val stepX = w / (Constants.GRAPH_SIZE - 1).coerceAtLeast(1)

            val path = Path()
            data.forEachIndexed { i, v ->
                val x = i * stepX; val y = h - ((v - minV) / range * h)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun StatsGrid(dns: String, loss: Double, lossType: String, rssi: Int, linkSpeed: Int,
              jitter: Double, blockedCount: Int, channel: Int, interference: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("DNS", dns, Modifier.weight(1f))
            StatCard("Loss", "${"%.1f".format(loss)}%", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("WiFi", if (rssi > Int.MIN_VALUE) "${rssi}dBm" else "N/A", Modifier.weight(1f))
            StatCard("Jitter", "${"%.1f".format(jitter)}ms", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Blocked", "$blockedCount", Modifier.weight(1f))
            StatCard("Ch", if (channel > 0) "${channel} (${if (interference.length < 15) interference else "${interference.take(12)}..."})" else "N/A", Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24)), shape = RoundedCornerShape(10.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Text(value, color = Color(0xFFE0E0E0), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(label, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun ScoreBreakdown(breakdown: com.pingshield.core.NetworkScore) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ScoreBar("Ping", breakdown.pingScore, Color(0xFF00E5CC))
        ScoreBar("Jitter", breakdown.jitterScore, Color(0xFFFFB800), labelSuffix = "(highest weight)")
        ScoreBar("Loss", breakdown.lossScore, Color(0xFF00E676))
        ScoreBar("Signal", breakdown.rssiScore, Color(0xFF448AFF))
    }
}

@Composable
fun ScoreBar(label: String, score: Int, color: Color, labelSuffix: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label $labelSuffix", color = Color(0xFFB0B0B0), fontSize = 11.sp, modifier = Modifier.width(100.dp))
        LinearProgressIndicator(
            progress = { score.toFloat() / 100f },
            modifier = Modifier.weight(1f).height(6.dp),
            color = color,
            trackColor = Color(0xFF1A1A24),
        )
        Spacer(Modifier.width(6.dp))
        Text("$score/100", color = Color(0xFFB0B0B0), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
