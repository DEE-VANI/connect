package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.resilience.FailureSimulationManager

@Composable
fun ResilienceBanner(
    simulationManager: FailureSimulationManager,
    onOpenLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNetworkOffline by simulationManager.isNetworkOffline.collectAsState()
    val isApiUnavailable by simulationManager.isApiUnavailable.collectAsState()
    val isDatabaseLocked by simulationManager.isDatabaseLocked.collectAsState()
    val isTimeout by simulationManager.isTimeoutSimulated.collectAsState()
    val isMissingEnv by simulationManager.isMissingEnvSimulated.collectAsState()
    val isMalformedJson by simulationManager.isMalformedResponseSimulated.collectAsState()

    val activeCount = simulationManager.getActiveFaultCount()
    if (activeCount == 0) return

    val label = when {
        isNetworkOffline -> "Offline Campus Mode Active (Link Down)"
        isApiUnavailable -> "API 503 Outage Simulated (Serving Local Cache)"
        isDatabaseLocked -> "Database Lock Fault Injected (Atomic Rollback Active)"
        isTimeout -> "Gateway Timeout (>10s) Injected"
        isMissingEnv -> "Missing Environment Secrets Simulated"
        isMalformedJson -> "Malformed API Response Simulated"
        else -> "Resilience Fault Active ($activeCount faults injected)"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF2F2))
            .clickable { onOpenLab() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("app_resilience_fault_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "CHAOS SIMULATION ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFDC2626),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF991B1B),
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onOpenLab,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Lab", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 12.sp)
                }
                TextButton(
                    onClick = { simulationManager.resetAllFaults() },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Clear", fontWeight = FontWeight.Bold, color = Color(0xFF4B5563), fontSize = 12.sp)
                }
            }
        }
    }
}
