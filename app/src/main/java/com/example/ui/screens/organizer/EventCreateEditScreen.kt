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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.EventCategory
import com.example.data.local.entities.EventEntity
import com.example.data.local.entities.EventStatus
import com.example.data.repository.CampusConnectRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCreateEditScreen(
    repository: CampusConnectRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser by repository.currentUser.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var capacityStr by remember { mutableStateOf("60") }
    var durationStr by remember { mutableStateOf("2.0") }
    var tags by remember { mutableStateOf("Campus,Tech,Workshop") }

    // Organizer & Coordinator fields
    var organizerName by remember(currentUser) { mutableStateOf(currentUser?.fullName ?: "") }
    var organizerEmail by remember(currentUser) { mutableStateOf(currentUser?.email ?: "") }
    var organizerContact by remember { mutableStateOf("+1 (555) 019-2834") }
    var studentCoordinators by remember {
        mutableStateOf("Rohan Sharma (rohan@campus.edu - 555-0144), Ananya Das (ananya@campus.edu)")
    }
    var facultyAdvisor by remember {
        mutableStateOf("Dr. Sarah Vance, Dept of Computer Science (vance@campus.edu)")
    }

    var selectedCategory by remember { mutableStateOf(EventCategory.TECH) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Calendar & Time state
    val calendar = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3); set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0) } }
    var selectedDateTimeMillis by remember { mutableStateOf(calendar.timeInMillis) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.US) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateTimeMillis)
    val timePickerState = rememberTimePickerState(initialHour = 10, initialMinute = 0)

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Create Campus Event", fontWeight = FontWeight.Bold)
                        Text(
                            "Open to all students, club leads & faculty",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_create_event")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Open Creation Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Any registered student or faculty member can organize and publish events with dynamic anti-proxy ticketing!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Section 1: Event Details
            Text(
                text = "1. Event Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Event Title *") },
                placeholder = { Text("e.g. AI Innovation Showcase & Hackathon") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_event_title")
            )

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor().testTag("dropdown_event_category")
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    EventCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.label) },
                            onClick = {
                                selectedCategory = category
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Campus Venue / Location *") },
                placeholder = { Text("e.g. Science Complex Auditorium Hall A") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_event_location")
            )

            // Section 2: Date & Time with Interactive Calendar Pickers
            Text(
                text = "2. Date & Time Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Date Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDatePickerDialog = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Event Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    dateFormat.format(Date(selectedDateTimeMillis)),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        TextButton(onClick = { showDatePickerDialog = true }) {
                            Text("Change Date")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Time Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showTimePickerDialog = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Start Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    timeFormat.format(Date(selectedDateTimeMillis)),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        TextButton(onClick = { showTimePickerDialog = true }) {
                            Text("Change Time")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = capacityStr,
                    onValueChange = { capacityStr = it },
                    label = { Text("Capacity *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("input_event_capacity")
                )

                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it },
                    label = { Text("Duration (Hours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("input_event_duration")
                )
            }

            // Section 3: Organizers & Coordinators
            Text(
                text = "3. Organizers & Student Coordinators",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = organizerName,
                onValueChange = { organizerName = it },
                label = { Text("Lead Organizer Name *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_organizer_name")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = organizerEmail,
                    onValueChange = { organizerEmail = it },
                    label = { Text("Organizer Email *") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("input_organizer_email")
                )

                OutlinedTextField(
                    value = organizerContact,
                    onValueChange = { organizerContact = it },
                    label = { Text("Phone / Office") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("input_organizer_contact")
                )
            }

            OutlinedTextField(
                value = studentCoordinators,
                onValueChange = { studentCoordinators = it },
                label = { Text("Student Coordinators (Name, Email & Phone) *") },
                placeholder = { Text("e.g. Rahul Verma (rahul@campus.edu - 555-0199), Sneha Patil (sneha@campus.edu)") },
                leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("input_student_coordinators")
            )

            OutlinedTextField(
                value = facultyAdvisor,
                onValueChange = { facultyAdvisor = it },
                label = { Text("Faculty / Department Advisor") },
                placeholder = { Text("e.g. Prof. Alan Turing, Dept of CSE (alan@campus.edu)") },
                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_faculty_advisor")
            )

            // Section 4: Details & Rules
            Text(
                text = "4. Description & Pamphlet Guidelines",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description & Instructions *") },
                placeholder = { Text("Provide details about agendas, speakers, prizes, what to bring...") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth().testTag("input_event_description")
            )

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma separated)") },
                placeholder = { Text("Tech, AI, Prizes, Free Food") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_event_tags")
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val user = currentUser
                    if (user == null) {
                        errorMessage = "Please sign in to publish an event."
                        return@Button
                    }

                    if (title.isBlank() || location.isBlank() || description.isBlank() || organizerName.isBlank() || organizerEmail.isBlank()) {
                        errorMessage = "Please fill in all required fields."
                        return@Button
                    }

                    val capacity = capacityStr.toIntOrNull() ?: 50
                    if (capacity <= 0) {
                        errorMessage = "Capacity must be greater than zero."
                        return@Button
                    }

                    val duration = durationStr.toDoubleOrNull() ?: 2.0
                    val endMillis = selectedDateTimeMillis + (duration * 3600000).toLong()

                    val newEvent = EventEntity(
                        title = title.trim(),
                        description = description.trim(),
                        category = selectedCategory,
                        location = location.trim(),
                        dateTimeMillis = selectedDateTimeMillis,
                        endDateTimeMillis = endMillis,
                        durationHours = duration,
                        capacity = capacity,
                        organizerId = user.id,
                        organizerName = organizerName.trim(),
                        organizerEmail = organizerEmail.trim(),
                        organizerContact = organizerContact.trim(),
                        coordinators = studentCoordinators.trim(),
                        facultyAdvisor = facultyAdvisor.trim(),
                        status = EventStatus.ACTIVE,
                        tags = tags.trim()
                    )

                    isSubmitting = true
                    coroutineScope.launch {
                        val result = repository.createEvent(newEvent, user)
                        isSubmitting = false
                        if (result.isSuccess) {
                            onNavigateBack()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to publish event"
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_publish_event")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish Campus Event", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Date Picker Dialog
        if (showDatePickerDialog) {
            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { pickedUtc ->
                            // Preserve current hour and minute
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = pickedUtc
                                val currentCal = Calendar.getInstance().apply { timeInMillis = selectedDateTimeMillis }
                                set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                            }
                            selectedDateTimeMillis = cal.timeInMillis
                        }
                        showDatePickerDialog = false
                    }) {
                        Text("Confirm Date")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Time Picker Dialog
        if (showTimePickerDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showTimePickerDialog = false },
                title = { Text("Select Event Time", fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TimePicker(state = timePickerState)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedDateTimeMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        selectedDateTimeMillis = cal.timeInMillis
                        showTimePickerDialog = false
                    }) {
                        Text("Confirm Time")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePickerDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
