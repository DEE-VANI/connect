package com.example.ui.screens.organizer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.EventEntity
import com.example.data.local.entities.TicketEntity
import com.example.data.local.entities.TicketStatus
import com.example.data.local.entities.UserRole
import com.example.data.reports.EventPamphletData
import com.example.data.reports.EventReportData
import com.example.data.repository.CampusConnectRepository
import com.example.ui.screens.discovery.EventPamphletDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerDashboardScreen(
    repository: CampusConnectRepository,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToScanner: (Long) -> Unit,
    onNavigateToAttendees: (Long) -> Unit,
    onOpenPersonaSwitcher: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()
    val allEvents by repository.allEvents.collectAsState(initial = emptyList())

    var activeSpreadsheetEvent by remember { mutableStateOf<EventEntity?>(null) }
    var activeSpreadsheetTickets by remember { mutableStateOf<List<TicketEntity>>(emptyList()) }
    var activeSpreadsheetCsv by remember { mutableStateOf("") }

    var activeReportData by remember { mutableStateOf<EventReportData?>(null) }
    var activePamphletData by remember { mutableStateOf<EventPamphletData?>(null) }

    // If currentUser is Admin or Organizer, filter events
    val managedEvents = remember(allEvents, currentUser) {
        if (currentUser?.role == UserRole.ADMIN) allEvents
        else allEvents.filter { it.organizerId == currentUser?.id || currentUser?.role == UserRole.ORGANIZER }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Organizer Command Center", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = currentUser?.let { "${it.fullName} • ${it.department}" } ?: "Organizer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPersonaSwitcher, modifier = Modifier.testTag("btn_switch_persona_organizer")) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Persona", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateEvent,
                modifier = Modifier.testTag("fab_create_event")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Event")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Event", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Metrics Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Events Managed",
                        value = "${managedEvents.size}",
                        icon = Icons.Default.Event,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Open Campus",
                        value = "All Allowed",
                        icon = Icons.Default.CalendarMonth,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Managed Events",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${managedEvents.size} total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (managedEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No events managed yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onNavigateToCreateEvent, modifier = Modifier.testTag("btn_create_first_event")) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Your First Event")
                            }
                        }
                    }
                }
            } else {
                items(managedEvents, key = { it.id }) { event ->
                    OrganizerEventCard(
                        event = event,
                        repository = repository,
                        onOpenScanner = { onNavigateToScanner(event.id) },
                        onOpenAttendees = { onNavigateToAttendees(event.id) },
                        onOpenSpreadsheet = {
                            coroutineScope.launch {
                                val csv = repository.getAttendanceCsv(event.id).getOrDefault("")
                                val tList = repository.getEventTickets(event.id).first()
                                activeSpreadsheetTickets = tList
                                activeSpreadsheetCsv = csv
                                activeSpreadsheetEvent = event
                            }
                        },
                        onOpenReport = {
                            coroutineScope.launch {
                                activeReportData = repository.getEventReport(event.id).getOrNull()
                            }
                        },
                        onOpenPamphlet = {
                            coroutineScope.launch {
                                activePamphletData = repository.getEventPamphlet(event.id).getOrNull()
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Attendance Spreadsheet Viewer Dialog
    if (activeSpreadsheetEvent != null) {
        AttendanceSpreadsheetDialog(
            eventTitle = activeSpreadsheetEvent!!.title,
            csvContent = activeSpreadsheetCsv,
            tickets = activeSpreadsheetTickets,
            onDismiss = { activeSpreadsheetEvent = null }
        )
    }

    // Official Event Executive Report Dialog
    if (activeReportData != null) {
        EventExecutiveReportDialog(
            report = activeReportData!!,
            onDismiss = { activeReportData = null }
        )
    }

    // Official Event Pamphlet Dialog
    if (activePamphletData != null) {
        EventPamphletDialog(
            pamphlet = activePamphletData!!,
            onDismiss = { activePamphletData = null }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OrganizerEventCard(
    event: EventEntity,
    repository: CampusConnectRepository,
    onOpenScanner: () -> Unit,
    onOpenAttendees: () -> Unit,
    onOpenSpreadsheet: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenPamphlet: () -> Unit
) {
    val activeRegistrations by repository.getActiveRegistrationCount(event.id).collectAsState(initial = 0)
    val eventTickets by repository.getEventTickets(event.id).collectAsState(initial = emptyList())
    val checkedInCount = eventTickets.count { it.status == TicketStatus.CHECKED_IN }

    val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(event.dateTimeMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("organizer_event_card_${event.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = event.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "$checkedInCount Check-ins",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669)
                )
            }

            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$dateStr • ${event.location}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Coordinators line if present
            if (event.coordinators.isNotBlank()) {
                Text(
                    text = "Coordinators: ${event.coordinators}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Capacity & Check-in meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Booked: $activeRegistrations / ${event.capacity} seats",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (event.capacity > 0) "${(activeRegistrations * 100) / event.capacity}%" else "0%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { (activeRegistrations.toFloat() / event.capacity).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Primary Actions: Scanner & Roster
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenScanner,
                    modifier = Modifier.weight(1f).testTag("btn_scanner_event_${event.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("QR Scanner", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenAttendees,
                    modifier = Modifier.weight(1f).testTag("btn_roster_event_${event.id}")
                ) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Roster (${eventTickets.size})", fontSize = 12.sp)
                }
            }

            // Secondary Actions: Excel, Report, Pamphlet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onOpenSpreadsheet,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("btn_excel_event_${event.id}")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenReport,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("btn_report_event_${event.id}")
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenPamphlet,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("btn_pamphlet_event_${event.id}")
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pamphlet", fontSize = 11.sp)
                }
            }
        }
    }
}
