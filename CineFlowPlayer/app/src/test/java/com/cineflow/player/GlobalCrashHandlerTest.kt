package com.cineflow.player

import android.content.Context
import com.cineflow.player.crash.CrashReportStore
import com.cineflow.player.crash.GlobalCrashHandler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GlobalCrashHandlerTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    @After
    fun cleanup() {
        CrashReportStore.clearPendingReport(context)
    }

    @Test
    fun testUncaughtExceptionHandlingAndDelegation() {
        val delegated = AtomicBoolean(false)
        val dummyPreviousHandler = Thread.UncaughtExceptionHandler { _, _ ->
            delegated.set(true)
        }

        Thread.setDefaultUncaughtExceptionHandler(dummyPreviousHandler)
        GlobalCrashHandler.install(context)

        val handler = Thread.getDefaultUncaughtExceptionHandler()
        assertTrue(handler is GlobalCrashHandler)

        val testException = IllegalStateException("Test crash exception")
        val testThread = Thread.currentThread()

        handler?.uncaughtException(testThread, testException)

        assertTrue("Crash report should be saved in store", CrashReportStore.hasPendingReport(context))
        val report = CrashReportStore.getPendingReport(context)
        assertEquals("java.lang.IllegalStateException", report?.exceptionClass)
        assertEquals("Test crash exception", report?.message)

        assertTrue("Default handler delegation should have occurred", delegated.get())
    }
}
