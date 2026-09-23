package com.example.ui.screens.organizer

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.TicketEntity
import com.example.data.local.entities.TicketStatus
import com.example.data.reports.EventReportData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceSpreadsheetDialog(
    eventTitle: String,
    csvContent: String,
    tickets: List<TicketEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredTickets = remember(tickets, searchQuery) {
        if (searchQuery.isBlank()) tickets else {
            tickets.filter {
                it.attendeeName.contains(searchQuery, ignoreCase = true) ||
                        it.attendeeStudentId.contains(searchQuery, ignoreCase = true) ||
                        it.attendeeEmail.contains(searchQuery, ignoreCase = true) ||
                        it.attendeeDepartment.contains(searchQuery, ignoreCase = true) ||
                        it.attendeeSemester.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(680.dp)
                .padding(8.dp)
                .testTag("dialog_attendance_spreadsheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = null,
                            tint = Color(0xFF107C41), // Excel Green
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Official Attendance Register (Excel / CSV)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(eventTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_spreadsheet")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Search & Filter Row
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, student ID, department, semester...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_search_spreadsheet")
                )

                // Spreadsheet Table
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // Table Header Row
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF107C41))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableHeaderCell("SL", 40.dp)
                            TableHeaderCell("Student Name", 140.dp)
                            TableHeaderCell("Register No / ID", 120.dp)
                            TableHeaderCell("Institutional Email", 160.dp)
                            TableHeaderCell("Department", 140.dp)
                            TableHeaderCell("Semester", 90.dp)
                            TableHeaderCell("Attendance Status", 130.dp)
                            TableHeaderCell("Check-in Time", 120.dp)
                            TableHeaderCell("Verified By", 110.dp)
                            TableHeaderCell("Anti-Proxy Nonce", 130.dp)
                        }

                        // Table Data Rows
                        if (filteredTickets.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No attendee records found matching query", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            filteredTickets.forEachIndexed { index, ticket ->
                                val rowBg = if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                Row(
                                    modifier = Modifier
                                        .background(rowBg)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TableCell("${index + 1}", 40.dp, isMonospace = true)
                                    TableCell(ticket.attendeeName, 140.dp, isBold = true)
                                    TableCell(ticket.attendeeStudentId, 120.dp, isMonospace = true)
                                    TableCell(if (ticket.attendeeEmail.isNotBlank()) ticket.attendeeEmail else "student@campus.edu", 160.dp)
                                    TableCell(ticket.attendeeDepartment, 140.dp)
                                    TableCell(ticket.attendeeSemester, 90.dp)
                                    TableStatusCell(ticket.status, 130.dp)
                                    val timeStr = if (ticket.checkedInAt != null) {
                                        SimpleDateFormat("hh:mm a", Locale.US).format(Date(ticket.checkedInAt))
                                    } else "—"
                                    TableCell(timeStr, 120.dp)
                                    TableCell(ticket.checkedInBy ?: "—", 110.dp)
                                    TableCell(if (ticket.antiProxyNonce.isNotBlank()) ticket.antiProxyNonce else "APX-OK", 130.dp, isMonospace = true)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }

                // Export Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "Attendance Register - $eventTitle")
                                putExtra(Intent.EXTRA_TEXT, csvContent)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Attendance List to Excel / CSV"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_attendance_excel")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share / Export CSV for Excel", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isBold: Boolean = false,
    isMonospace: Boolean = false
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun TableStatusCell(status: TicketStatus, width: androidx.compose.ui.unit.Dp) {
    val (label, bg, fg) = when (status) {
        TicketStatus.CHECKED_IN -> Triple("Verified Present", Color(0xFF059669).copy(alpha = 0.15f), Color(0xFF059669))
        TicketStatus.ACTIVE -> Triple("Registered", Color(0xFF2563EB).copy(alpha = 0.15f), Color(0xFF2563EB))
        TicketStatus.CANCELLED -> Triple("Cancelled", Color(0xFFDC2626).copy(alpha = 0.15f), Color(0xFFDC2626))
    }

    Box(
        modifier = Modifier
            .width(width)
            .padding(end = 8.dp)
    ) {
        Surface(shape = RoundedCornerShape(4.dp), color = bg) {
            Text(
                text = label,
                color = fg,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun EventExecutiveReportDialog(
    report: EventReportData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_event_executive_report")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Official Event Report", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(report.eventDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_executive_report")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // High-level KPI Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportKpiCard("Registered", "${report.totalRegistered}", "Limit: ${report.totalCapacity}", MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
                    ReportKpiCard("Verified Present", "${report.totalCheckedIn}", "${String.format(Locale.US, "%.1f", report.attendanceRatePercentage)}% turn-out", Color(0xFF059669).copy(alpha = 0.18f), Modifier.weight(1f))
                    ReportKpiCard("Proxies Blocked", "${report.proxyAttemptsBlocked}", "100% prevented", MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
                }

                // Executive Summary
                Text("Executive Summary for University Affairs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = report.executiveSummary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 20.sp
                    )
                }

                // Department Breakdown
                Text("Department Participation Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((dept, count) in report.departmentBreakdown) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dept, style = MaterialTheme.typography.bodySmall)
                            Text("$count Students", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Semester Breakdown
                Text("Semester Distribution", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((sem, count) in report.semesterBreakdown) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sem, style = MaterialTheme.typography.bodySmall)
                            Text("$count Students", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val shareText = """
                            📊 OFFICIAL CAMPUS EVENT REPORT
                            Event: ${report.eventTitle}
                            Date: ${report.eventDate}
                            Venue: ${report.location}
                            
                            📈 KEY METRICS:
                            • Total Capacity: ${report.totalCapacity}
                            • Total Registrations: ${report.totalRegistered}
                            • Verified Attendees Present: ${report.totalCheckedIn} (${String.format(Locale.US, "%.1f", report.attendanceRatePercentage)}%)
                            • Proxy Attempts Blocked: ${report.proxyAttemptsBlocked}
                            
                            📝 SUMMARY:
                            ${report.executiveSummary}
                        """.trimIndent()

                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Official Event Report - ${report.eventTitle}")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Event Report"))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_share_event_report")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Official Report", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReportKpiCard(
    title: String,
    value: String,
    subtitle: String,
    containerColor: Color,
    modifier: Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
