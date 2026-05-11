package com.example.promptbooks

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.promptbooks.AppDatabase
import com.example.promptbooks.Record

class MainActivity : AppCompatActivity() {

    private lateinit var btnChat: TextView
    private lateinit var btnDashboard: TextView
    private lateinit var titleText: TextView
    private lateinit var selector: View
    private var chatWidth = 0
    private var dashboardWidth = 0

    private val chatFragment = ChatFragment()
    private val dashboardFragment = DashboardFragment()
    private var activeFragment: Fragment = chatFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_main)

        @Suppress("DEPRECATION")
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_light)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        titleText = findViewById(R.id.title)
        btnChat = findViewById(R.id.btnChat)
        btnDashboard = findViewById(R.id.btnDashboard)
        selector = findViewById(R.id.selector)

        btnChat.post {
            chatWidth = btnChat.width
            dashboardWidth = btnDashboard.width
            
            val params = selector.layoutParams
            params.width = chatWidth
            selector.layoutParams = params
        }

        setupTitle()

        // Initialize fragments
        supportFragmentManager.beginTransaction().apply {
            add(R.id.container, dashboardFragment, "dashboard").hide(dashboardFragment)
            add(R.id.container, chatFragment, "chat")
            commit()
        }
        updateTabUI(isChatActive = true)

        btnChat.setOnClickListener {
            showFragment(chatFragment)
            updateTabUI(isChatActive = true)
        }

        btnDashboard.setOnClickListener {
            showFragment(dashboardFragment)
            updateTabUI(isChatActive = false)
        }

        findViewById<ImageView>(R.id.btnSupport).setOnClickListener {
            showSupportDialog()
        }
    }

    private fun showFragment(fragment: Fragment) {
        if (activeFragment != fragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(fragment)
                .commit()
            activeFragment = fragment
        }
    }

    fun getChatFragment(): ChatFragment = chatFragment

    private fun showSupportDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Need help?")
            .setMessage("Contact Joravar Singh on LinkedIn")
            .setPositiveButton("Open LinkedIn") { _, _ ->
                val link = "https://www.linkedin.com/in/joravarsingh/"
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Generate Sample Data") { _, _ ->
                generateSampleData()
            }
            .setNegativeButton("Cancel", null)
            .show()
        
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
    }

    private fun generateSampleData() {
        val samples = listOf(
            Record(0, "26 Apr 2026, 10:15 AM", "Income", "Sale: notebooks", -120.0),
            Record(0, "26 Apr 2026, 11:30 AM", "Income", "Sale: acrylic sign", -350.0),
            Record(0, "26 Apr 2026, 01:45 PM", "Expenses", "Purchase: plywood sheets", 250.0),
            Record(0, "27 Apr 2026, 09:10 AM", "Expenses", "Expense: taxi ride", 80.0),
            Record(0, "27 Apr 2026, 10:00 AM", "Income", "Income: Ahmed", -120.0),
            Record(0, "27 Apr 2026, 02:20 PM", "Expenses", "Expense: packaging", 45.0),
            Record(0, "28 Apr 2026, 11:15 AM", "Expenses", "Purchase: filament spool", 75.0),
            Record(0, "28 Apr 2026, 04:30 PM", "Income", "Sale: corporate gift box", -600.0),
            Record(0, "29 Apr 2026, 08:45 AM", "Income", "Sale: custom stamps", -180.0),
            Record(0, "29 Apr 2026, 12:15 PM", "Expenses", "Expense: coffee with client", 35.0)
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@MainActivity)
            samples.forEach { db.recordDao().insertRecord(it) }
            Toast.makeText(this@MainActivity, "Sample data added", Toast.LENGTH_SHORT).show()
            
            // Refresh dashboard if active
            if (activeFragment == dashboardFragment) {
                (dashboardFragment as? DashboardFragment)?.loadDashboardData()
            }
        }
    }

    private fun setupTitle() {
        val fullText = "PromptBooks"
        val spannable = SpannableString(fullText)
        
        // "Prompt" in dark color
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_primary)),
            0, 6,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        // "Books" in theme color
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_blue)),
            6, fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        titleText.text = spannable
    }

    private fun updateTabUI(isChatActive: Boolean) {
        val selectedTab = if (isChatActive) btnChat else btnDashboard
        
        if (isChatActive) {
            btnChat.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnChat.setTypeface(null, Typeface.BOLD)
            
            btnDashboard.setTextColor(ContextCompat.getColor(this, R.color.primary_blue))
            btnDashboard.setTypeface(null, Typeface.NORMAL)
        } else {
            btnDashboard.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnDashboard.setTypeface(null, Typeface.BOLD)
            
            btnChat.setTextColor(ContextCompat.getColor(this, R.color.primary_blue))
            btnChat.setTypeface(null, Typeface.NORMAL)
        }

        selectedTab.post {
            val targetX = selectedTab.left.toFloat()
            val targetWidth = selectedTab.width

            // Animate position
            selector.animate()
                .translationX(targetX)
                .setDuration(250)
                .start()

            // Animate width
            val animator = ValueAnimator.ofInt(selector.width, targetWidth)
            animator.addUpdateListener {
                val params = selector.layoutParams
                params.width = it.animatedValue as Int
                selector.layoutParams = params
            }
            animator.duration = 250
            animator.start()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
