package com.example.ui.screens.organizer

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.TicketEntity
import com.example.data.local.entities.TicketStatus
import com.example.data.reports.EventPamphletData
import com.example.data.reports.EventReportData
import com.example.data.repository.CampusConnectRepository
import com.example.ui.screens.discovery.EventPamphletDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventAttendeesScreen(
    eventId: Long,
    repository: CampusConnectRepository,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()
    val event by repository.getEventById(eventId).collectAsState(initial = null)
    val tickets by repository.getEventTickets(eventId).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("ALL") } // "ALL", "CHECKED_IN", "PENDING"

    var showSpreadsheetDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showPamphletDialog by remember { mutableStateOf(false) }

    var csvData by remember { mutableStateOf("") }
    var reportData by remember { mutableStateOf<EventReportData?>(null) }
    var pamphletData by remember { mutableStateOf<EventPamphletData?>(null) }

    val filteredTickets = remember(tickets, searchQuery, filterStatus) {
        tickets.filter { ticket ->
            val matchesQuery = ticket.attendeeName.contains(searchQuery, ignoreCase = true) ||
                    ticket.attendeeStudentId.contains(searchQuery, ignoreCase = true) ||
                    ticket.attendeeEmail.contains(searchQuery, ignoreCase = true) ||
                    ticket.attendeeDepartment.contains(searchQuery, ignoreCase = true) ||
                    ticket.attendeeSemester.contains(searchQuery, ignoreCase = true) ||
                    ticket.ticketToken.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterStatus) {
                "CHECKED_IN" -> ticket.status == TicketStatus.CHECKED_IN
                "PENDING" -> ticket.status == TicketStatus.ACTIVE
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    val checkedInCount = tickets.count { it.status == TicketStatus.CHECKED_IN }
    val totalCount = tickets.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Attendee Roster & Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(event?.title ?: "Event", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_attendees")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                csvData = repository.getAttendanceCsv(eventId).getOrDefault("")
                                showSpreadsheetDialog = true
                            }
                        },
                        modifier = Modifier.testTag("btn_top_open_excel")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = "Excel Spreadsheet", tint = Color(0xFF107C41))
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                reportData = repository.getEventReport(eventId).getOrNull()
                                showReportDialog = true
                            }
                        },
                        modifier = Modifier.testTag("btn_top_open_report")
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = "Event Report", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pamphletData = repository.getEventPamphlet(eventId).getOrNull()
                                showPamphletDialog = true
                            }
                        },
                        modifier = Modifier.testTag("btn_top_open_pamphlet")
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Pamphlet", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToScanner(eventId) },
                modifier = Modifier.testTag("fab_open_scanner_from_roster")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scanner")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QR Scanner", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ATTENDANCE METRICS & ROSTER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$checkedInCount / $totalCount Verified Present",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = if (totalCount > 0) "${(checkedInCount * 100) / totalCount}% Verified Rate" else "0%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Action Buttons: Excel, Report, Pamphlet
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
                                modifier = Modifier.weight(1f).testTag("btn_roster_excel_sheet")
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Excel List", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        reportData = repository.getEventReport(eventId).getOrNull()
                                        showReportDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f).testTag("btn_roster_event_report")
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        pamphletData = repository.getEventPamphlet(eventId).getOrNull()
                                        showPamphletDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f).testTag("btn_roster_pamphlet")
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pamphlet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, student ID, department, sem...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_search_attendees")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filterStatus == "ALL",
                            onClick = { filterStatus = "ALL" },
                            label = { Text("All ($totalCount)") },
                            modifier = Modifier.testTag("chip_filter_all")
                        )
                        FilterChip(
                            selected = filterStatus == "CHECKED_IN",
                            onClick = { filterStatus = "CHECKED_IN" },
                            label = { Text("Present ($checkedInCount)") },
                            modifier = Modifier.testTag("chip_filter_checked_in")
                        )
                        FilterChip(
                            selected = filterStatus == "PENDING",
                            onClick = { filterStatus = "PENDING" },
                            label = { Text("Pending (${totalCount - checkedInCount})") },
                            modifier = Modifier.testTag("chip_filter_pending")
                        )
                    }
                }
            }

            // Attendee List
            if (filteredTickets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No attendees registered yet." else "No attendees match your search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredTickets, key = { it.id }) { ticket ->
                    val isCheckedIn = ticket.status == TicketStatus.CHECKED_IN
                    val sdf = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attendee_card_${ticket.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCheckedIn) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCheckedIn) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isCheckedIn) Color(0xFF059669) else MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ticket.attendeeName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = ticket.attendeeName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ID: ${ticket.attendeeStudentId} • ${ticket.attendeeDepartment} • ${ticket.attendeeSemester}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (ticket.attendeeEmail.isNotBlank()) {
                                        Text(
                                            text = ticket.attendeeEmail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF059669))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Nonce: ${ticket.antiProxyNonce}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    if (isCheckedIn && ticket.checkedInAt != null) {
                                        Text(
                                            text = "Checked in: ${sdf.format(Date(ticket.checkedInAt))}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Manual Toggle Check-In Button
                            Button(
                                onClick = {
                                    currentUser?.let { org ->
                                        coroutineScope.launch {
                                            repository.toggleManualCheckIn(ticket.id, org)
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("btn_toggle_checkin_${ticket.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCheckedIn) Color(0xFFE2E8F0) else Color(0xFF059669),
                                    contentColor = if (isCheckedIn) Color(0xFF334155) else Color.White
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                if (isCheckedIn) {
                                    Icon(Icons.Default.Check, contentDescription = "Checked", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("In", fontSize = 11.sp)
                                } else {
                                    Text("Check In", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Attendance Spreadsheet Viewer Dialog
    if (showSpreadsheetDialog) {
        AttendanceSpreadsheetDialog(
            eventTitle = event?.title ?: "Campus Event",
            csvContent = csvData,
            tickets = tickets,
            onDismiss = { showSpreadsheetDialog = false }
        )
    }

    // Official Event Executive Report Dialog
    if (showReportDialog && reportData != null) {
        EventExecutiveReportDialog(
            report = reportData!!,
            onDismiss = { showReportDialog = false }
        )
    }

    // Official Event Pamphlet Dialog
    if (showPamphletDialog && pamphletData != null) {
        EventPamphletDialog(
            pamphlet = pamphletData!!,
            onDismiss = { showPamphletDialog = false }
        )
    }
}
