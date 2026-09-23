package com.example.ui.screens.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.EmailLogEntity
import com.example.data.local.entities.TicketEntity
import com.example.data.local.entities.TicketStatus
import com.example.data.repository.CampusConnectRepository
import com.example.ui.components.TicketPassCard
import com.example.ui.email.EmailLogDialog
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTicketsScreen(
    repository: CampusConnectRepository,
    onNavigateToDiscovery: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser by repository.currentUser.collectAsState()

    val tickets by (currentUser?.let { repository.getUserTickets(it.id) }
        ?: repository.getUserTickets(0)).collectAsState(initial = emptyList())

    val userEmails by (currentUser?.let { repository.getUserEmails(it.id) }
        ?: repository.getUserEmails(0)).collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(0) } // 0: Active, 1: Past / Checked-In
    var viewingEmailLog by remember { mutableStateOf<EmailLogEntity?>(null) }
    var ticketToCancel by remember { mutableStateOf<TicketEntity?>(null) }

    val activeTickets = remember(tickets) {
        tickets.filter { it.status == TicketStatus.ACTIVE }
    }
    val pastTickets = remember(tickets) {
        tickets.filter { it.status != TicketStatus.ACTIVE }
    }

    val displayTickets = if (selectedTab == 0) activeTickets else pastTickets

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Campus Tickets", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth().testTag("tabs_my_tickets")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active Passes (${activeTickets.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Past / Used (${pastTickets.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (displayTickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (selectedTab == 0) "No active tickets found." else "No past or checked-in tickets.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Browse campus events and confirm your RSVP to get a digital QR ticket pass.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToDiscovery,
                            modifier = Modifier.testTag("btn_explore_from_empty_tickets")
                        ) {
                            Text("Browse Events")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayTickets, key = { it.id }) { ticket ->
                        TicketPassCard(
                            ticket = ticket,
                            onViewEmail = {
                                val matchingEmail = userEmails.find {
                                    it.subject.contains(ticket.eventTitle, ignoreCase = true) ||
                                            it.body.contains(ticket.ticketToken, ignoreCase = true)
                                } ?: userEmails.firstOrNull()

                                if (matchingEmail != null) {
                                    viewingEmailLog = matchingEmail
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Digital confirmation email registered on campus server.")
                                    }
                                }
                            },
                            onCancelRegistration = {
                                ticketToCancel = ticket
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }

        // Email View Dialog
        if (viewingEmailLog != null) {
            EmailLogDialog(
                email = viewingEmailLog,
                onDismiss = { viewingEmailLog = null }
            )
        }

        // Cancel Registration Dialog
        if (ticketToCancel != null) {
            val targetTicket = ticketToCancel!!
            AlertDialog(
                onDismissRequest = { ticketToCancel = null },
                title = { Text("Cancel Event Registration?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to cancel your registration for \"${targetTicket.eventTitle}\"? " +
                                "This will invalidate ticket ${targetTicket.ticketToken} and release your reserved seat for other students."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            currentUser?.let { user ->
                                coroutineScope.launch {
                                    val result = repository.cancelRegistration(targetTicket.id, user)
                                    ticketToCancel = null
                                    if (result.isSuccess) {
                                        snackbarHostState.showSnackbar("Registration cancelled and seat released.")
                                    } else {
                                        snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Cancellation failed.")
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("btn_confirm_cancel_ticket")
                    ) {
                        Text("Confirm Cancellation")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { ticketToCancel = null }) {
                        Text("Keep Ticket")
                    }
                }
            )
        }
    }
}
