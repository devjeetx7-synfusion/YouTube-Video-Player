package com.cineflow.player

import android.content.Context
import com.cineflow.player.crash.CrashReportData
import com.cineflow.player.crash.CrashReportStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CrashReportStoreTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    @After
    fun cleanup() {
        CrashReportStore.clearPendingReport(context)
    }

    @Test
    fun testSaveAndGetPendingReport() {
        assertFalse(CrashReportStore.hasPendingReport(context))

        val reportData = CrashReportData(
            timestamp = 1700000000000L,
            exceptionClass = "java.lang.NullPointerException",
            message = "Test exception message",
            stackTrace = "java.lang.NullPointerException: Test exception message\n\tat com.cineflow.player.TestClass.testMethod(TestClass.kt:10)",
            appVersion = "1.0 (1)",
            androidVersion = "Android 13 (API 33)",
            threadName = "main"
        )

        CrashReportStore.saveReport(context, reportData)

        assertTrue(CrashReportStore.hasPendingReport(context))

        val retrieved = CrashReportStore.getPendingReport(context)
        assertNotNull(retrieved)
        assertEquals(reportData.timestamp, retrieved?.timestamp)
        assertEquals(reportData.exceptionClass, retrieved?.exceptionClass)
        assertEquals(reportData.message, retrieved?.message)
        assertEquals(reportData.stackTrace, retrieved?.stackTrace)
        assertEquals(reportData.appVersion, retrieved?.appVersion)
        assertEquals(reportData.androidVersion, retrieved?.androidVersion)
        assertEquals(reportData.threadName, retrieved?.threadName)
    }

    @Test
    fun testClearPendingReport() {
        val reportData = CrashReportData(
            timestamp = System.currentTimeMillis(),
            exceptionClass = "java.lang.IllegalStateException",
            message = "Illegal state",
            stackTrace = "StackTrace",
            appVersion = "1.0 (1)",
            androidVersion = "Android 13 (API 33)",
            threadName = "main"
        )

        CrashReportStore.saveReport(context, reportData)
        assertTrue(CrashReportStore.hasPendingReport(context))

        CrashReportStore.clearPendingReport(context)
        assertFalse(CrashReportStore.hasPendingReport(context))
        assertNull(CrashReportStore.getPendingReport(context))
    }

    @Test
    fun testStackTraceTruncationForBoundedSize() {
        val hugeStackTrace = "A".repeat(40000)
        val reportData = CrashReportData(
            timestamp = System.currentTimeMillis(),
            exceptionClass = "java.lang.RuntimeException",
            message = "Huge trace",
            stackTrace = hugeStackTrace,
            appVersion = "1.0 (1)",
            androidVersion = "Android 13 (API 33)",
            threadName = "main"
        )

        CrashReportStore.saveReport(context, reportData)
        val retrieved = CrashReportStore.getPendingReport(context)
        assertNotNull(retrieved)
        assertTrue(retrieved!!.stackTrace.contains("... [stack trace truncated]"))
        assertTrue(retrieved.stackTrace.length < 40000)
    }
}
