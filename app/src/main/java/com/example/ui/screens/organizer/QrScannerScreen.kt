package com.example.ui.screens.organizer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entities.EventEntity
import com.example.data.local.entities.TicketEntity
import com.example.data.local.entities.TicketStatus
import com.example.data.local.entities.UserEntity
import com.example.data.repository.CampusConnectRepository
import com.example.data.repository.CheckInResult
import com.example.ui.components.CameraScannerView
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    eventId: Long,
    repository: CampusConnectRepository,
    onNavigateBack: () -> Unit,
    onNavigateToAttendees: (Long) -> Unit
) {
    val context = LocalContext.current
    val currentUser by repository.currentUser.collectAsState()
    val event by repository.getEventById(eventId).collectAsState(initial = null)
    val eventTickets by repository.getEventTickets(eventId).collectAsState(initial = emptyList())

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualTokenInput by remember { mutableStateOf("") }
    var lastCheckInResult by remember { mutableStateOf<CheckInResult?>(null) }
    var isCheckingIn by remember { mutableStateOf(false) }

    fun triggerVibrate(isSuccess: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (isSuccess) {
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                } else {
                    VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 100), -1)
                }
                vibrator.vibrate(effect)
            }
        } catch (_: Exception) {}
    }

    suspend fun performCheckIn(token: String) {
        val organizer = currentUser ?: return
        val result = repository.validateAndCheckIn(token, eventId, organizer)
        lastCheckInResult = result
        when (result) {
            is CheckInResult.Success -> triggerVibrate(true)
            is CheckInResult.Duplicate, is CheckInResult.WrongEvent, is CheckInResult.NotFound -> triggerVibrate(false)
            else -> {}
        }
    }

    val checkedInCount = eventTickets.count { it.status == TicketStatus.CHECKED_IN }
    val totalTickets = eventTickets.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Entrance QR Scanner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(event?.title ?: "Event Check-in", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_scanner")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToAttendees(eventId) }, modifier = Modifier.testTag("btn_view_attendees_from_scanner")) {
                        Icon(Icons.Default.People, contentDescription = "View Attendees")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Event Status Strip
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CHECK-IN PROGRESS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$checkedInCount of $totalTickets Checked In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (totalTickets > 0) "${(checkedInCount * 100) / totalTickets}%" else "0%",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Camera / Viewfinder
            item {
                CameraScannerView(
                    hasCameraPermission = hasCameraPermission,
                    modifier = Modifier.testTag("camera_scanner_view")
                )
            }

            // Validation Result Banner
            if (lastCheckInResult != null) {
                item {
                    when (val res = lastCheckInResult) {
                        is CheckInResult.Success -> {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("result_success_banner"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF059669))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "VALID TICKET • CHECKED IN",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF065F46),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = res.ticket.attendeeName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF064E3B)
                                        )
                                        Text(
                                            text = "${res.ticket.attendeeStudentId} • ${res.ticket.attendeeDepartment}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF047857)
                                        )
                                        Text(
                                            text = "Token: ${res.ticket.ticketToken}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color(0xFF065F46)
                                        )
                                    }
                                }
                            }
                        }

                        is CheckInResult.Duplicate -> {
                            val sdf = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
                            val checkInTimeStr = sdf.format(Date(res.alreadyCheckedInAt))
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("result_duplicate_banner"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD97706))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = "Duplicate Warning",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "⚠️ DUPLICATE CHECK-IN DETECTED",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Already checked in at $checkInTimeStr",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF78350F)
                                        )
                                        Text(
                                            text = "Attendee: ${res.ticket.attendeeName} (${res.ticket.attendeeStudentId})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF92400E)
                                        )
                                        if (res.checkedInBy != null) {
                                            Text(
                                                text = "Verified by: ${res.checkedInBy}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF92400E)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is CheckInResult.WrongEvent -> {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("result_wrong_event_banner"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "WRONG EVENT TICKET",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Ticket issued for: ${res.ticketEventTitle}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF7F1D1D)
                                        )
                                    }
                                }
                            }
                        }

                        is CheckInResult.NotFound -> {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("result_not_found_banner"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                            ) {
                                Text(
                                    text = "Invalid Ticket Token: \"${res.token}\" not found in campus database.",
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        is CheckInResult.Cancelled -> {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("result_cancelled_banner"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                            ) {
                                Text(
                                    text = "Cancelled Ticket: This registration was cancelled by the student.",
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }

            // Manual Code Entry
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Manual Ticket Code Check-In",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualTokenInput,
                                onValueChange = { manualTokenInput = it },
                                placeholder = { Text("e.g. TK-CC-...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_manual_token")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (manualTokenInput.isNotBlank()) {
                                        kotlinx.coroutines.runBlocking {
                                            performCheckIn(manualTokenInput.trim())
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("btn_verify_manual_token")
                            ) {
                                Text("Check In")
                            }
                        }
                    }
                }
            }

            // Quick-Scan Ticket Simulator tray
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Emulator Quick-Scan Tray",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap any ticket below to simulate immediate optical scan validation & duplicate prevention:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (eventTickets.isEmpty()) {
                            Text(
                                text = "No registered tickets for this event yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                eventTickets.forEach { ticket ->
                                    val isAlreadyIn = ticket.status == TicketStatus.CHECKED_IN
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isAlreadyIn) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isAlreadyIn) Color.LightGray else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = ticket.attendeeName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "${ticket.attendeeStudentId} • ${ticket.ticketToken}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (isAlreadyIn) {
                                                    Text(
                                                        text = "Status: Already Checked In",
                                                        color = Color(0xFF059669),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    kotlinx.coroutines.runBlocking {
                                                        performCheckIn(ticket.ticketToken)
                                                    }
                                                },
                                                modifier = Modifier.testTag("btn_sim_scan_${ticket.id}"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isAlreadyIn) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                                                ),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.QrCodeScanner,
                                                    contentDescription = "Scan",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isAlreadyIn) "Retest Scan" else "Simulate Scan",
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
