package com.example.ui.screens.profile

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.EmailLogEntity
import com.example.data.local.entities.TicketStatus
import com.example.data.repository.CampusConnectRepository
import com.example.ui.components.QrCodeCanvas
import com.example.ui.email.EmailLogDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    repository: CampusConnectRepository,
    onOpenPersonaSwitcher: () -> Unit,
    onOpenSimulationLab: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()
    val userTickets by (currentUser?.let { repository.getUserTickets(it.id) }
        ?: repository.getUserTickets(0)).collectAsState(initial = emptyList())
    val userEmails by (currentUser?.let { repository.getUserEmails(it.id) }
        ?: repository.getUserEmails(0)).collectAsState(initial = emptyList())

    var viewingEmail by remember { mutableStateOf<EmailLogEntity?>(null) }
    var showEmptyDbDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showIdQrDialog by remember { mutableStateOf(false) }

    val user = currentUser
    val checkedInCount = userTickets.count { it.status == TicketStatus.CHECKED_IN }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campus ID & Student Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenPersonaSwitcher, modifier = Modifier.testTag("btn_switch_persona_profile")) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (user == null) {
                Text("No user signed in.")
            } else {
                // Official Student ID Card Component
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_id_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CAMPUSCONNECT UNIVERSITY",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "OFFICIAL IDENTITY PASS",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { showIdQrDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "Identity QR",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(66.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString(""),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = user.fullName,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Register No: ${user.studentId}",
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "${user.role.name} • ${user.semester}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Department", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                Text(user.department, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Login Email", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                Text(user.email, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Current Semester", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                Text(user.semester, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Phone", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                Text(if (user.phone.isNotBlank()) user.phone else "Not set", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons inside ID card: Edit Profile & Extract Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showEditProfileDialog = true },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f).testTag("btn_edit_profile")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val idSummary = """
                                        🎓 CAMPUS IDENTITY RECORD
                                        Name: ${user.fullName}
                                        Student ID / Register No: ${user.studentId}
                                        Login Email: ${user.email}
                                        Department: ${user.department}
                                        Semester: ${user.semester}
                                        Phone: ${user.phone}
                                        Role: ${user.role.name}
                                    """.trimIndent()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Student Identity - ${user.fullName}")
                                        putExtra(Intent.EXTRA_TEXT, idSummary)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Extracted Student Info"))
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f).testTag("btn_extract_student_info")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extract Info", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Anti-Proxy Gate Security Notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF059669).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF059669))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Anti-Proxy Gate Protection Active",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dynamic session nonces and token encryption prevent QR screenshot spoofing, ticket reselling, and proxy attendance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Activity Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Registered Passes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("${userTickets.size}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Events Attended", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("$checkedInCount", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }
                    }
                }

                // Switch Persona Action
                Button(
                    onClick = onOpenPersonaSwitcher,
                    modifier = Modifier.fillMaxWidth().testTag("btn_switch_persona_big")
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Switch Active Persona / Role")
                }

                // Recent Notifications / Confirmation Emails
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Campus Email Delivery Log (${userEmails.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Official digital slips sent to ${user.email}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (userEmails.isEmpty()) {
                        Text(
                            text = "No confirmation emails logged yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        userEmails.forEach { email ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_item_${email.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(email.subject, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(email.recipientEmail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { viewingEmail = email },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("View Slip", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Failure Simulation Lab & System Resilience
                OutlinedButton(
                    onClick = onOpenSimulationLab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_profile_open_simulation_lab"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("💥 Real-World Failure Simulation Lab", fontWeight = FontWeight.Bold)
                }

                // Empty Database / Real World Clean Slate
                OutlinedButton(
                    onClick = { showEmptyDbDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_empty_database"),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🗑️ Empty Database (Clean Real-World Slate)", fontWeight = FontWeight.Bold)
                }

                // Sign Out Button
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.logout()
                            onLogout()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_profile_logout"),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of Campus Account", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // Edit Profile Dialog
        if (showEditProfileDialog && user != null) {
            var editName by remember { mutableStateOf(user.fullName) }
            var editStudentId by remember { mutableStateOf(user.studentId) }
            var editDept by remember { mutableStateOf(user.department) }
            var editSem by remember { mutableStateOf(user.semester) }
            var editPhone by remember { mutableStateOf(user.phone) }

            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                title = { Text("Update Student Profile", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Full Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_edit_name")
                        )
                        OutlinedTextField(
                            value = editStudentId,
                            onValueChange = { editStudentId = it },
                            label = { Text("Register No / Student ID *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_edit_student_id")
                        )
                        OutlinedTextField(
                            value = editDept,
                            onValueChange = { editDept = it },
                            label = { Text("Department *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_edit_dept")
                        )
                        OutlinedTextField(
                            value = editSem,
                            onValueChange = { editSem = it },
                            label = { Text("Semester (e.g. Semester 6) *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_edit_sem")
                        )
                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_edit_phone")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                repository.updateStudentProfile(
                                    userId = user.id,
                                    fullName = editName.trim(),
                                    studentId = editStudentId.trim(),
                                    department = editDept.trim(),
                                    semester = editSem.trim(),
                                    phone = editPhone.trim()
                                )
                                showEditProfileDialog = false
                            }
                        },
                        modifier = Modifier.testTag("btn_save_profile_dialog")
                    ) {
                        Text("Save Profile")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Identity QR Dialog
        if (showIdQrDialog && user != null) {
            val idPayload = "CAMPUS_ID:${user.id}:${user.studentId}:${user.fullName}:${user.department}:${user.semester}"
            AlertDialog(
                onDismissRequest = { showIdQrDialog = false },
                title = { Text("Digital Student ID Pass", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.size(200.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                QrCodeCanvas(
                                    payload = idPayload,
                                    modifier = Modifier.size(180.dp)
                                )
                            }
                        }

                        Text(user.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${user.studentId} • ${user.department}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Encrypted Campus ID: $idPayload", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showIdQrDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showEmptyDbDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyDbDialog = false },
                title = { Text("Empty Entire Database?") },
                text = {
                    Text("This will purge all users, events, tickets, registrations, and logs to provide a pristine, 100% empty slate for real-world production usage. You will be returned to the registration/login screen.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showEmptyDbDialog = false
                            coroutineScope.launch {
                                repository.emptyDatabase()
                                onLogout()
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Empty Database Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyDbDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (viewingEmail != null) {
            EmailLogDialog(
                email = viewingEmail,
                onDismiss = { viewingEmail = null }
            )
        }
    }
}
