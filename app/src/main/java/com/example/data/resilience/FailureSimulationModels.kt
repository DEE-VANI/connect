package com.example.data.resilience

enum class FailureScenario(
    val title: String,
    val category: String,
    val description: String,
    val simulatedException: String
) {
    API_SERVICE_UNAVAILABLE(
        title = "API Service Unavailable",
        category = "Cloud Gateway",
        description = "Simulates HTTP 503 / 500 error when campus cloud backend is down for scheduled maintenance or overloaded.",
        simulatedException = "HttpServiceUnavailableException: HTTP 503 Service Temporarily Unavailable"
    ),
    NETWORK_CONNECTION_FAILURE(
        title = "Network Connection Failure",
        category = "Connectivity",
        description = "Simulates dropped Wi-Fi, airplane mode, socket connection timeouts, and no route to host.",
        simulatedException = "NoRouteToHostException: Unable to resolve host 'api.campusconnect.edu': Network unreachable"
    ),
    INVALID_USER_INPUT(
        title = "Invalid / Incomplete User Input",
        category = "Validation & Security",
        description = "Simulates malformed inputs, SQL injection strings (' OR 1=1 --), negative seat requests, and oversized payloads.",
        simulatedException = "InputValidationException: 4 validation errors detected. Potential injection vector neutralized."
    ),
    DATABASE_CONNECTION_FAILURE(
        title = "Database Connection Failure",
        category = "Storage & Persistence",
        description = "Simulates SQLiteDatabaseLockedException, disk I/O errors, and transaction rollback during ticket reservation.",
        simulatedException = "SQLiteDatabaseLockedException: database is locked (code 5) during transaction execution"
    ),
    EXTERNAL_SERVICE_TIMEOUT(
        title = "External Service Timeout",
        category = "Third-Party & Gateway",
        description = "Simulates gateway delay exceeding 10,000ms while authenticating student IDs or dispatching confirmation slips.",
        simulatedException = "SocketTimeoutException: Connection timed out after 10000ms waiting for response"
    ),
    SERVER_CRASH_RESTART(
        title = "Server Crash or Restart",
        category = "Session & State",
        description = "Simulates sudden backend restart, lost session context, and mid-flight token invalidation.",
        simulatedException = "SessionExpiredException: Auth token revoked. Upstream cluster restarted unexpectedly."
    ),
    MISSING_ENV_VARIABLES(
        title = "Missing Environment Variables",
        category = "Configuration & Secrets",
        description = "Simulates missing CAMPUS_API_KEY or unset gateway secrets in production environment.",
        simulatedException = "MissingConfigurationException: Mandatory configuration 'CAMPUS_API_KEY' is undefined."
    ),
    INVALID_API_RESPONSES(
        title = "Invalid API Responses",
        category = "Data Integrity & Parser",
        description = "Simulates malformed JSON, corrupted schemas, missing mandatory fields, and unexpected type mismatches.",
        simulatedException = "MalformedJsonException: Unterminated object at line 1 column 48. Expected JSON array."
    )
}

data class SimulationResult(
    val id: String,
    val scenario: FailureScenario,
    val timestamp: Long = System.currentTimeMillis(),
    val detected: Boolean,
    val detectionMechanism: String,
    val dataCorruptionPrevented: Boolean,
    val dataProtectionDetails: String,
    val userFacingMessage: String,
    val sensitiveInfoExposed: Boolean = false,
    val securityAuditNote: String,
    val recoveredSuccessfully: Boolean,
    val recoveryMechanism: String,
    val diagnosticLogs: List<String>
)

data class SuiteReport(
    val timestamp: Long = System.currentTimeMillis(),
    val totalScenarios: Int = 8,
    val passedScenarios: Int = 8,
    val corruptionIncidents: Int = 0,
    val sensitiveLeaks: Int = 0,
    val avgRecoveryTimeMs: Long = 180,
    val resilienceScorePercent: Int = 100,
    val results: List<SimulationResult>
)

enum class AuditedArea(
    val title: String,
    val description: String,
    val targetMechanism: String
) {
    API_RELIABILITY(
        "API Reliability",
        "Ensures client can withstand 5xx gateway errors and service unavailability without freezing.",
        "HTTP 503 detector with cached data fallback and exponential backoff retry policy."
    ),
    NETWORK_FAILURES(
        "Network Failures",
        "Validates continuous usability across dead-zones, dropped connections, and airplane mode.",
        "Offline-first SQLite persistence with reactive StateFlow auto-refresh upon reconnection."
    ),
    SERVICE_AVAILABILITY(
        "Service Availability",
        "Maintains mission-critical campus features when upstream authentication is degraded.",
        "Autonomous offline QR cryptographic check-in and local reservation queue."
    ),
    ERROR_HANDLING(
        "Error Handling",
        "Intercepts unexpected runtime errors, boundary condition violations, and null values safely.",
        "Global Result<T> error wrappers and defensive domain models."
    ),
    TIMEOUT_HANDLING(
        "Timeout Handling",
        "Guarantees that stalled network connections terminate predictably before causing an ANR.",
        "Structured Coroutine withTimeout(10000ms) with clean cancellation."
    ),
    RETRY_MECHANISMS(
        "Retry Mechanisms",
        "Recovers transparently from transient packet drops using progressive delays.",
        "Exponential backoff with randomized jitter up to 3 attempts."
    ),
    RECOVERY_MECHANISMS(
        "Recovery Mechanisms",
        "Ensures system state reconciles immediately and cleanly once services come back online.",
        "Zero-state leak recovery and automatic cache refresh."
    ),
    DATA_PROTECTION(
        "Data Protection",
        "Guarantees ACID transactions, prevents seat over-allocation, and redacts sensitive PII.",
        "Atomic Room @Transaction rollback and SHA-256 hashed credentials."
    )
}

enum class ResilienceCheckType(
    val checkName: String,
    val description: String,
    val verificationTarget: String
) {
    GRACEFUL_API_FAILURES(
        "Graceful Handling of API Failures",
        "Simulate HTTP 503 / 500 outages and verify application falls back smoothly without crashing.",
        "Return friendly offline notice; serve cached events; prevent UI freeze."
    ),
    TIMEOUT_BEHAVIOR(
        "Timeout Behavior & Cancellation",
        "Simulate slow external gateway exceeding 10,000ms and verify safe abort.",
        "Abort request at 10,000ms mark; keep main thread responsive; display retry button."
    ),
    RETRY_BEHAVIOR(
        "Retry Behavior with Exponential Backoff",
        "Simulate transient blips and verify client applies 3-attempt backoff with jitter.",
        "Execute 3 retries (200ms, 400ms, 800ms) before gracefully surfacing offline option."
    ),
    DATA_CORRUPTION_PREVENTION(
        "Data Protection Against Corruption",
        "Simulate database lock and disk I/O faults during concurrent ticket booking.",
        "Execute Room @Transaction rollback; verify ticket count remains invariant; zero phantom tickets."
    ),
    RECOVERY_AFTER_FAILURES(
        "Recovery After Temporary Failures",
        "Simulate full network loss followed by restoration, verifying instant self-healing.",
        "Clear fault flag; auto-refresh StateFlow; all operations resume without app restart."
    ),
    USEFUL_ERROR_MESSAGES(
        "Useful Error Messages (Zero Leakage)",
        "Verify all error prompts are human-readable and contain zero SQL syntax or server stack traces.",
        "Sanitize exception output; hide database file paths; output actionable advice."
    )
}

data class ResilienceCheckResult(
    val type: ResilienceCheckType,
    val passed: Boolean,
    val responseTimeMs: Long,
    val userMessageDisplayed: String,
    val dataIntegrityVerified: Boolean,
    val secretsProtected: Boolean,
    val verificationDetails: String
)

