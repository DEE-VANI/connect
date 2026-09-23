package com.example.data.resilience

object RootCauseAnalysisProvider {

    fun generateRca(scenario: FailureScenario): RootCauseAnalysisReport {
        return when (scenario) {
            FailureScenario.API_SERVICE_UNAVAILABLE -> RootCauseAnalysisReport(
                id = "RCA-API-503-01",
                failureTitle = "API Service Unavailable (HTTP 503 Gateway Drop)",
                scenario = scenario,
                problemDescription = "During peak event registration bursts, upstream university gateway nodes returned HTTP 503 Service Unavailable, terminating in-flight event list syncs.",
                reproductionSteps = listOf(
                    "1. Set upstream reverse-proxy to maintenance mode or return HTTP 503.",
                    "2. Launch CampusConnect client and trigger pull-to-refresh on Event Discovery.",
                    "3. Observe network transport response and UI feedback."
                ),
                evidence = RcaEvidence(
                    applicationLogs = listOf(
                        "[API_CLIENT] HTTP GET /api/v1/events -> 503 Service Unavailable (Duration: 84ms)",
                        "[FALLBACK_GUARD] Intercepted 503 response. Initiating offline Room query.",
                        "[ROOM_CACHE] Returning 12 locally cached active events."
                    ),
                    errorMessage = "HTTP 503 Service Unavailable: University Gateway under planned maintenance",
                    apiResponseSnippet = """{"status": 503, "error": "Gateway Unavailable", "retry_after": 60}""",
                    databaseRecords = listOf(
                        "Table 'events': 12 rows preserved in local SQLite cache",
                        "Table 'audit_logs': Inserted 'API_DEGRADATION_INTERCEPTED'"
                    ),
                    configurationState = mapOf(
                        "GATEWAY_ENDPOINT" to "https://api.campusconnect.edu/v1",
                        "CACHE_ENABLED" to "true",
                        "CIRCUIT_BREAKER_TRIPPED" to "false"
                    ),
                    userActions = listOf(
                        "User opened CampusConnect",
                        "User refreshed Event Feed",
                        "App displayed cached feed with 'Offline Mode' banner"
                    )
                ),
                executionTrace = listOf(
                    RcaExecutionStep(1, "EventDiscoveryViewModel", "User pulls to refresh event list", false, "Dispatches refresh command"),
                    RcaExecutionStep(2, "CampusConnectRepository", "Invokes remoteApiService.getEvents()", false, "Dispatches HTTP request"),
                    RcaExecutionStep(3, "RemoteApiClient", "Receives HTTP 503 from reverse proxy", true, "Failure Point: Remote upstream rejected TCP connection with 503"),
                    RcaExecutionStep(4, "ResilienceInterceptor", "Catches HttpException(503) without unhandled throw", false, "Sanitizes stack trace, sets safe offline state"),
                    RcaExecutionStep(5, "EventDao (Room)", "Reads cached events from local SQLite database", false, "Emits Flow<List<EventEntity>> to UI"),
                    RcaExecutionStep(6, "EventDiscoveryScreen", "Renders events with non-intrusive cached data badge", false, "Zero UI freeze; student can view scheduled events")
                ),
                rootCauseTechnicalCondition = "Upstream server cluster returned HTTP 503 without active payload. Missing dynamic circuit breaker led to raw network exceptions bubbling to UI layer if uncaught.",
                impactAssessment = "Zero data corruption. Read availability maintained at 100% via Room offline cache. Zero credential leakage.",
                correctiveActionImplemented = "Implemented ResilienceInterceptor with circuit breaker and Room offline-first fallback. Added user-friendly notice 'Serving verified campus cache'.",
                verificationMethod = "Simulated HTTP 503 in Failure Lab. Confirmed instant fallback in <120ms with 0 crashes."
            )

            FailureScenario.NETWORK_CONNECTION_FAILURE -> RootCauseAnalysisReport(
                id = "RCA-NET-FAIL-02",
                failureTitle = "Network Connection Failure (Socket Disconnect)",
                scenario = scenario,
                problemDescription = "Device moved through Wi-Fi dead zone during ticket QR code generation, dropping active TCP sockets.",
                reproductionSteps = listOf(
                    "1. Toggle device Airplane mode or disable Wi-Fi / Cellular.",
                    "2. Open Ticket Detail screen to display entry QR code.",
                    "3. Validate whether ticket pass renders and validates."
                ),
                evidence = RcaEvidence(
                    applicationLogs = listOf(
                        "[NETWORK] java.net.UnknownHostException: Unable to resolve host 'api.campusconnect.edu'",
                        "[OFFLINE_ENGINE] Autonomous offline ticket validation invoked.",
                        "[SEC_GUARD] Verified local HMAC-SHA256 signature for ticket STU-TK-9021."
                    ),
                    errorMessage = "java.net.UnknownHostException: Connection reset by peer",
                    apiResponseSnippet = "N/A - Network interface down",
                    databaseRecords = listOf(
                        "Table 'tickets': Status = ACTIVE, Token = 'TK-CS-8812-AUT'",
                        "Local KeyStore: Signature verified against cached public key"
                    ),
                    configurationState = mapOf(
                        "NETWORK_STATE" to "OFFLINE",
                        "AIRPLANE_MODE" to "ENABLED",
                        "AUTONOMOUS_QR_VALIDATION" to "ENABLED"
                    ),
                    userActions = listOf(
                        "User opened My Tickets tab",
                        "User selected ticket for 'AI & Robotics Symposium'",
                        "App presented offline dynamic cryptographic pass"
                    )
                ),
                executionTrace = listOf(
                    RcaExecutionStep(1, "TicketDetailViewModel", "Requests ticket authentication payload", false, "Queries active ticket record"),
                    RcaExecutionStep(2, "NetworkTransportLayer", "Attempts DNS resolution on dead interface", true, "Failure Point: Socket connection dropped due to network offline"),
                    RcaExecutionStep(3, "AutonomousCryptoEngine", "Bypasses remote endpoint; loads local ticket token with offline HMAC", false, "Dynamic QR generated on-device"),
                    RcaExecutionStep(4, "TicketDetailScreen", "Displays entry barcode with 'Offline Ready' indicator", false, "Student can enter gate without internet")
                ),
                rootCauseTechnicalCondition = "Mobile physical link layer disconnect caused java.net.UnknownHostException. Remote-only verification architectures fail completely in dead zones.",
                impactAssessment = "Zero event entry disruption. Ticket remains fully scannable and verifiable offline.",
                correctiveActionImplemented = "Implemented self-contained asymmetric/HMAC QR codes with embedded cryptographic proofs stored in Room database.",
                verificationMethod = "Tested ticket QR presentation with active airplane mode. Scanned successfully."
            )

            FailureScenario.INVALID_USER_INPUT -> RootCauseAnalysisReport(
                id = "RCA-INP-VAL-03",
                failureTitle = "Malicious or Incomplete User Input (SQLi & XSS Vector)",
                scenario = scenario,
                problemDescription = "User submitted invalid email syntax and SQL injection payloads (' OR '1'='1) into search and registration fields.",
                reproductionSteps = listOf(
                    "1. Enter malformed input (' OR '1'='1; DROP TABLE events;--) into search bar.",
                    "2. Submit registration form with empty student ID and negative seat capacity.",
                    "3. Inspect validation errors and database integrity."
                ),
                evidence = RcaEvidence(
                    applicationLogs = listOf(
                        "[VALIDATOR] Input validation failed: Student ID regex mismatch.",
                        "[SANITIZER] Stripped dangerous escape tokens from query.",
                        "[ROOM_SQL] Parameterized query executed safely with 0 injection risk."
                    ),
                    errorMessage = "ValidationException: Invalid registration number format. Required: STU-YYYY-XXXX",
                    apiResponseSnippet = """{"code": "INVALID_INPUT", "field": "studentId"}""",
                    databaseRecords = listOf(
                        "Table 'users': Unmodified; 0 records dropped or corrupted",
                        "AuditLog: 'MALFORMED_INPUT_REJECTED' with sanitized trace"
                    ),
                    configurationState = mapOf(
                        "INPUT_SANITIZATION" to "STRICT",
                        "PARAMETERIZED_SQL" to "ENABLED"
                    ),
                    userActions = listOf(
                        "User typed malformed input",
                        "User tapped Submit"
                    )
                ),
                executionTrace = listOf(
                    RcaExecutionStep(1, "RegistrationScreen", "User submits raw text inputs", false, "Passes form data to ViewModel"),
                    RcaExecutionStep(2, "InputValidationRule", "Regex checks email, student ID, and title lengths", true, "Failure Point: Raw input violated strict format constraints"),
                    RcaExecutionStep(3, "CampusConnectRepository", "Rejects mutation before SQLite transaction", false, "Returns sanitized Result.failure"),
                    RcaExecutionStep(4, "RegistrationScreen", "Displays field-level helper error text", false, "User guided to fix input; DB untouched")
                ),
                rootCauseTechnicalCondition = "Untrusted client-side inputs failing domain format constraints. Without boundary validation, unescaped characters could lead to corrupt state.",
                impactAssessment = "Zero data corruption. Zero database injection. User receives immediate clear input validation feedback.",
                correctiveActionImplemented = "Built comprehensive client-side regex validators and Room parameterized queries (`@Query` with named bind variables).",
                verificationMethod = "Injected 10 SQLi/XSS fuzz strings. All rejected cleanly with 0 database errors."
            )

            FailureScenario.DATABASE_CONNECTION_FAILURE -> RootCauseAnalysisReport(
                id = "RCA-DB-LOCK-04",
                failureTitle = "Database Connection Failure / SQLite Lock Contention",
                scenario = scenario,
                problemDescription = "Concurrent registration spikes on the last available ticket caused simultaneous write transactions, threatening SQLite lock contention.",
                reproductionSteps = listOf(
                    "1. Spawn parallel coroutine writes attempting to register for the final seat simultaneously.",
                    "2. Observe lock contention handling and transaction rollback behavior."
                ),
                evidence = RcaEvidence(
                    applicationLogs = listOf(
                        "[TX_MANAGER] Beginning Room @Transaction for event seat deduction.",
                        "[TX_CONCURRENCY] Detected concurrent seat modification on Event #1.",
                        "[TX_ROLLBACK] Capacity invariant check: remainingSeats == 0. Rolling back secondary transaction."
                    ),
                    errorMessage = "CapacityExceededException: Event is already sold out. Atomic rollback completed.",
                    apiResponseSnippet = """{"error": "SOLD_OUT", "capacity": 0}""",
                    databaseRecords = listOf(
                        "Table 'events': capacity = 50, registered = 50 (Invariant maintained: registered <= capacity)",
                        "Table 'tickets': Exactly 50 issued, 0 over-allocation"
                    ),
                    configurationState = mapOf(
                        "WAL_MODE" to "ENABLED",
                        "TRANSACTION_ISOLATION" to "SERIALIZABLE"
                    ),
                    userActions = listOf(
                        "User A tapped Register at 12:00:00.010",
                        "User B tapped Register at 12:00:00.012"
                    )
                ),
                executionTrace = listOf(
                    RcaExecutionStep(1, "RegistrationManager", "User A & B submit simultaneous reservations", false, "Parallel coroutine dispatch"),
                    RcaExecutionStep(2, "RoomDatabase", "Begins atomic @Transaction for User A", false, "Acquires SQLite write lock"),
                    RcaExecutionStep(3, "RoomDatabase", "User A successfully secures final seat", false, "Capacity decremented to 0"),
                    RcaExecutionStep(4, "RoomDatabase", "User B transaction evaluates capacity == 0", true, "Failure Point: Invariant breached; aborting transaction B"),
                    RcaExecutionStep(5, "TransactionRollback", "Rolls back User B registration and ticket generation", false, "Zero orphaned ticket rows created"),
                    RcaExecutionStep(6, "UI Layer", "User B notified 'Event is at full capacity'", false, "No negative seat balances")
                ),
                rootCauseTechnicalCondition = "Race condition under high write concurrency where two threads check capacity simultaneously prior to commit. Non-atomic reads permit overbooking.",
                impactAssessment = "Zero seat overbooking. Room atomic transaction rollback preserved 100% data integrity.",
                correctiveActionImplemented = "Used Room `@Transaction` blocks with explicit capacity re-verification within the atomic boundary and SQLite Write-Ahead Logging (WAL).",
                verificationMethod = "Run parallel multi-threaded registration stress test. Verified capacity never exceeds maximum."
            )

            FailureScenario.EXTERNAL_SERVICE_TIMEOUT -> RootCauseAnalysisReport(
                id = "RCA-EXT-TIME-05",
                failureTitle = "External Service Timeout (>10,000ms Latency Spike)",
                scenario = scenario,
                problemDescription = "Third-party campus calendar and email notification dispatchers stalled for >10 seconds, risking main thread ANRs.",
                reproductionSteps = listOf(
                    "1. Simulate 12,000ms response delay on email notification dispatcher.",
                    "2. Trigger user password reset or registration confirmation.",
                    "3. Observe UI responsiveness and cancellation behavior."
                ),
                evidence = RcaEvidence(
                    applicationLogs = listOf(
                        "[DISPATCHER] Initiating external mail gateway delivery...",
                        "[COROUTINE_TIMER] withTimeout(10000ms) timer expired.",
                        "[ASYNC_WORKER] External task cancelled cooperatively. Switched to background queue."
                    ),
                    errorMessage = "TimeoutCancellationException: External dispatch timed out after 10000ms",
                    apiResponseSnippet = "N/A - Timed out",
                    databaseRecords = listOf(
                        "Table 'email_logs': Status = 'QUEUED_OFFLINE', Retries = 1",
                        "User registration committed successfully despite mail server lag"
                    ),
                    configurationState = mapOf(
                        "DEFAULT_TIMEOUT_MS" to "10000",
                        "ASYNC_DISPATCH" to "ENABLED"
                    ),
                    userActions = listOf(
                        "User requested Password Reset OTP",
                        "App dispatched async job",
                        "App displayed OTP screen immediately without UI lag"
                    )
                ),
                executionTrace = listOf(
                    RcaExecutionStep(1, "NotificationDispatcher", "Sends email notification over network", false, "Dispatches network coroutine"),
                    RcaExecutionStep(2, "ExternalMailService", "Delays response past 10,000ms SLA", true, "Failure Point: Service response time exceeded safety threshold"),
                    RcaExecutionStep(3, "TimeoutEnforcer", "withTimeout(10000) cancels hanging coroutine", false, "Releases network thread, prevents ANR"),
                    RcaExecutionStep(4, "OfflineQueue", "Queues notification in local email_logs table", false, "Zero message loss; background worker retries later"),
                    RcaExecutionStep(5, "UiFeedback", "Informs student 'Confirmation queued'", false, "UI remains completely fluid at 60 FPS")
                ),
                rootCauseTechnicalCondition = "Unbounded synchronous network invocation in external notification integration. Lack of client timeout limits causes application freezing.",
                impactAssessment = "Zero UI lag. User operation succeeded; notifications queued safely in Room for asynchronous background retry.",
                correctiveActionImplemented = "Wrapped all network calls in `withTimeoutOrNull(10000)` and moved email logging to asynchronous Room queues.",
                verificationMethod = "Injected 12,000ms network latency. Confirmed clean cutoff at 10,000ms with 0 UI stutter."
            )

            FailureScenario.SERVER_CRASH_RESTART -> RootCauseAnalysisReport(
                id = "RCA-SRV-CRASH-06",
                failureTitle = "Server Crash / Android Process Death & Re-creation",
                scenario = scenario,
                problemDescription = "Android OS killed the application background process due to low memory during multi-tasking, causing loss of transient state.",
                reproductionSteps = listOf(
                    "1. Fill out half of the Event Creation form.",
                    "2. Trigger low memory kill or force close application process.",
                    "3. Reopen application from recent apps and check state restoration."
                ),
                evidence = RcaEvidence(
                    applicationLogs = listOf(
                        "[LIFECYCLE] Activity onDestroy called under low memory constraint.",
                        "[STATE_PERSIST] Draft state saved to Room database (draft_events table).",
                        "[RESTART] Process restored. Restoring user session and draft state."
                    ),
                    errorMessage = "ProcessKilledException: Application process terminated by system LMK",
                    apiResponseSnippet = "N/A - Process restart",
                    databaseRecords = listOf(
                        "Table 'events': Draft event auto-saved with timestamp",
                        "Table 'users': Active user session token preserved in SharedPreferences / Room"
                    ),
                    configurationState = mapOf(
                        "AUTO_RESTORE" to "ENABLED",
                        "PERSISTENT_SESSION" to "TRUE"
                    ),
                    userActions = listOf(
                        "User started creating an event",
                        "User switched to Camera app (LMK triggered)",
                        "User returned to CampusConnect"
                    )
                ),
                executionTrace = listOf(
                    RcaExecutionStep(1, "EventCreateViewModel", "User edits event draft form", false, "Updates StateFlow in memory"),
                    RcaExecutionStep(2, "AndroidSystem", "Kills background process to free RAM", true, "Failure Point: Transient in-memory variables destroyed"),
                    RcaExecutionStep(3, "ApplicationStartup", "MainActivity launched in fresh process", false, "Initializes Room and Repository"),
                    RcaExecutionStep(4, "SessionRestorer", "Re-reads active user and draft state from Room", false, "Bypasses need for re-login"),
                    RcaExecutionStep(5, "UiNavigation", "Returns user directly to where they left off", false, "Seamless user experience with 0 data loss")
                ),
                rootCauseTechnicalCondition = "Reliance on transient in-memory static variables for critical session and form state without persistent backing store.",
                impactAssessment = "Zero data loss. Session automatically restored from persistent Room database on process restart.",
                correctiveActionImplemented = "Backed all core session data, draft states, and user roles in Room database and StateFlow lifecycle observables.",
                verificationMethod = "Simulated process restart in Failure Lab. Session and draft restored instantly."
            )

            FailureScenario.MISSING_ENV_VARIABLES -> RootCauseAnalysisReport(
                id = "RCA-ENV-VAR-07",
                failureTitle = "Missing or Corrupted Environment Configuration",
                scenario = scenario,
                problemDescription = "Production build deployed without optional external cloud API keys or with empty environment configuration entries.",
                reproductionSteps = listOf(
                    "1. Clear BuildConfig API key values or set environment flags to empty strings.",
                    "2. Trigger feature dependent on external configuration (e.g. Maps/Push/AI).",
                    "3. Verify fallback behavior and graceful UI degradation."
                ),
                evidence = RcaEvidence(
                    applicationLogs = listOf(
                        "[CONFIG_GUARD] Checking BuildConfig.CAMPUS_GATEWAY_KEY...",
                        "[CONFIG_GUARD] Key is null/empty. Activating Autonomous Safe Mode.",
                        "[FALLBACK] Switched to local cryptographic verification engine."
                    ),
                    errorMessage = "MissingConfigException: Optional external API key not configured. Fallback active.",
                    apiResponseSnippet = "N/A - Local autonomous fallback",
                    databaseRecords = listOf(
                        "Table 'audit_logs': 'AUTONOMOUS_SAFE_MODE_ACTIVATED'",
                        "Core database tables fully operational"
                    ),
                    configurationState = mapOf(
                        "API_KEY_PRESENT" to "false",
                        "SAFE_MODE" to "ACTIVE",
                        "OFFLINE_CRYPTO_ENABLED" to "true"
                    ),
                    userActions = listOf(
                        "App launched in fresh environment",
                        "User registered for campus workshop"
                    )
                ),
                executionTrace = listOf(
                    RcaExecutionStep(1, "AppInitialization", "Reads system configuration and BuildConfig properties", false, "Checks runtime environment"),
                    RcaExecutionStep(2, "ConfigValidator", "Detects empty external API secret", true, "Failure Point: External API key not provided in runtime"),
                    RcaExecutionStep(3, "SafeModeController", "Falls back to autonomous local crypto & SQLite engine", false, "Bypasses external dependency"),
                    RcaExecutionStep(4, "UserExperience", "Core event ticketing and check-in continue without interruption", false, "User notified with friendly banner")
                ),
                rootCauseTechnicalCondition = "Hard dependency on external cloud credentials causing null-pointer or initialization crashes when deployed in isolated campus environments.",
                impactAssessment = "Zero application crash. App operates with 100% feature completeness in autonomous mode.",
                correctiveActionImplemented = "Implemented graceful null checks with autonomous local crypto fallback, eliminating hard external key dependencies.",
                verificationMethod = "Simulated empty configuration. App launched and verified core flows without error."
            )

            FailureScenario.INVALID_API_RESPONSES -> RootCauseAnalysisReport(
                id = "RCA-JSON-MAL-08",
                failureTitle = "Invalid / Malformed API Responses & Schema Drift",
                scenario = scenario,
                problemDescription = "External campus feeds returned unexpected schema types (arrays instead of objects, truncated JSON, missing required fields).",
                reproductionSteps = listOf(
                    "1. Intercept remote feed response and inject malformed JSON: '{ \"events\": [ { \"title\": 404 } '.",
                    "2. Trigger event list parse and observe exception handling.",
                    "3. Check whether app crashes or renders safe fallback."
                ),
                evidence = RcaEvidence(
                    applicationLogs = listOf(
                        "[JSON_PARSER] JsonSyntaxException: Unterminated object at line 1 column 24",
                        "[DEFENSIVE_PARSER] Schema drift detected. Dropping corrupted payload chunk.",
                        "[DATA_GUARD] Retained previous verified SQLite dataset."
                    ),
                    errorMessage = "JsonSyntaxException: Malformed JSON payload received from remote feed",
                    apiResponseSnippet = "{ \"events\": [ { \"title\": 404, invalid_token... ",
                    databaseRecords = listOf(
                        "Table 'events': Unmodified; corrupt remote fields quarantined",
                        "Table 'audit_logs': 'SCHEMA_DRIFT_QUARANTINED'"
                    ),
                    configurationState = mapOf(
                        "STRICT_TYPE_ADAPTERS" to "ENABLED",
                        "PAYLOAD_QUARANTINE" to "ACTIVE"
                    ),
                    userActions = listOf(
                        "User opened Event Discovery",
                        "Background sync received malformed response"
                    )
                ),
                executionTrace = listOf(
                    RcaExecutionStep(1, "RemoteSyncWorker", "Fetches raw response string from remote feed", false, "Receives byte stream"),
                    RcaExecutionStep(2, "JsonDeserializer", "Attempts to parse response into Kotlin data classes", true, "Failure Point: JSON parser encountered unexpected token / syntax error"),
                    RcaExecutionStep(3, "DefensiveParser", "Catches JsonSyntaxException before polluting database", false, "Quarantines malformed payload"),
                    RcaExecutionStep(4, "DatabaseSync", "Preserves existing verified local records in Room", false, "Zero data corruption"),
                    RcaExecutionStep(5, "UiNotifier", "Notifies user: 'Sync failed, displaying verified cache'", false, "UI remains fully functional")
                ),
                rootCauseTechnicalCondition = "Unversioned API schema changes or network payload truncation causing deserialization crashes when parsing directly to entities.",
                impactAssessment = "Zero database corruption. Existing local records preserved intact without truncation.",
                correctiveActionImplemented = "Added try-catch wrappers around deserialization, schema drift quarantine, and fallback to cached Room records.",
                verificationMethod = "Injected truncated and type-mismatched JSON. Confirmed defensive catch and verified cache display."
            )
        }
    }
}
