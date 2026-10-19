package com.cineflow.player.crash

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

data class CrashReportData(
    val timestamp: Long,
    val exceptionClass: String,
    val message: String?,
    val stackTrace: String,
    val appVersion: String,
    val androidVersion: String,
    val threadName: String,
    val exitReason: String? = null
) {
    fun toFormattedString(): String {
        val sb = StringBuilder()
        sb.append("=== CineFlow Player Crash Report ===\n")
        sb.append("Timestamp: ").append(java.util.Date(timestamp)).append("\n")
        sb.append("App Version: ").append(appVersion).append("\n")
        sb.append("OS Version: ").append(androidVersion).append("\n")
        sb.append("Thread Name: ").append(threadName).append("\n")
        if (!exitReason.isNullOrBlank()) {
            sb.append("Exit Reason: ").append(exitReason).append("\n")
        }
        sb.append("Exception: ").append(exceptionClass).append("\n")
        sb.append("Message: ").append(message ?: "No message").append("\n\n")
        sb.append("--- Stack Trace ---\n")
        sb.append(stackTrace)
        return sb.toString()
    }
}

object CrashReportStore {

    private const val TAG = "CrashReportStore"
    private const val REPORT_FILE_NAME = "pending_crash_report.json"
    private const val MAX_STACK_TRACE_LENGTH = 30000 // Limit stack trace to ~30KB

    fun saveReport(context: Context, report: CrashReportData) {
        synchronized(this) {
            try {
                val truncatedStackTrace = if (report.stackTrace.length > MAX_STACK_TRACE_LENGTH) {
                    report.stackTrace.substring(0, MAX_STACK_TRACE_LENGTH) + "\n... [stack trace truncated]"
                } else {
                    report.stackTrace
                }

                val json = JSONObject().apply {
                    put("timestamp", report.timestamp)
                    put("exceptionClass", report.exceptionClass)
                    put("message", report.message ?: "")
                    put("stackTrace", truncatedStackTrace)
                    put("appVersion", report.appVersion)
                    put("androidVersion", report.androidVersion)
                    put("threadName", report.threadName)
                    put("exitReason", report.exitReason ?: "")
                }

                val file = File(context.filesDir, REPORT_FILE_NAME)
                val tempFile = File(context.filesDir, "$REPORT_FILE_NAME.tmp")
                tempFile.writeText(json.toString(2), Charsets.UTF_8)
                if (tempFile.exists()) {
                    if (file.exists()) {
                        file.delete()
                    }
                    tempFile.renameTo(file)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save crash report synchronously", e)
            }
            Unit
        }
    }

    fun getPendingReport(context: Context): CrashReportData? {
        synchronized(this) {
            val file = File(context.filesDir, REPORT_FILE_NAME)
            if (!file.exists()) {
                return null
            }
            return try {
                val jsonString = file.readText(Charsets.UTF_8)
                val json = JSONObject(jsonString)

                val msgStr = json.optString("message", "")
                val messageVal = if (msgStr.isEmpty()) null else msgStr

                val exitReasonStr = json.optString("exitReason", "")
                val exitReasonVal = if (exitReasonStr.isEmpty()) null else exitReasonStr

                CrashReportData(
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    exceptionClass = json.optString("exceptionClass", "UnknownException"),
                    message = messageVal,
                    stackTrace = json.optString("stackTrace", ""),
                    appVersion = json.optString("appVersion", "1.0"),
                    androidVersion = json.optString("androidVersion", "Unknown"),
                    threadName = json.optString("threadName", "main"),
                    exitReason = exitReasonVal
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read pending crash report", e)
                null
            }
        }
    }

    fun hasPendingReport(context: Context): Boolean {
        return getPendingReport(context) != null
    }

    fun clearPendingReport(context: Context) {
        synchronized(this) {
            try {
                val file = File(context.filesDir, REPORT_FILE_NAME)
                if (file.exists()) {
                    file.delete()
                }
                val tempFile = File(context.filesDir, "$REPORT_FILE_NAME.tmp")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear pending crash report", e)
            }
            Unit
        }
    }
}
