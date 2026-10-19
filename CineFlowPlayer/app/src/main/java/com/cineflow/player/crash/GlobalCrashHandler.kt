package com.cineflow.player.crash

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

class GlobalCrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashReport(thread, throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Error in GlobalCrashHandler while persisting exception", e)
        } finally {
            // Delegate to the previous default uncaught exception handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashReport(thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTraceString = sw.toString()

        val appVersion = getAppVersionString(context)
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        val report = CrashReportData(
            timestamp = System.currentTimeMillis(),
            exceptionClass = throwable.javaClass.name,
            message = throwable.message,
            stackTrace = stackTraceString,
            appVersion = appVersion,
            androidVersion = androidVersion,
            threadName = thread.name
        )

        CrashReportStore.saveReport(context, report)
    }

    companion object {
        private const val TAG = "GlobalCrashHandler"

        fun install(context: Context) {
            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler !is GlobalCrashHandler) {
                val handler = GlobalCrashHandler(context.applicationContext, currentHandler)
                Thread.setDefaultUncaughtExceptionHandler(handler)
                Log.d(TAG, "GlobalCrashHandler successfully installed")
            }
        }

        fun getAppVersionString(context: Context): String {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                "${packageInfo.versionName} ($versionCode)"
            } catch (e: Exception) {
                "1.0 (1)"
            }
        }
    }
}
