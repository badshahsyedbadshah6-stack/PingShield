package com.pingshield.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
fun AppsScreen(
    viewModel: AppsViewModel = hiltViewModel()
) {
    val blockedApps by viewModel.blockedApps.collectAsState()
    val whitelist by viewModel.whitelistedPackages.collectAsState()
    val activeApps by viewModel.activeApps.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0F)).padding(16.dp)
    ) {
        Text("App Management", color = Color(0xFF00E5CC), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Text("Blocked (${blockedApps.size})", color = Color(0xFFFF5252), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (blockedApps.isEmpty()) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24)), shape = RoundedCornerShape(10.dp)) {
                Text("None blocked", color = Color.Gray, modifier = Modifier.padding(14.dp))
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().height(150.dp)) {
                items(blockedApps) { pkg ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1A1A)), shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(pkg, color = Color(0xFFFF5252), fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Button({ viewModel.releaseApp(pkg) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), shape = RoundedCornerShape(6.dp)) {
                                Text("Release", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Whitelisted (${whitelist.size})", color = Color(0xFF00E676), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.fillMaxWidth().height(120.dp)) {
            items(whitelist.toList()) { pkg ->
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A)), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(pkg, color = Color(0xFF00E676), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Button({ viewModel.removeWhitelist(pkg) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)), shape = RoundedCornerShape(6.dp)) {
                            Text("Remove", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Active Non-Game Apps", color = Color(0xFFB0B0B0), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.fillMaxWidth()) {
            items(activeApps) { pkg ->
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24)), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(pkg, color = Color(0xFFB0B0B0), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Button({ viewModel.blockApp(pkg) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)), shape = RoundedCornerShape(6.dp)) {
                            Text("Block", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
