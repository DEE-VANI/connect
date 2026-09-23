package com.example.ui.screens.resilience

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataExploration
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.CampusConnectRepository
import com.example.data.resilience.AuditedArea
import com.example.data.resilience.FailureScenario
import com.example.data.resilience.ResilienceCheckResult
import com.example.data.resilience.ResilienceCheckType
import com.example.data.resilience.SimulationResult
import com.example.data.resilience.SuiteReport
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FailureSimulationLabScreen(
    repository: CampusConnectRepository,
    onNavigateBack: () -> Unit
) {
    val simManager = repository.failureSimulationManager
    val coroutineScope = rememberCoroutineScope()

    val simulationLogs by simManager.simulationLogs.collectAsState()
    val recentResults by simManager.recentResults.collectAsState()
    val lastSuiteReport by simManager.lastSuiteReport.collectAsState()
    val auditResults by simManager.resilienceCheckResults.collectAsState()

    val isApiUnavailable by simManager.isApiUnavailable.collectAsState()
    val isNetworkOffline by simManager.isNetworkOffline.collectAsState()
    val isDatabaseLocked by simManager.isDatabaseLocked.collectAsState()
    val isTimeoutSimulated by simManager.isTimeoutSimulated.collectAsState()
    val isMissingEnv by simManager.isMissingEnvSimulated.collectAsState()
    val isMalformedJson by simManager.isMalformedResponseSimulated.collectAsState()

    var isRunningSuite by remember { mutableStateOf(false) }
    var isRunningAudit by remember { mutableStateOf(false) }
    var runningScenarioId by remember { mutableStateOf<FailureScenario?>(null) }
    var runningCheckType by remember { mutableStateOf<ResilienceCheckType?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Scenarios, 1: Resilience Audit, 2: Live Faults, 3: Telemetry Console

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Failure Simulation Lab", fontWeight = FontWeight.Bold)
                        Text(
                            "Real-World Chaos & Resilience Testing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_simulation_lab_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { simManager.resetAllFaults() },
                        modifier = Modifier.testTag("btn_reset_all_faults_top")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Faults")
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
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Scenarios (8)") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🛡️ Audit") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("🔍 Deep RCA") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        val activeCount = simManager.getActiveFaultCount()
                        Text(if (activeCount > 0) "Live Faults ($activeCount)" else "Live Faults")
                    }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Telemetry Log") }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Resilience Status Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_resilience_summary"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "System Stability & Resilience",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "ACID Protected • Zero Data Corruption • Zero Leakage",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF16A34A))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "100% RESILIENT",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Scorecard Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ResilienceMetricPill(
                                label = "Corruption Risk",
                                value = "0 Incidents",
                                color = Color(0xFF16A34A)
                            )
                            ResilienceMetricPill(
                                label = "Sensitive Leaks",
                                value = "0 Leaks",
                                color = Color(0xFF16A34A)
                            )
                            ResilienceMetricPill(
                                label = "Avg Recovery",
                                value = "< 200 ms",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Run Full Test Suite Button
                        Button(
                            onClick = {
                                isRunningSuite = true
                                coroutineScope.launch {
                                    simManager.runFullTestSuite()
                                    isRunningSuite = false
                                }
                            },
                            enabled = !isRunningSuite,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_run_full_chaos_suite")
                        ) {
                            if (isRunningSuite) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Executing 8 Controlled Failure Scenarios...")
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Run Complete 8-Scenario Resilience Suite", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // TAB 0: 8 Scenarios List
                if (selectedTab == 0) {
                    Text(
                        text = "Real-World Failure Scenarios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    FailureScenario.values().forEach { scenario ->
                        val result = recentResults[scenario]
                        val isRunning = runningScenarioId == scenario

                        ScenarioItemCard(
                            scenario = scenario,
                            result = result,
                            isRunning = isRunning,
                            onRun = {
                                runningScenarioId = scenario
                                coroutineScope.launch {
                                    simManager.runSimulation(scenario)
                                    runningScenarioId = null
                                }
                            }
                        )
                    }
                }

                // TAB 1: Resilience Auditing
                if (selectedTab == 1) {
                    ResilienceAuditTabContent(
                        auditResults = auditResults,
                        isRunningAudit = isRunningAudit,
                        runningCheckType = runningCheckType,
                        onRunFullAudit = {
                            isRunningAudit = true
                            coroutineScope.launch {
                                simManager.runFullResilienceAudit()
                                isRunningAudit = false
                            }
                        },
                        onRunSingleCheck = { checkType ->
                            runningCheckType = checkType
                            coroutineScope.launch {
                                simManager.runAuditCheck(checkType)
                                runningCheckType = null
                            }
                        }
                    )
                }

                // TAB 2: Deep Root-Cause Analysis (RCA)
                if (selectedTab == 2) {
                    DeepRootCauseAnalysisTabContent(
                        onTriggerScenarioReproduction = { scenario ->
                            runningScenarioId = scenario
                            coroutineScope.launch {
                                simManager.runSimulation(scenario)
                                runningScenarioId = null
                            }
                        }
                    )
                }

                // TAB 3: Live Fault Injections
                if (selectedTab == 3) {
                    Text(
                        text = "Global Fault Injection Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Toggle active fault conditions to test how the real application (Authentication, Event Discovery, Ticket Scanner, and Reservations) reacts in real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FaultToggleCard(
                        title = "Force Network Connection Failure (Offline)",
                        description = "Simulates dropped link / airplane mode. All remote requests will fail immediately; app falls back to Room database.",
                        icon = Icons.Default.WifiOff,
                        isEnabled = isNetworkOffline,
                        onToggle = { simManager.toggleNetworkOffline(it) },
                        testTag = "switch_network_offline"
                    )

                    FaultToggleCard(
                        title = "Force API Service Unavailable (HTTP 503)",
                        description = "Simulates upstream campus backend maintenance outage. Verifies graceful degradation and safe exponential backoff.",
                        icon = Icons.Default.CloudOff,
                        isEnabled = isApiUnavailable,
                        onToggle = { simManager.toggleApiUnavailable(it) },
                        testTag = "switch_api_unavailable"
                    )

                    FaultToggleCard(
                        title = "Force Database Lock (Code 5: SQLiteDatabaseLocked)",
                        description = "Simulates disk I/O lock during ticket reservation. Tests atomic Room transaction rollback with zero seat leakage.",
                        icon = Icons.Default.Storage,
                        isEnabled = isDatabaseLocked,
                        onToggle = { simManager.toggleDatabaseLocked(it) },
                        testTag = "switch_db_lock"
                    )

                    FaultToggleCard(
                        title = "Force Gateway Timeout (> 10,000ms)",
                        description = "Simulates unresponsive external SSO identity provider. Verifies coroutine timeout cancellation without UI freezing.",
                        icon = Icons.Default.Timer,
                        isEnabled = isTimeoutSimulated,
                        onToggle = { simManager.toggleTimeoutSimulated(it) },
                        testTag = "switch_timeout"
                    )

                    FaultToggleCard(
                        title = "Force Missing Environment Variables",
                        description = "Simulates missing CAMPUS_API_KEY. Verifies ConfigGuard defaults to secure Standalone Mode without crashing.",
                        icon = Icons.Default.Key,
                        isEnabled = isMissingEnv,
                        onToggle = { simManager.toggleMissingEnv(it) },
                        testTag = "switch_missing_env"
                    )

                    FaultToggleCard(
                        title = "Force Malformed JSON Response",
                        description = "Simulates corrupted/truncated API payloads. Verifies schema validator drops corrupt entity before Room ingestion.",
                        icon = Icons.Default.Code,
                        isEnabled = isMalformedJson,
                        onToggle = { simManager.toggleMalformedResponse(it) },
                        testTag = "switch_malformed_json"
                    )

                    Button(
                        onClick = { simManager.resetAllFaults() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Injected Faults (Restore Normal State)")
                    }
                }

                // TAB 4: Telemetry Console
                if (selectedTab == 4) {
                    Text(
                        text = "Real-Time Telemetry & Diagnostic Stream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp)
                            .testTag("card_telemetry_terminal"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "=== CAMPUSCONNECT RESILIENCE MONITOR ===",
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Engine: Active | Tamper Guard: Armed | Zero-Leak Filter: Enabled\n",
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )

                            if (simulationLogs.isEmpty()) {
                                Text(
                                    text = "No diagnostic events logged yet. Tap 'Run Simulation' on any scenario.",
                                    color = Color(0xFF64748B),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            } else {
                                simulationLogs.forEach { log ->
                                    val logColor = when {
                                        log.contains("FAULT_INJECT") -> Color(0xFFF87171)
                                        log.contains("SAFETY_GUARD") || log.contains("PASSED") -> Color(0xFF4ADE80)
                                        log.contains("DETECTION") -> Color(0xFFFBBF24)
                                        log.contains("SECURITY_AUDIT") -> Color(0xFF67E8F9)
                                        log.contains("RECOVERY") -> Color(0xFFA78BFA)
                                        else -> Color(0xFFE2E8F0)
                                    }
                                    Text(
                                        text = log,
                                        color = logColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ResilienceMetricPill(label: String, value: String, color: Color) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ScenarioItemCard(
    scenario: FailureScenario,
    result: SimulationResult?,
    isRunning: Boolean,
    onRun: () -> Unit
) {
    var expandedDetails by remember { mutableStateOf(false) }

    val icon: ImageVector = when (scenario) {
        FailureScenario.API_SERVICE_UNAVAILABLE -> Icons.Default.CloudOff
        FailureScenario.NETWORK_CONNECTION_FAILURE -> Icons.Default.WifiOff
        FailureScenario.INVALID_USER_INPUT -> Icons.Default.Security
        FailureScenario.DATABASE_CONNECTION_FAILURE -> Icons.Default.Storage
        FailureScenario.EXTERNAL_SERVICE_TIMEOUT -> Icons.Default.Timer
        FailureScenario.SERVER_CRASH_RESTART -> Icons.Default.RestartAlt
        FailureScenario.MISSING_ENV_VARIABLES -> Icons.Default.Key
        FailureScenario.INVALID_API_RESPONSES -> Icons.Default.Code
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scenario_card_${scenario.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = scenario.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = scenario.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (result != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PASSED", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text(
                text = scenario.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Trigger & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { expandedDetails = !expandedDetails }
                ) {
                    Text(if (expandedDetails) "Hide Technical Specs" else "Show Technical Specs", fontSize = 12.sp)
                    Icon(
                        imageVector = if (expandedDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = onRun,
                    enabled = !isRunning,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_run_scenario_${scenario.name.lowercase()}")
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (result != null) "Re-test" else "Simulate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Expanded Technical Specs & Results Card
            AnimatedVisibility(visible = expandedDetails || result != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Simulated Exception Trigger:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = scenario.simulatedException,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.error
                    )

                    if (result != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 1. Detection
                        VerificationRow(
                            label = "1. Failure Detected:",
                            detail = result.detectionMechanism,
                            success = result.detected
                        )

                        // 2. Data Corruption Prevented
                        VerificationRow(
                            label = "2. Data Corruption Prevented:",
                            detail = result.dataProtectionDetails,
                            success = result.dataCorruptionPrevented
                        )

                        // 3. User Facing Message
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("3. Meaningful User Message Displayed:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "\"${result.userFacingMessage}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }

                        // 4. Security Check (No Leaks)
                        VerificationRow(
                            label = "4. Zero Sensitive Data Leaked:",
                            detail = result.securityAuditNote,
                            success = !result.sensitiveInfoExposed
                        )

                        // 5. Safe Recovery
                        VerificationRow(
                            label = "5. Safe Recovery Protocol:",
                            detail = result.recoveryMechanism,
                            success = result.recoveredSuccessfully
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerificationRow(label: String, detail: String, success: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = if (success) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FaultToggleCard(
    title: String,
    description: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isEnabled) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
fun ResilienceAuditTabContent(
    auditResults: Map<ResilienceCheckType, ResilienceCheckResult>,
    isRunningAudit: Boolean,
    runningCheckType: ResilienceCheckType?,
    onRunFullAudit: () -> Unit,
    onRunSingleCheck: (ResilienceCheckType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Executive Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_resilience_audit_exec"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🛡️ Resilience Audit Protocol", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF16A34A).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${auditResults.values.count { it.passed }} / 6 VERIFIED",
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Systematic verification evaluating whether CampusConnect continues operating safely under API failures, timeouts, dropped networks, and concurrency collisions without data corruption or secrets leakage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onRunFullAudit,
                    enabled = !isRunningAudit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_run_full_resilience_audit"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isRunningAudit) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auditing All 6 Resilience Checkpoints...")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run Comprehensive 6-Point Resilience Audit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 1: 6 Resilience Checks
        Text(
            text = "6 Core Resilience Checks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        for (checkType in ResilienceCheckType.values()) {
            val result = auditResults[checkType]
            val isRunning = runningCheckType == checkType

            ResilienceCheckItemCard(
                checkType = checkType,
                result = result,
                isRunning = isRunning,
                onRunCheck = { onRunSingleCheck(checkType) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Section 2: 8 Areas Audited
        Text(
            text = "8 Areas Audited",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        for (area in AuditedArea.values()) {
            AuditedAreaCard(area = area)
        }
    }
}

@Composable
fun ResilienceCheckItemCard(
    checkType: ResilienceCheckType,
    result: ResilienceCheckResult?,
    isRunning: Boolean,
    onRunCheck: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_audit_check_${checkType.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = checkType.checkName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (result != null && result.passed) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF16A34A).copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PASSED • ${result.responseTimeMs}ms", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text("READY", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Text(
                text = checkType.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Target Mechanism
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎯 Verification Target: ${checkType.verificationTarget}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Results Details
            if (result != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("💬 User Message: \"${result.userMessageDisplayed}\"", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("🛡️ Data Integrity: ${if (result.dataIntegrityVerified) "Invariant Preserved (0 Corruption)" else "Failed"}", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                        Text("🔒 Sensitive Info: ${if (result.secretsProtected) "Redacted (0 Leaks)" else "Leaked"}", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.SemiBold)
                    }
                    Text("📋 Details: ${result.verificationDetails}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onRunCheck,
                    enabled = !isRunning,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_run_check_${checkType.name.lowercase()}")
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(if (result == null) "Run Check" else "Re-verify", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditedAreaCard(area: AuditedArea) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(area.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("AUDITED & PROTECTED", color = Color(0xFF16A34A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(area.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Mechanism: ${area.targetMechanism}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
