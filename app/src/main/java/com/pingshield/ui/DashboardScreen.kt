package com.pingshield.ui

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import com.pingshield.vpn.PingShieldVpn
import kotlin.math.min

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
    val rssi by viewModel.rssi.collectAsState()
    val blockedCount by viewModel.blockedCount.collectAsState()
    val isVpnActive by viewModel.isVpnActive.collectAsState()
    val graphData = viewModel.graphData

    val pingColor = when {
        ping <= 50 -> Color(0xFF00E676)
        ping <= 100 -> Color(0xFFFFEB3B)
        else -> Color(0xFFFF1744)
    }

    val animatedPingSize by animateFloatAsState(
        targetValue = if (isVpnActive) 1f else 0.8f,
        animationSpec = tween(200)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top status bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PingShield",
                color = Color(0xFF00E5FF),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (isVpnActive) Color(0xFF00E676) else Color.Gray,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isVpnActive) "ON" else "OFF",
                    color = if (isVpnActive) Color(0xFF00E676) else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Giant ping number
        Text(
            text = if (isVpnActive && ping > 0) "${ping}ms" else "---",
            color = pingColor,
            fontSize = (72 * animatedPingSize).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "PING",
            color = Color.Gray,
            fontSize = 14.sp,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Live ping graph (last 30 seconds = 60 values at 500ms intervals)
        PingGraphCard(
            data = graphData,
            isSpike = spike,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "DNS", value = dns)
                StatItem(label = "Loss", value = String.format("%.1f%%", loss))
                StatItem(
                    label = "WiFi",
                    value = if (rssi > Int.MIN_VALUE) "${rssi}dBm" else "N/A"
                )
                StatItem(label = "Blocked", value = "$blockedCount")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Current action text
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isVpnActive) action else "Tap LAUNCH PUBG to start",
                color = Color(0xFFB0B0B0),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // LAUNCH PUBG button
        Button(
            onClick = {
                if (!isVpnActive) {
                    startVpn(context)
                    viewModel.startGameMode()
                } else {
                    launchPubg(context)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isVpnActive) Color(0xFF00897B) else Color(0xFF00BCD4)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (isVpnActive) "LAUNCH PUBG" else "START VPN",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // STOP button
        Button(
            onClick = {
                stopVpn(context)
                viewModel.stopGameMode()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "STOP",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PingGraphCard(
    data: List<Float>,
    isSpike: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        val lineColor = if (isSpike) Color(0xFFFF1744) else Color(0xFF00E676)
        val gridColor = Color(0xFF2A2A2A)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val width = size.width
            val height = size.height
            val maxPing = maxOf(data.maxOrNull() ?: 100f, 100f)
            val minPing = minOf(data.minOrNull() ?: 0f, 0f)
            val range = maxOf(maxPing - minPing, 50f)

            // Draw grid lines
            for (i in 0..4) {
                val y = height - (height * i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            if (data.size < 2) {
                drawLine(
                    color = Color(0xFF555555),
                    start = Offset(0f, height / 2),
                    end = Offset(width, height / 2),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                return@Canvas
            }

            val stepX = width / (Constants.GRAPH_PING_SIZE - 1).coerceAtLeast(1)
            val path = Path()

            data.forEachIndexed { index, pingValue ->
                val x = index * stepX
                val y = height - ((pingValue - minPing) / range * height)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color(0xFFE0E0E0),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}

private fun startVpn(context: Context) {
    val intent = VpnService.prepare(context)
    if (intent != null) {
        context.startActivity(intent)
    } else {
        PingShieldVpn.startVpn(context)
    }
}

private fun stopVpn(context: Context) {
    PingShieldVpn.stopVpn(context)
}

private fun launchPubg(context: Context) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(Constants.GAME_PACKAGE)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
