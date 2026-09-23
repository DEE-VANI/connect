package com.example.data.resilience

/**
 * Models representing the 6-Step Deep Root-Cause Analysis (RCA) Process:
 * 1. Identify the Failure
 * 2. Reproduce the Issue
 * 3. Collect Evidence (logs, error messages, API responses, DB records, config, user actions)
 * 4. Trace the Failure (execution call stack / event flow)
 * 5. Identify Root Cause (underlying technical condition)
 * 6. Define & Verify Corrective Action (permanent solution)
 */
data class RcaEvidence(
    val applicationLogs: List<String>,
    val errorMessage: String,
    val apiResponseSnippet: String,
    val databaseRecords: List<String>,
    val configurationState: Map<String, String>,
    val userActions: List<String>
)

data class RcaExecutionStep(
    val stepNumber: Int,
    val component: String,
    val action: String,
    val isFailurePoint: Boolean,
    val technicalObservation: String
)

data class RootCauseAnalysisReport(
    val id: String,
    val failureTitle: String,
    val scenario: FailureScenario,
    val timestamp: Long = System.currentTimeMillis(),
    // 1. Identify the Failure
    val problemDescription: String,
    // 2. Reproduce the Issue
    val reproductionSteps: List<String>,
    // 3. Collect Evidence
    val evidence: RcaEvidence,
    // 4. Trace the Failure
    val executionTrace: List<RcaExecutionStep>,
    // 5. Identify Root Cause
    val rootCauseTechnicalCondition: String,
    val impactAssessment: String,
    // 6. Define Corrective Action
    val correctiveActionImplemented: String,
    val verificationMethod: String,
    val verificationStatus: Boolean = true
)
