package com.cineflow.player

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.cineflow.player.crash.CrashReportData
import com.cineflow.player.crash.CrashReportStore
import com.cineflow.player.crash.GlobalCrashHandler

class CineFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        GlobalCrashHandler.install(this)
        checkHistoricalProcessExits()
    }

    private fun checkHistoricalProcessExits() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (CrashReportStore.hasPendingReport(this)) {
                    return
                }

                val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val exits = am?.getHistoricalProcessExitReasons(packageName, 0, 1)
                if (!exits.isNullOrEmpty()) {
                    val lastExit = exits[0]
                    val reason = lastExit.reason
                    val isCrashExit = reason == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE ||
                            reason == android.app.ApplicationExitInfo.REASON_CRASH ||
                            reason == android.app.ApplicationExitInfo.REASON_ANR

                    if (isCrashExit) {
                        val reasonStr = when (reason) {
                            android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native Crash (REASON_CRASH_NATIVE)"
                            android.app.ApplicationExitInfo.REASON_CRASH -> "Fatal Exception (REASON_CRASH)"
                            android.app.ApplicationExitInfo.REASON_ANR -> "Application Not Responding (REASON_ANR)"
                            else -> "Process Exit (Reason $reason)"
                        }

                        val description = lastExit.description ?: "Process exit recorded by system."
                        val appVersion = GlobalCrashHandler.getAppVersionString(this)
                        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

                        val report = CrashReportData(
                            timestamp = lastExit.timestamp,
                            exceptionClass = "ProcessExitInfo",
                            message = description,
                            stackTrace = "Process exit details: $description\nExit Status: ${lastExit.status}\nImportance: ${lastExit.importance}",
                            appVersion = appVersion,
                            androidVersion = androidVersion,
                            threadName = "main",
                            exitReason = reasonStr
                        )

                        CrashReportStore.saveReport(this, report)
                    }
                }
            } catch (e: Exception) {
                Log.w("CineFlowApplication", "Could not check historical process exit info: ${e.message}")
            }
        }
    }
}
