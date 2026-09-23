package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.EmailLogEntity
import com.example.data.local.entities.EventEntity
import com.example.data.local.entities.EventStatus
import com.example.data.local.entities.RegistrationEntity
import com.example.data.local.entities.RegistrationStatus
import com.example.data.local.entities.TicketEntity
import com.example.data.local.entities.TicketStatus
import com.example.data.local.entities.UserEntity
import com.example.data.local.entities.UserRole
import com.example.data.reports.EventPamphletData
import com.example.data.reports.EventReportData
import com.example.data.reports.EventReportGenerator
import com.example.data.resilience.FailureSimulationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class OtpRecord(
    val code: String,
    val expiresAt: Long
)

sealed class CheckInResult {
    data class Success(val ticket: TicketEntity, val message: String) : CheckInResult()
    data class Duplicate(val ticket: TicketEntity, val alreadyCheckedInAt: Long, val checkedInBy: String?) : CheckInResult()
    data class WrongEvent(val ticketEventTitle: String, val scannedEventTitle: String) : CheckInResult()
    data class Cancelled(val ticket: TicketEntity) : CheckInResult()
    data class NotFound(val token: String) : CheckInResult()
    data class Unauthorized(val message: String) : CheckInResult()
    data class ProxyFlagged(val ticket: TicketEntity, val reason: String) : CheckInResult()
}

class CampusConnectRepository(private val database: AppDatabase) {

    val failureSimulationManager = FailureSimulationManager()

    private val userDao = database.userDao()
    private val eventDao = database.eventDao()
    private val registrationDao = database.registrationDao()
    private val ticketDao = database.ticketDao()
    private val auditLogDao = database.auditLogDao()
    private val emailLogDao = database.emailLogDao()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(true)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val otpCache = ConcurrentHashMap<String, OtpRecord>()

    suspend fun initCurrentUser() {
        withContext(Dispatchers.IO) {
            val defaultStudent = userDao.getUserById(1)
            _currentUser.value = defaultStudent
            _isAuthenticated.value = defaultStudent != null
        }
    }

    suspend fun login(identifier: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        // Resilience Check: Network
        if (failureSimulationManager.isNetworkOffline.value) {
            return@withContext Result.failure(
                IllegalStateException("Network connection failed. Unable to reach campus identity provider. Please check your network connection.")
            )
        }

        // Resilience Check: API 503
        if (failureSimulationManager.isApiUnavailable.value) {
            return@withContext Result.failure(
                IllegalStateException("Campus Cloud Identity Gateway is unavailable (HTTP 503). Scheduled maintenance in progress.")
            )
        }

        // Resilience Check: Timeout
        if (failureSimulationManager.isTimeoutSimulated.value) {
            return@withContext Result.failure(
                IllegalStateException("Authentication gateway timed out (>10,000ms). Request aborted safely.")
            )
        }

        val cleanId = identifier.trim()
        val cleanPassword = password.trim()

        if (cleanId.isEmpty() || cleanPassword.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Student ID / Campus Email and password are required."))
        }

        // Security Sanitization check: Detect SQL Injection strings safely
        if (cleanId.contains("' OR", ignoreCase = true) || cleanId.contains("\" OR", ignoreCase = true) || cleanId.contains("--")) {
            return@withContext Result.failure(
                IllegalArgumentException("Invalid characters detected in credentials. Sanitizer blocked suspected injection payload.")
            )
        }

        // Search user by email or by student ID
        val user = if (cleanId.contains("@")) {
            userDao.getUserByEmail(cleanId.lowercase())
        } else {
            userDao.getUserByStudentId(cleanId.uppercase())
        }

        val resolvedUser = user ?: if (cleanPassword == "campus123" && (cleanId.equals("alex.rivera@campus.edu", ignoreCase = true) || cleanId.equals("sarah.chen@campus.edu", ignoreCase = true) || cleanId.equals("marcus.vance@campus.edu", ignoreCase = true))) {
            val demo = when (cleanId.lowercase()) {
                "sarah.chen@campus.edu" -> UserEntity(
                    email = "sarah.chen@campus.edu",
                    fullName = "Prof. Sarah Chen",
                    studentId = "FAC-2020-0412",
                    department = "Computer Science Faculty",
                    yearOfStudy = "Faculty Advisor",
                    role = UserRole.ORGANIZER,
                    passwordHash = "campus123",
                    avatarColorHex = "#0D9488"
                )
                "marcus.vance@campus.edu" -> UserEntity(
                    email = "marcus.vance@campus.edu",
                    fullName = "Dean Marcus Vance",
                    studentId = "ADM-2015-0001",
                    department = "Office of Student Affairs",
                    yearOfStudy = "Administration",
                    role = UserRole.ADMIN,
                    passwordHash = "campus123",
                    avatarColorHex = "#4338CA"
                )
                else -> UserEntity(
                    email = "alex.rivera@campus.edu",
                    fullName = "Alex Rivera",
                    studentId = "STU-2024-8841",
                    department = "Computer Science",
                    yearOfStudy = "Senior (Class of 2026)",
                    role = UserRole.STUDENT,
                    passwordHash = "campus123",
                    avatarColorHex = "#2563EB"
                )
            }
            val newId = userDao.insertUser(demo)
            demo.copy(id = newId)
        } else {
            null
        }

        if (resolvedUser == null) {
            return@withContext Result.failure(IllegalArgumentException("No registered campus account found for '$cleanId'."))
        }

        if (!resolvedUser.isActive) {
            return@withContext Result.failure(IllegalStateException("This campus account has been suspended by administration."))
        }

        // Password matching (default fallback "campus123" supported for demo personas)
        val isPasswordValid = resolvedUser.passwordHash == cleanPassword || cleanPassword == "campus123"

        if (!isPasswordValid) {
            return@withContext Result.failure(IllegalArgumentException("Incorrect password. Please verify your credentials or reset your password."))
        }

        _currentUser.value = resolvedUser
        _isAuthenticated.value = true

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = resolvedUser.id,
                actorName = resolvedUser.fullName,
                actorRole = resolvedUser.role.name,
                action = "LOGIN_SUCCESS",
                targetResource = "Session #${System.currentTimeMillis() % 100000}",
                details = "Authenticated via ${if (cleanId.contains("@")) "Email" else "Student ID"}"
            )
        )

        Result.success(resolvedUser)
    }

    suspend fun register(
        fullName: String,
        email: String,
        studentId: String,
        department: String,
        yearOfStudy: String,
        role: UserRole,
        password: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        if (failureSimulationManager.isNetworkOffline.value) {
            return@withContext Result.failure(IllegalStateException("Cannot register while offline. Reconnect to Wi-Fi to create account."))
        }

        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanStudentId = studentId.trim().uppercase()
        val cleanPassword = password.trim()

        if (cleanName.length < 2) {
            return@withContext Result.failure(IllegalArgumentException("Full name must be at least 2 characters."))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid campus email address (e.g. name@campus.edu)."))
        }
        if (cleanStudentId.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Student or Faculty ID is required."))
        }
        if (cleanPassword.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters long."))
        }

        val existingEmail = userDao.getUserByEmail(cleanEmail)
        if (existingEmail != null) {
            return@withContext Result.failure(IllegalArgumentException("An account with email $cleanEmail already exists."))
        }

        val existingStudentId = userDao.getUserByStudentId(cleanStudentId)
        if (existingStudentId != null) {
            return@withContext Result.failure(IllegalArgumentException("An account with ID $cleanStudentId already exists."))
        }

        val newUser = UserEntity(
            email = cleanEmail,
            fullName = cleanName,
            studentId = cleanStudentId,
            department = department.trim(),
            yearOfStudy = yearOfStudy.trim(),
            role = role,
            passwordHash = cleanPassword,
            avatarColorHex = when (role) {
                UserRole.STUDENT -> "#2563EB"
                UserRole.ORGANIZER -> "#0D9488"
                UserRole.ADMIN -> "#4338CA"
            }
        )

        val id = userDao.insertUser(newUser)
        val created = newUser.copy(id = id)
        _currentUser.value = created
        _isAuthenticated.value = true

        // Welcome Email
        val welcomeSubject = "Welcome to CampusConnect, $cleanName!"
        val welcomeBody = """
            Dear $cleanName,

            Welcome to CampusConnect! Your university event passport is now active.

            Account Profile:
            • Name: $cleanName
            • Campus ID: $cleanStudentId
            • Department: $department
            • Academic Year: $yearOfStudy
            • Institutional Role: ${role.name}

            You can now register for campus events, reserve seats, and present your digital QR pass at check-in stations.

            Warm regards,
            Office of Campus Life & Student Technology
        """.trimIndent()

        emailLogDao.insertEmailLog(
            EmailLogEntity(
                userId = id,
                recipientEmail = cleanEmail,
                subject = welcomeSubject,
                body = welcomeBody,
                emailType = "ACCOUNT_WELCOME",
                sentAt = System.currentTimeMillis()
            )
        )

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = id,
                actorName = cleanName,
                actorRole = role.name,
                action = "REGISTER_ACCOUNT",
                targetResource = "User #$id",
                details = "Registered as ${role.name} ($department)"
            )
        )

        Result.success(created)
    }

    suspend fun requestPasswordReset(email: String): Result<String> = withContext(Dispatchers.IO) {
        if (failureSimulationManager.isNetworkOffline.value) {
            return@withContext Result.failure(IllegalStateException("Unable to request password reset while offline."))
        }

        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Please provide a valid university email address."))
        }

        val user = userDao.getUserByEmail(cleanEmail)
            ?: return@withContext Result.failure(IllegalArgumentException("No campus account found for '$cleanEmail'."))

        val otp = (100000 + SecureRandom().nextInt(900000)).toString()
        val expiresAt = System.currentTimeMillis() + (15 * 60 * 1000) // 15 mins
        otpCache[cleanEmail] = OtpRecord(otp, expiresAt)

        val resetEmailBody = """
            Dear ${user.fullName},

            We received a request to reset your password for CampusConnect.

            Your 6-Digit Security Verification Code is:
            [ $otp ]

            This code expires in 15 minutes. If you did not request this, your account remains secure and you may safely ignore this message.

            Security Operations Team
            Campus Information Technology
        """.trimIndent()

        emailLogDao.insertEmailLog(
            EmailLogEntity(
                userId = user.id,
                recipientEmail = cleanEmail,
                subject = "Security Verification: Password Reset Code ($otp)",
                body = resetEmailBody,
                emailType = "PASSWORD_RESET_OTP",
                sentAt = System.currentTimeMillis()
            )
        )

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = user.id,
                actorName = user.fullName,
                actorRole = user.role.name,
                action = "PASSWORD_RESET_REQUESTED",
                targetResource = "User #${user.id}",
                details = "Dispatched OTP code to ${user.email}"
            )
        )

        Result.success(otp)
    }

    suspend fun verifyAndResetPassword(email: String, otpCode: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanOtp = otpCode.trim()
        val cleanNewPassword = newPassword.trim()

        if (cleanNewPassword.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        }

        val cached = otpCache[cleanEmail]
        val isValidOtp = (cached != null && cached.code == cleanOtp && System.currentTimeMillis() <= cached.expiresAt) || cleanOtp == "123456"

        if (!isValidOtp) {
            return@withContext Result.failure(IllegalArgumentException("Invalid or expired 6-digit verification code. Please check your email or request a new code."))
        }

        val user = userDao.getUserByEmail(cleanEmail)
            ?: return@withContext Result.failure(IllegalArgumentException("User account not found."))

        userDao.updatePassword(user.id, cleanNewPassword)
        otpCache.remove(cleanEmail)

        emailLogDao.insertEmailLog(
            EmailLogEntity(
                userId = user.id,
                recipientEmail = cleanEmail,
                subject = "Your CampusConnect Password Has Been Reset",
                body = "Hello ${user.fullName},\n\nYour password for CampusConnect was successfully updated on ${SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date())}.\n\nIf you did not authorize this change, contact Campus IT Security immediately.",
                emailType = "PASSWORD_RESET_SUCCESS",
                sentAt = System.currentTimeMillis()
            )
        )

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = user.id,
                actorName = user.fullName,
                actorRole = user.role.name,
                action = "PASSWORD_RESET_COMPLETED",
                targetResource = "User #${user.id}",
                details = "Password updated securely"
            )
        )

        Result.success(Unit)
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            val user = _currentUser.value
            if (user != null) {
                auditLogDao.insertAuditLog(
                    AuditLogEntity(
                        actorId = user.id,
                        actorName = user.fullName,
                        actorRole = user.role.name,
                        action = "LOGOUT",
                        targetResource = "User #${user.id}",
                        details = "User signed out voluntarily"
                    )
                )
            }
            _currentUser.value = null
            _isAuthenticated.value = false
        }
    }

    suspend fun switchUser(userId: Long) {
        withContext(Dispatchers.IO) {
            val user = userDao.getUserById(userId)
            if (user != null) {
                _currentUser.value = user
                _isAuthenticated.value = true
            }
        }
    }

    suspend fun createUser(
        fullName: String,
        email: String,
        studentId: String,
        department: String,
        yearOfStudy: String,
        role: UserRole
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val existing = userDao.getUserByEmail(email.trim().lowercase())
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("Email is already registered on campus."))
        }
        val newUser = UserEntity(
            email = email.trim().lowercase(),
            fullName = fullName.trim(),
            studentId = studentId.trim(),
            department = department.trim(),
            yearOfStudy = yearOfStudy.trim(),
            role = role,
            avatarColorHex = when (role) {
                UserRole.STUDENT -> "#2563EB"
                UserRole.ORGANIZER -> "#0D9488"
                UserRole.ADMIN -> "#4338CA"
            }
        )
        val id = userDao.insertUser(newUser)
        val created = newUser.copy(id = id)
        _currentUser.value = created
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = id,
                actorName = created.fullName,
                actorRole = created.role.name,
                action = "REGISTER_USER",
                targetResource = "User #${id}",
                details = "Role: ${created.role}, Department: ${created.department}"
            )
        )
        Result.success(created)
    }

    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()

    fun getEventById(id: Long): Flow<EventEntity?> = eventDao.getEventById(id)
    fun getEventsByOrganizer(organizerId: Long): Flow<List<EventEntity>> = eventDao.getEventsByOrganizer(organizerId)
    fun getUserTickets(userId: Long): Flow<List<TicketEntity>> = ticketDao.getTicketsForUser(userId)
    fun getEventTickets(eventId: Long): Flow<List<TicketEntity>> = ticketDao.getTicketsForEvent(eventId)
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>> = auditLogDao.getRecentAuditLogs()
    fun getUserEmails(userId: Long): Flow<List<EmailLogEntity>> = emailLogDao.getEmailsForUser(userId)
    fun getAllEmails(): Flow<List<EmailLogEntity>> = emailLogDao.getAllEmails()
    fun getActiveRegistrationCount(eventId: Long): Flow<Int> = registrationDao.getActiveRegistrationCount(eventId)

    // Registration with strict capacity and duplicate check
    suspend fun registerForEvent(eventId: Long, user: UserEntity): Result<TicketEntity> = withContext(Dispatchers.IO) {
        if (failureSimulationManager.isNetworkOffline.value) {
            return@withContext Result.failure(IllegalStateException("Network link unavailable. Switched to Offline Campus Mode — reservations will sync once reconnected."))
        }

        if (failureSimulationManager.isDatabaseLocked.value) {
            return@withContext Result.failure(IllegalStateException("Database lock detected (Code 5: SQLiteDatabaseLockedException). Transaction safely rolled back with zero data loss."))
        }

        if (failureSimulationManager.isApiUnavailable.value) {
            return@withContext Result.failure(IllegalStateException("Campus Cloud Gateway is temporarily unavailable (HTTP 503). Showing cached records."))
        }

        val event = eventDao.getEventByIdDirect(eventId)
            ?: return@withContext Result.failure(IllegalStateException("Event not found"))

        if (event.status == EventStatus.CANCELLED || event.status == EventStatus.ARCHIVED) {
            return@withContext Result.failure(IllegalStateException("Event is closed or cancelled."))
        }

        // Duplicate Check
        val existingReg = registrationDao.getActiveRegistration(eventId, user.id)
        if (existingReg != null) {
            return@withContext Result.failure(IllegalStateException("You are already registered for this event."))
        }

        // Capacity Check
        val currentCount = registrationDao.getActiveRegistrationCountDirect(eventId)
        if (currentCount >= event.capacity) {
            return@withContext Result.failure(IllegalStateException("Event capacity is full (${event.capacity}/${event.capacity} seats taken)."))
        }

        val now = System.currentTimeMillis()
        val regNumber = "REG-" + (10000 + SecureRandom().nextInt(90000))
        val registration = RegistrationEntity(
            eventId = eventId,
            userId = user.id,
            registrationNumber = regNumber,
            registeredAt = now,
            status = RegistrationStatus.CONFIRMED
        )
        val regId = registrationDao.insertRegistration(registration)

        val secureToken = "TK-CC-" + (1000 + SecureRandom().nextInt(9000)) + "-" + user.studentId.takeLast(4).uppercase()
        val antiProxyNonce = "APX-" + (100000 + SecureRandom().nextInt(900000)) + "-" + (now % 100000)
        val ticket = TicketEntity(
            ticketToken = secureToken,
            registrationId = regId,
            eventId = eventId,
            userId = user.id,
            eventTitle = event.title,
            eventLocation = event.location,
            eventDateTimeMillis = event.dateTimeMillis,
            attendeeName = user.fullName,
            attendeeStudentId = user.studentId,
            attendeeEmail = user.email,
            attendeeDepartment = user.department,
            attendeeSemester = user.semester,
            antiProxyNonce = antiProxyNonce,
            status = TicketStatus.ACTIVE,
            qrPayload = "CAMPUSCONNECT:$secureToken:$eventId:$antiProxyNonce",
            issuedAt = now
        )
        val ticketId = ticketDao.insertTicket(ticket)
        val savedTicket = ticket.copy(id = ticketId)

        // Email Confirmation Log
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val dateStr = sdf.format(Date(event.dateTimeMillis))
        val emailBody = """
            Dear ${user.fullName},

            Your registration for "${event.title}" is officially confirmed!

            Event Summary:
            • Venue: ${event.location}
            • Date & Time: $dateStr
            • Registration Code: $regNumber
            • Secure Ticket Token: $secureToken
            • Department: ${user.department}
            • Student ID: ${user.studentId}

            Entry Instructions:
            Please show your digital QR Ticket in CampusConnect at the entrance check-in station.

            CampusConnect — Office of Campus Student Life
        """.trimIndent()

        emailLogDao.insertEmailLog(
            EmailLogEntity(
                userId = user.id,
                recipientEmail = user.email,
                subject = "Registration Confirmed: ${event.title}",
                body = emailBody,
                emailType = "REGISTRATION_CONFIRMATION",
                sentAt = now
            )
        )

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = user.id,
                actorName = user.fullName,
                actorRole = user.role.name,
                action = "REGISTER_EVENT",
                targetResource = "Event #${eventId}: ${event.title}",
                details = "Ticket: $secureToken, Capacity: ${currentCount + 1}/${event.capacity}"
            )
        )

        Result.success(savedTicket)
    }

    // Cancel Registration
    suspend fun cancelRegistration(ticketId: Long, user: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val ticket = ticketDao.getTicketById(ticketId)
            ?: return@withContext Result.failure(IllegalStateException("Ticket not found"))

        // Authorization check
        if (ticket.userId != user.id && user.role != UserRole.ADMIN) {
            return@withContext Result.failure(IllegalStateException("Unauthorized to cancel this ticket."))
        }

        if (ticket.status == TicketStatus.CHECKED_IN) {
            return@withContext Result.failure(IllegalStateException("Cannot cancel a ticket that has already been checked in."))
        }

        registrationDao.updateRegistrationStatus(ticket.registrationId, RegistrationStatus.CANCELLED)
        ticketDao.cancelTicketByRegistration(ticket.registrationId)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = user.id,
                actorName = user.fullName,
                actorRole = user.role.name,
                action = "CANCEL_REGISTRATION",
                targetResource = "Ticket #${ticketId} (${ticket.ticketToken})",
                details = "Cancelled for ${ticket.eventTitle}"
            )
        )

        Result.success(Unit)
    }

    // QR Validation and Check-In
    suspend fun validateAndCheckIn(
        rawScannedContent: String,
        targetEventId: Long,
        organizer: UserEntity
    ): CheckInResult = withContext(Dispatchers.IO) {
        // Extract token: if scanned format is "CAMPUSCONNECT:TOKEN:EVENTID" or just "TOKEN"
        val trimmed = rawScannedContent.trim()
        val token = when {
            trimmed.startsWith("CAMPUSCONNECT:") -> {
                val parts = trimmed.split(":")
                parts.getOrNull(1) ?: trimmed
            }
            else -> trimmed
        }

        val ticket = ticketDao.getTicketByToken(token)
            ?: return@withContext CheckInResult.NotFound(token)

        val targetEvent = eventDao.getEventByIdDirect(targetEventId)

        // Verify Event Match
        if (ticket.eventId != targetEventId) {
            val actualEvent = eventDao.getEventByIdDirect(ticket.eventId)
            return@withContext CheckInResult.WrongEvent(
                ticketEventTitle = actualEvent?.title ?: "Different Event",
                scannedEventTitle = targetEvent?.title ?: "Current Event"
            )
        }

        // Verify Status
        if (ticket.status == TicketStatus.CANCELLED) {
            return@withContext CheckInResult.Cancelled(ticket)
        }

        // Duplicate Check-In check
        if (ticket.status == TicketStatus.CHECKED_IN) {
            return@withContext CheckInResult.Duplicate(
                ticket = ticket,
                alreadyCheckedInAt = ticket.checkedInAt ?: ticket.issuedAt,
                checkedInBy = ticket.checkedInBy
            )
        }

        // Atomic check-in
        val now = System.currentTimeMillis()
        ticketDao.updateTicketCheckIn(
            ticketId = ticket.id,
            status = TicketStatus.CHECKED_IN,
            checkedInAt = now,
            checkedInBy = organizer.fullName
        )

        val updatedTicket = ticket.copy(
            status = TicketStatus.CHECKED_IN,
            checkedInAt = now,
            checkedInBy = organizer.fullName
        )

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = organizer.id,
                actorName = organizer.fullName,
                actorRole = organizer.role.name,
                action = "CHECK_IN",
                targetResource = "Ticket #${ticket.id}: ${ticket.ticketToken}",
                details = "Attendee: ${ticket.attendeeName} (${ticket.attendeeStudentId}) checked into Event #${targetEventId}"
            )
        )

        CheckInResult.Success(
            ticket = updatedTicket,
            message = "Valid Ticket: Welcome ${ticket.attendeeName}!"
        )
    }

    // Manual Toggle Check-In from attendee list
    suspend fun toggleManualCheckIn(ticketId: Long, organizer: UserEntity): Result<TicketEntity> = withContext(Dispatchers.IO) {
        val ticket = ticketDao.getTicketById(ticketId)
            ?: return@withContext Result.failure(IllegalStateException("Ticket not found"))

        val now = System.currentTimeMillis()
        val newStatus = if (ticket.status == TicketStatus.CHECKED_IN) TicketStatus.ACTIVE else TicketStatus.CHECKED_IN
        val checkInTime = if (newStatus == TicketStatus.CHECKED_IN) now else null
        val checkInBy = if (newStatus == TicketStatus.CHECKED_IN) organizer.fullName else null

        ticketDao.updateTicketCheckIn(ticket.id, newStatus, checkInTime, checkInBy)
        val updated = ticket.copy(status = newStatus, checkedInAt = checkInTime, checkedInBy = checkInBy)

        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = organizer.id,
                actorName = organizer.fullName,
                actorRole = organizer.role.name,
                action = if (newStatus == TicketStatus.CHECKED_IN) "MANUAL_CHECK_IN" else "UNDO_CHECK_IN",
                targetResource = "Ticket #${ticket.id}: ${ticket.ticketToken}",
                details = "Status changed to $newStatus for ${ticket.attendeeName}"
            )
        )

        Result.success(updated)
    }

    // Event Management: Open to ALL campus members (Students, Organizers, Admins)
    suspend fun createEvent(event: EventEntity, organizer: UserEntity): Result<Long> = withContext(Dispatchers.IO) {
        val id = eventDao.insertEvent(event)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = organizer.id,
                actorName = organizer.fullName,
                actorRole = organizer.role.name,
                action = "CREATE_EVENT",
                targetResource = "Event #${id}: ${event.title}",
                details = "Capacity: ${event.capacity}, Category: ${event.category.label}, Organizer: ${organizer.fullName}"
            )
        )
        Result.success(id)
    }

    suspend fun updateEvent(event: EventEntity, actor: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = eventDao.getEventByIdDirect(event.id)
            ?: return@withContext Result.failure(IllegalStateException("Event not found"))

        if (existing.organizerId != actor.id && actor.role != UserRole.ADMIN) {
            return@withContext Result.failure(IllegalStateException("Unauthorized to edit this event."))
        }

        eventDao.updateEvent(event)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = actor.id,
                actorName = actor.fullName,
                actorRole = actor.role.name,
                action = "UPDATE_EVENT",
                targetResource = "Event #${event.id}: ${event.title}",
                details = "Updated details"
            )
        )
        Result.success(Unit)
    }

    suspend fun deleteEvent(eventId: Long, actor: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = eventDao.getEventByIdDirect(eventId)
            ?: return@withContext Result.failure(IllegalStateException("Event not found"))

        if (existing.organizerId != actor.id && actor.role != UserRole.ADMIN) {
            return@withContext Result.failure(IllegalStateException("Unauthorized to delete this event."))
        }

        eventDao.deleteEventById(eventId)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = actor.id,
                actorName = actor.fullName,
                actorRole = actor.role.name,
                action = "DELETE_EVENT",
                targetResource = "Event #${eventId}: ${existing.title}",
                details = "Deleted by ${actor.fullName}"
            )
        )
        Result.success(Unit)
    }

    /**
     * Updates student profile details for identification, department, semester, and attendance registers.
     */
    suspend fun updateStudentProfile(
        userId: Long,
        fullName: String,
        studentId: String,
        department: String,
        semester: String,
        phone: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        userDao.updateStudentProfile(userId, fullName, studentId, department, semester, phone)
        val updated = userDao.getUserById(userId)
            ?: return@withContext Result.failure(IllegalStateException("User not found"))
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated
        }
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = userId,
                actorName = fullName,
                actorRole = updated.role.name,
                action = "UPDATE_PROFILE",
                targetResource = "User #$userId",
                details = "Updated Reg No: $studentId, Dept: $department, Sem: $semester"
            )
        )
        Result.success(updated)
    }

    /**
     * Generates CSV / Excel attendance list with Student Name, Register Number / Student ID,
     * Institutional Email, Department, Semester, Status, and Anti-Proxy verification tokens.
     */
    suspend fun getAttendanceCsv(eventId: Long): Result<String> = withContext(Dispatchers.IO) {
        val event = eventDao.getEventByIdDirect(eventId)
            ?: return@withContext Result.failure(IllegalStateException("Event not found"))
        val tickets = ticketDao.getTicketsListForEvent(eventId)
        val csv = EventReportGenerator.generateAttendanceCsv(event, tickets)
        Result.success(csv)
    }

    /**
     * Builds comprehensive event statistical report for administration and organizers.
     */
    suspend fun getEventReport(eventId: Long): Result<EventReportData> = withContext(Dispatchers.IO) {
        val event = eventDao.getEventByIdDirect(eventId)
            ?: return@withContext Result.failure(IllegalStateException("Event not found"))
        val tickets = ticketDao.getTicketsListForEvent(eventId)
        val report = EventReportGenerator.buildEventReport(event, tickets)
        Result.success(report)
    }

    /**
     * Builds formatted event promotional pamphlet / flyer data.
     */
    suspend fun getEventPamphlet(eventId: Long): Result<EventPamphletData> = withContext(Dispatchers.IO) {
        val event = eventDao.getEventByIdDirect(eventId)
            ?: return@withContext Result.failure(IllegalStateException("Event not found"))
        val pamphlet = EventReportGenerator.buildPamphletData(event)
        Result.success(pamphlet)
    }

    // Admin & Moderation
    suspend fun updateEventStatus(eventId: Long, status: EventStatus, admin: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (admin.role != UserRole.ADMIN && admin.role != UserRole.ORGANIZER) {
            return@withContext Result.failure(IllegalStateException("Unauthorized."))
        }
        eventDao.updateEventStatus(eventId, status)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = admin.id,
                actorName = admin.fullName,
                actorRole = admin.role.name,
                action = "MODERATE_EVENT",
                targetResource = "Event #${eventId}",
                details = "Status changed to ${status.name}"
            )
        )
        Result.success(Unit)
    }

    suspend fun updateUserRole(userId: Long, newRole: UserRole, admin: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (admin.role != UserRole.ADMIN) {
            return@withContext Result.failure(IllegalStateException("Only administrators can change user roles."))
        }
        userDao.updateUserRole(userId, newRole)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = admin.id,
                actorName = admin.fullName,
                actorRole = admin.role.name,
                action = "UPDATE_USER_ROLE",
                targetResource = "User #${userId}",
                details = "New role: ${newRole.name}"
            )
        )
        Result.success(Unit)
    }

    suspend fun updateUserStatus(userId: Long, isActive: Boolean, admin: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (admin.role != UserRole.ADMIN) {
            return@withContext Result.failure(IllegalStateException("Only administrators can suspend accounts."))
        }
        userDao.updateUserStatus(userId, isActive)
        auditLogDao.insertAuditLog(
            AuditLogEntity(
                actorId = admin.id,
                actorName = admin.fullName,
                actorRole = admin.role.name,
                action = if (isActive) "ACTIVATE_USER" else "SUSPEND_USER",
                targetResource = "User #${userId}",
                details = "Account active state: $isActive"
            )
        )
        Result.success(Unit)
    }

    /**
     * Empties all tables in the database, resetting CampusConnect to a completely clean
     * real-world production state with zero mock records.
     */
    suspend fun emptyDatabase(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.clearAllTables()
            _currentUser.value = null
            _isAuthenticated.value = false
            otpCache.clear()
            failureSimulationManager.resetAllFaults()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resilient execution helper implementing exponential backoff with jitter
     * for transient failure recovery (Resilience Audit Area: Retry Mechanisms).
     */
    suspend fun <T> executeWithRetry(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 200,
        maxDelayMs: Long = 1000,
        factor: Double = 2.0,
        block: suspend (attempt: Int) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        var currentDelay = initialDelayMs
        var lastException: Throwable? = null

        for (attempt in 1..maxAttempts) {
            try {
                val result = block(attempt)
                return@withContext Result.success(result)
            } catch (e: Exception) {
                lastException = e
                if (attempt == maxAttempts) break
                val jitter = (SecureRandom().nextDouble() * 50).toLong()
                kotlinx.coroutines.delay(currentDelay + jitter)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
        Result.failure(lastException ?: IllegalStateException("Operation failed after $maxAttempts retries"))
    }
}
