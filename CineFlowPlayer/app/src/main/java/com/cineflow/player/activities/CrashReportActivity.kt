package com.cineflow.player.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cineflow.player.R
import com.cineflow.player.crash.CrashReportStore
import com.google.android.material.button.MaterialButton

class CrashReportActivity : AppCompatActivity() {

    private lateinit var tvCrashDetails: TextView
    private lateinit var btnCopy: MaterialButton
    private lateinit var btnShare: MaterialButton
    private lateinit var btnDismiss: MaterialButton
    private lateinit var btnRestart: MaterialButton

    private var reportText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash_report)

        tvCrashDetails = findViewById(R.id.tv_crash_details)
        btnCopy = findViewById(R.id.btn_copy)
        btnShare = findViewById(R.id.btn_share)
        btnDismiss = findViewById(R.id.btn_dismiss)
        btnRestart = findViewById(R.id.btn_restart)

        val report = CrashReportStore.getPendingReport(this)
        if (report != null) {
            reportText = report.toFormattedString()
            tvCrashDetails.text = reportText
        } else {
            tvCrashDetails.text = "No pending crash report found."
        }

        btnCopy.setOnClickListener {
            if (reportText.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("CineFlow Crash Report", reportText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Crash report copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        btnShare.setOnClickListener {
            if (reportText.isNotEmpty()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "CineFlow Player Crash Report")
                    putExtra(Intent.EXTRA_TEXT, reportText)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Crash Report"))
            }
        }

        btnDismiss.setOnClickListener {
            CrashReportStore.clearPendingReport(this)
            finish()
        }

        btnRestart.setOnClickListener {
            CrashReportStore.clearPendingReport(this)
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
