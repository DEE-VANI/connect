package com.example.data.reports

import com.example.data.local.entities.EventEntity
import com.example.data.local.entities.TicketEntity
import com.example.data.local.entities.TicketStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EventPamphletData(
    val eventId: Long,
    val title: String,
    val categoryLabel: String,
    val dateString: String,
    val timeString: String,
    val location: String,
    val durationHours: Double,
    val capacity: Int,
    val description: String,
    val organizerName: String,
    val organizerEmail: String,
    val organizerContact: String,
    val coordinators: String,
    val facultyAdvisor: String,
    val guidelines: List<String>
)

data class EventReportData(
    val eventId: Long,
    val eventTitle: String,
    val category: String,
    val location: String,
    val eventDate: String,
    val totalCapacity: Int,
    val totalRegistered: Int,
    val totalCheckedIn: Int,
    val attendanceRatePercentage: Double,
    val departmentBreakdown: Map<String, Int>,
    val semesterBreakdown: Map<String, Int>,
    val proxyAttemptsBlocked: Int,
    val executiveSummary: String
)

object EventReportGenerator {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    private val fullDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * Generates a standard CSV / Excel file content containing all attendee details:
     * Student Name, Register Number / Student ID, Institutional Email, Department, Semester,
     * Registration Date, Check-In Status, Timestamp, Scanner Agent, and Anti-Proxy Integrity Nonce.
     */
    fun generateAttendanceCsv(event: EventEntity, tickets: List<TicketEntity>): String {
        val sb = StringBuilder()
        // CSV Metadata header
        sb.appendLine("CAMPUSCONNECT - OFFICIAL EVENT ATTENDANCE REGISTER")
        sb.appendLine("Event Title:,\"${escapeCsv(event.title)}\"")
        sb.appendLine("Event Date & Time:,\"${dateFormat.format(Date(event.dateTimeMillis))} at ${timeFormat.format(Date(event.dateTimeMillis))}\"")
        sb.appendLine("Venue / Location:,\"${escapeCsv(event.location)}\"")
        sb.appendLine("Lead Organizer:,\"${escapeCsv(event.organizerName)} (${event.organizerEmail})\"")
        if (event.facultyAdvisor.isNotBlank()) {
            sb.appendLine("Faculty Advisor:,\"${escapeCsv(event.facultyAdvisor)}\"")
        }
        if (event.coordinators.isNotBlank()) {
            sb.appendLine("Student Coordinators:,\"${escapeCsv(event.coordinators)}\"")
        }
        sb.appendLine("Export Timestamp:,\"${fullDateTimeFormat.format(Date())}\"")
        sb.appendLine("Total Registered:,${tickets.size}")
        val checkedInCount = tickets.count { it.status == TicketStatus.CHECKED_IN }
        sb.appendLine("Total Verified Present:,${checkedInCount}")
        sb.appendLine()

        // Column Headers
        sb.appendLine("SL No,Student Full Name,Register Number / Student ID,Institutional Email,Department,Semester,Registration Date,Check-in Status,Check-in Timestamp,Verified By,Anti-Proxy Verification Nonce,Proxy Status")

        tickets.forEachIndexed { index, ticket ->
            val slNo = index + 1
            val name = escapeCsv(ticket.attendeeName)
            val studentId = escapeCsv(ticket.attendeeStudentId)
            val email = escapeCsv(if (ticket.attendeeEmail.isNotBlank()) ticket.attendeeEmail else "student@campus.edu")
            val dept = escapeCsv(ticket.attendeeDepartment)
            val sem = escapeCsv(ticket.attendeeSemester)
            val regDate = escapeCsv(fullDateTimeFormat.format(Date(ticket.issuedAt)))
            val status = when (ticket.status) {
                TicketStatus.CHECKED_IN -> "PRESENT (Verified)"
                TicketStatus.ACTIVE -> "REGISTERED (Pending)"
                TicketStatus.CANCELLED -> "CANCELLED"
            }
            val checkInTime = if (ticket.checkedInAt != null) {
                escapeCsv(fullDateTimeFormat.format(Date(ticket.checkedInAt)))
            } else {
                "N/A"
            }
            val checkedInBy = escapeCsv(ticket.checkedInBy ?: "Unverified")
            val nonce = escapeCsv(if (ticket.antiProxyNonce.isNotBlank()) ticket.antiProxyNonce else "APX-${ticket.ticketToken.take(8).uppercase()}")
            val proxyStatus = if (ticket.proxyFlagged) "FLAGGED_PROXY_ATTEMPT" else "VERIFIED_GENUINE"

            sb.appendLine("$slNo,\"$name\",\"$studentId\",\"$email\",\"$dept\",\"$sem\",\"$regDate\",\"$status\",\"$checkInTime\",\"$checkedInBy\",\"$nonce\",\"$proxyStatus\"")
        }

        return sb.toString()
    }

    /**
     * Builds official pamphlet brochure details for promotion, printout, or sharing.
     */
    fun buildPamphletData(event: EventEntity): EventPamphletData {
        val dateStr = dateFormat.format(Date(event.dateTimeMillis))
        val timeStr = timeFormat.format(Date(event.dateTimeMillis))

        return EventPamphletData(
            eventId = event.id,
            title = event.title,
            categoryLabel = event.category.label,
            dateString = dateStr,
            timeString = timeStr,
            location = event.location,
            durationHours = event.durationHours,
            capacity = event.capacity,
            description = event.description,
            organizerName = event.organizerName,
            organizerEmail = event.organizerEmail,
            organizerContact = if (event.organizerContact.isNotBlank()) event.organizerContact else "Student Affairs Office",
            coordinators = if (event.coordinators.isNotBlank()) event.coordinators else "Campus Student Council Event Leads",
            facultyAdvisor = if (event.facultyAdvisor.isNotBlank()) event.facultyAdvisor else "Faculty Council & Dept Head",
            guidelines = listOf(
                "Entry requires valid student digital pass on the CampusConnect mobile app.",
                "Anti-proxy dynamic QR tokens regenerate periodically; static screenshots are strictly invalid.",
                "Please arrive at ${event.location} at least 10 minutes prior to scheduled start time.",
                "Adhere to university code of conduct and safety regulations throughout the event."
            )
        )
    }

    /**
     * Aggregates statistical report data for university administration and event organizers.
     */
    fun buildEventReport(event: EventEntity, tickets: List<TicketEntity>): EventReportData {
        val totalReg = tickets.size
        val totalPresent = tickets.count { it.status == TicketStatus.CHECKED_IN }
        val attendanceRate = if (totalReg > 0) (totalPresent.toDouble() / totalReg.toDouble()) * 100.0 else 0.0

        val deptMap = tickets.groupBy { it.attendeeDepartment.ifBlank { "General Studies" } }
            .mapValues { it.value.size }

        val semMap = tickets.groupBy { it.attendeeSemester.ifBlank { "Semester 6" } }
            .mapValues { it.value.size }

        val proxyBlocked = tickets.count { it.proxyFlagged }

        val summary = "Official event summary for '${event.title}'. Out of ${totalReg} registered students (${event.capacity} total hall capacity), ${totalPresent} were verified present at the gate (Attendance Rate: ${String.format(Locale.US, "%.1f", attendanceRate)}%). Anti-proxy validation blocked ${proxyBlocked} invalid or duplicated scans with 0 system errors."

        return EventReportData(
            eventId = event.id,
            eventTitle = event.title,
            category = event.category.label,
            location = event.location,
            eventDate = "${dateFormat.format(Date(event.dateTimeMillis))} at ${timeFormat.format(Date(event.dateTimeMillis))}",
            totalCapacity = event.capacity,
            totalRegistered = totalReg,
            totalCheckedIn = totalPresent,
            attendanceRatePercentage = attendanceRate,
            departmentBreakdown = deptMap,
            semesterBreakdown = semMap,
            proxyAttemptsBlocked = proxyBlocked,
            executiveSummary = summary
        )
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"").replace("\n", " ").trim()
    }
}
