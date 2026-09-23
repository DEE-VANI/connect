package com.example.ui.screens.discovery

import android.content.Intent
import android.provider.CalendarContract
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.TicketEntity
import com.example.data.local.entities.TicketStatus
import com.example.data.local.entities.UserRole
import com.example.data.reports.EventPamphletData
import com.example.data.reports.EventReportData
import com.example.data.repository.CampusConnectRepository
import com.example.ui.screens.organizer.AttendanceSpreadsheetDialog
import com.example.ui.screens.organizer.EventExecutiveReportDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Long,
    repository: CampusConnectRepository,
    onNavigateBack: () -> Unit,
    onNavigateToTicket: (Long) -> Unit,
    onNavigateToScanner: (Long) -> Unit,
    onNavigateToAttendees: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser by repository.currentUser.collectAsState()
    val event by repository.getEventById(eventId).collectAsState(initial = null)
    val activeCount by repository.getActiveRegistrationCount(eventId).collectAsState(initial = 0)
    val tickets by repository.getEventTickets(eventId).collectAsState(initial = emptyList())

    val userTickets by (currentUser?.let { repository.getUserTickets(it.id) }
        ?: repository.getUserTickets(0)).collectAsState(initial = emptyList())

    val existingTicket = userTickets.find {
        it.eventId == eventId && it.status != TicketStatus.CANCELLED
    }
    val isRegistered = existingTicket != null

    var showConfirmDialog by remember { mutableStateOf(false) }
    var registrationError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var showPamphletDialog by remember { mutableStateOf(false) }
    var pamphletData by remember { mutableStateOf<EventPamphletData?>(null) }

    var showSpreadsheetDialog by remember { mutableStateOf(false) }
    var csvData by remember { mutableStateOf("") }

    var showReportDialog by remember { mutableStateOf(false) }
    var reportData by remember { mutableStateOf<EventReportData?>(null) }

    val currentEvent = event

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Event Overview", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_event_detail")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Add to Device Calendar
                    IconButton(
                        onClick = {
                            if (currentEvent != null) {
                                val intent = Intent(Intent.ACTION_INSERT).apply {
                                    data = CalendarContract.Events.CONTENT_URI
                                    putExtra(CalendarContract.Events.TITLE, currentEvent.title)
                                    putExtra(CalendarContract.Events.DESCRIPTION, currentEvent.description)
                                    putExtra(CalendarContract.Events.EVENT_LOCATION, currentEvent.location)
                                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, currentEvent.dateTimeMillis)
                                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, currentEvent.endDateTimeMillis)
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.testTag("btn_add_to_calendar")
                    ) {
                        Icon(Icons.Default.Event, contentDescription = "Add to Calendar", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Open Official Pamphlet
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pamphletData = repository.getEventPamphlet(eventId).getOrNull()
                                showPamphletDialog = true
                            }
                        },
                        modifier = Modifier.testTag("btn_open_pamphlet_detail")
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Official Pamphlet", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (currentEvent == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading event information...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
            val dateStr = sdf.format(Date(currentEvent.dateTimeMillis))
            val isSoldOut = activeCount >= currentEvent.capacity
            val isOrganizerOrAdmin = currentUser?.role == UserRole.ADMIN ||
                    currentUser?.id == currentEvent.organizerId ||
                    (currentUser?.role == UserRole.ORGANIZER)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category & Status Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = currentEvent.category.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSoldOut) Color(0xFFDC2626).copy(alpha = 0.15f) else Color(0xFF059669).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isSoldOut) "SOLD OUT" else "RSVP OPEN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSoldOut) Color(0xFFDC2626) else Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Title
                Text(
                    text = currentEvent.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Date, Time & Venue Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Date & Start Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(text = dateStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(text = "${currentEvent.durationHours} Hours", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Campus Location", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(text = currentEvent.location, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Official Pamphlet & Flyer Shortcut Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Official Event Pamphlet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("University flyer, rules & guidelines", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pamphletData = repository.getEventPamphlet(eventId).getOrNull()
                                    showPamphletDialog = true
                                }
                            },
                            modifier = Modifier.testTag("btn_view_pamphlet_card")
                        ) {
                            Text("View Flyer")
                        }
                    }
                }

                // Capacity Management Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Event Capacity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "$activeCount / ${currentEvent.capacity} Seats",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSoldOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (activeCount.toFloat() / currentEvent.capacity).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (isSoldOut) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isSoldOut) "No remaining seats available. Registrations closed."
                            else "${currentEvent.capacity - activeCount} seats remaining for students.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Organizing Committee & Coordinators Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "Organizing Committee", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        // Lead Organizer
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Lead Organizer: ${currentEvent.organizerName}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(text = currentEvent.organizerEmail + if (currentEvent.organizerContact.isNotBlank()) " • ${currentEvent.organizerContact}" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Student Coordinators
                        if (currentEvent.coordinators.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Student Coordinators", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = currentEvent.coordinators, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Faculty Advisor
                        if (currentEvent.facultyAdvisor.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Faculty Advisor", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = currentEvent.facultyAdvisor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Description
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "About this Event", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = currentEvent.description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Tags
                if (currentEvent.tags.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        currentEvent.tags.split(",").forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "#${tag.trim()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Organizer Controls (if organizer, coordinator, or admin)
                if (isOrganizerOrAdmin) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Organizer Command & Reporting",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToScanner(eventId) },
                                    modifier = Modifier.weight(1f).testTag("btn_detail_open_scanner")
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scanner", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { onNavigateToAttendees(eventId) },
                                    modifier = Modifier.weight(1f).testTag("btn_detail_view_roster")
                                ) {
                                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Roster", fontSize = 11.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            csvData = repository.getAttendanceCsv(eventId).getOrDefault("")
                                            showSpreadsheetDialog = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                                    modifier = Modifier.weight(1f).testTag("btn_detail_open_excel")
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Excel Register", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            reportData = repository.getEventReport(eventId).getOrNull()
                                            showReportDialog = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f).testTag("btn_detail_open_report")
                                ) {
                                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Report", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Primary Action Button (Register or View Ticket)
                Spacer(modifier = Modifier.height(10.dp))

                if (isRegistered && existingTicket != null) {
                    Button(
                        onClick = { onNavigateToTicket(existingTicket.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_view_existing_ticket"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View My QR Ticket Pass", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            if (currentUser == null) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please select a student user profile first.")
                                }
                            } else {
                                showConfirmDialog = true
                            }
                        },
                        enabled = !isSoldOut && !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_register_rsvp"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isSoldOut) "Sold Out — Registrations Closed" else "Confirm RSVP & Get Ticket",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }

            // RSVP Confirmation Dialog
            if (showConfirmDialog && currentUser != null) {
                val student = currentUser!!
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("Confirm Event RSVP", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("A digital anti-proxy QR ticket and email confirmation will be issued for:")
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "Attendee: ${student.fullName}", fontWeight = FontWeight.Bold)
                                    Text(text = "Register No / ID: ${student.studentId}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Department: ${student.department}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Semester: ${student.semester}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Institutional Login Email: ${student.email}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (registrationError != null) {
                                Text(text = registrationError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isSubmitting = true
                                coroutineScope.launch {
                                    val result = repository.registerForEvent(eventId, student)
                                    isSubmitting = false
                                    if (result.isSuccess) {
                                        showConfirmDialog = false
                                        val ticket = result.getOrNull()
                                        if (ticket != null) {
                                            snackbarHostState.showSnackbar("Registration confirmed! Ticket generated.")
                                            onNavigateToTicket(ticket.id)
                                        }
                                    } else {
                                        registrationError = result.exceptionOrNull()?.message ?: "Failed to register"
                                    }
                                }
                            },
                            modifier = Modifier.testTag("btn_confirm_registration_dialog")
                        ) {
                            Text("Confirm & Book")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Pamphlet Dialog
            if (showPamphletDialog && pamphletData != null) {
                EventPamphletDialog(
                    pamphlet = pamphletData!!,
                    onDismiss = { showPamphletDialog = false }
                )
            }

            // Attendance Spreadsheet Dialog
            if (showSpreadsheetDialog) {
                AttendanceSpreadsheetDialog(
                    eventTitle = currentEvent.title,
                    csvContent = csvData,
                    tickets = tickets,
                    onDismiss = { showSpreadsheetDialog = false }
                )
            }

            // Executive Report Dialog
            if (showReportDialog && reportData != null) {
                EventExecutiveReportDialog(
                    report = reportData!!,
                    onDismiss = { showReportDialog = false }
                )
            }
        }
    }
}
