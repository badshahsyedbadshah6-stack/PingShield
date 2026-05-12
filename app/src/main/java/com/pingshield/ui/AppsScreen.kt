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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val whitelistedApps by viewModel.whitelistedPackages.collectAsState()
    val activeApps by viewModel.activeApps.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(16.dp)
    ) {
        Text(
            text = "App Management",
            color = Color(0xFF00E5FF),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Currently blocked apps section
        Text(
            text = "Currently Blocked (${blockedApps.size})",
            color = Color(0xFFFF5252),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (blockedApps.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "No apps currently blocked",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                items(blockedApps) { pkg ->
                    BlockedAppItem(pkg, onRelease = { viewModel.releaseApp(pkg) })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Whitelisted apps
        Text(
            text = "Whitelisted Apps (${whitelistedApps.size})",
            color = Color(0xFF00E676),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            items(whitelistedApps.toList()) { pkg ->
                WhitelistedAppItem(pkg, onRemove = { viewModel.removeWhitelist(pkg) })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Active non-game apps
        Text(
            text = "Detected Non-Game Apps",
            color = Color(0xFFB0B0B0),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(activeApps) { pkg ->
                ActiveAppItem(pkg, onBlock = { viewModel.blockApp(pkg) })
            }
        }
    }
}

@Composable
fun BlockedAppItem(pkg: String, onRelease: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1A1A)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pkg,
                color = Color(0xFFFF5252),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onRelease,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Release", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WhitelistedAppItem(pkg: String, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pkg,
                color = Color(0xFF00E676),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onRemove,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Remove", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ActiveAppItem(pkg: String, onBlock: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pkg,
                color = Color(0xFFB0B0B0),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onBlock,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Block", fontSize = 12.sp)
            }
        }
    }
}
