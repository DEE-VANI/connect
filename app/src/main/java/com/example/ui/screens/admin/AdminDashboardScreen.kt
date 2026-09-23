package com.example.ui.screens.admin

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.EventEntity
import com.example.data.local.entities.EventStatus
import com.example.data.local.entities.UserEntity
import com.example.data.local.entities.UserRole
import com.example.data.repository.CampusConnectRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    repository: CampusConnectRepository,
    onOpenPersonaSwitcher: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser by repository.currentUser.collectAsState()

    val allEvents by repository.allEvents.collectAsState(initial = emptyList())
    val allUsers by repository.allUsers.collectAsState(initial = emptyList())
    val recentAuditLogs by repository.getRecentAuditLogs().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(0) } // 0: Moderation, 1: Users, 2: Audit Log

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Campus Administration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = currentUser?.let { "${it.fullName} • ${it.role.name}" } ?: "Admin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPersonaSwitcher, modifier = Modifier.testTag("btn_switch_persona_admin")) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Persona", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Admin Note Banner if not admin
            if (currentUser?.role != UserRole.ADMIN) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                ) {
                    Text(
                        text = "💡 Note: To test full admin features (moderation, role modification, suspension), switch to Dean Marcus Vance in the top-right menu.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E)
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth().testTag("tabs_admin")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Events (${allEvents.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Users (${allUsers.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Audit Trail", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Event Moderation List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allEvents, key = { it.id }) { event ->
                            AdminEventModerationCard(
                                event = event,
                                repository = repository,
                                onActionCompleted = { msg ->
                                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        }
                    }
                }

                1 -> {
                    // User Directory & Role Promotion
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allUsers, key = { it.id }) { user ->
                            AdminUserCard(
                                user = user,
                                repository = repository,
                                onActionCompleted = { msg ->
                                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        }
                    }
                }

                2 -> {
                    // Audit Trail
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recentAuditLogs, key = { it.id }) { log ->
                            AdminAuditLogCard(log = log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminEventModerationCard(
    event: EventEntity,
    repository: CampusConnectRepository,
    onActionCompleted: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth().testTag("admin_event_card_${event.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (event.status) {
                        EventStatus.FEATURED -> Color(0xFFFEF3C7)
                        EventStatus.ACTIVE -> Color(0xFFD1FAE5)
                        EventStatus.ARCHIVED -> Color(0xFFE2E8F0)
                        EventStatus.CANCELLED -> Color(0xFFFEE2E2)
                    }
                ) {
                    Text(
                        text = event.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (event.status) {
                            EventStatus.FEATURED -> Color(0xFFB45309)
                            EventStatus.ACTIVE -> Color(0xFF047857)
                            EventStatus.ARCHIVED -> Color(0xFF475569)
                            EventStatus.CANCELLED -> Color(0xFFB91C1C)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Capacity: ${event.capacity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = "Organizer: ${event.organizerName} (${event.organizerEmail})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (event.status != EventStatus.FEATURED) {
                    OutlinedButton(
                        onClick = {
                            currentUser?.let { admin ->
                                coroutineScope.launch {
                                    repository.updateEventStatus(event.id, EventStatus.FEATURED, admin)
                                    onActionCompleted("Event featured on campus homepage.")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_feature_event_${event.id}")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Feature", fontSize = 11.sp)
                    }
                }

                if (event.status != EventStatus.ARCHIVED) {
                    OutlinedButton(
                        onClick = {
                            currentUser?.let { admin ->
                                coroutineScope.launch {
                                    repository.updateEventStatus(event.id, EventStatus.ARCHIVED, admin)
                                    onActionCompleted("Event moved to archive.")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_archive_event_${event.id}")
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Archive", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(
    user: UserEntity,
    repository: CampusConnectRepository,
    onActionCompleted: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth().testTag("admin_user_card_${user.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (user.role) {
                                    UserRole.STUDENT -> Color(0xFF2563EB)
                                    UserRole.ORGANIZER -> Color(0xFF0D9488)
                                    UserRole.ADMIN -> Color(0xFF4338CA)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = user.fullName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = user.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = "${user.studentId} • ${user.email}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = user.role.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "${user.department} • ${user.yearOfStudy}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(10.dp))

            // Promote Role buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (user.role == UserRole.STUDENT) {
                    OutlinedButton(
                        onClick = {
                            currentUser?.let { admin ->
                                coroutineScope.launch {
                                    repository.updateUserRole(user.id, UserRole.ORGANIZER, admin)
                                    onActionCompleted("${user.fullName} promoted to Organizer.")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_promote_user_${user.id}")
                    ) {
                        Text("Promote to Organizer", fontSize = 11.sp)
                    }
                } else if (user.role == UserRole.ORGANIZER) {
                    OutlinedButton(
                        onClick = {
                            currentUser?.let { admin ->
                                coroutineScope.launch {
                                    repository.updateUserRole(user.id, UserRole.STUDENT, admin)
                                    onActionCompleted("${user.fullName} role set to Student.")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Demote to Student", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAuditLogCard(log: AuditLogEntity) {
    val sdf = SimpleDateFormat("h:mm:ss a • MMM d", Locale.getDefault())
    val timeStr = sdf.format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (log.action) {
                        "CHECK_IN" -> Color(0xFFD1FAE5)
                        "REGISTER_EVENT" -> Color(0xFFDBEAFE)
                        "CANCEL_REGISTRATION" -> Color(0xFFFEE2E2)
                        "CREATE_EVENT" -> Color(0xFFFEF3C7)
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = log.action,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (log.action) {
                            "CHECK_IN" -> Color(0xFF047857)
                            "REGISTER_EVENT" -> Color(0xFF1D4ED8)
                            "CANCEL_REGISTRATION" -> Color(0xFFB91C1C)
                            "CREATE_EVENT" -> Color(0xFFB45309)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = timeStr,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${log.actorName} (${log.actorRole}): ${log.targetResource}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            if (log.details.isNotBlank()) {
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
