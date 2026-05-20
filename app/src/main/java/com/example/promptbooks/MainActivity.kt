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
            Record(
                id = 0,
                date = "26 Apr 2026, 10:15 AM",
                account = "Sales Revenue",
                description = "Sale: custom notebooks (x20)",
                amount = -120.0,
                type = "sale",
                currency = "AED",
                counterpartyName = "Greenleaf Stationery LLC",
                counterpartyType = "Customer",
                paymentMode = "bank",
                isPaid = true,
                referenceNumber = "INV-2026-001",
                vatApplicable = true,
                vatRate = 5.0,
                vatAmount = 6.0,
                taxCode = "S5",
                createdAt = "2026-04-26T10:15:00",
                updatedAt = "2026-04-26T10:15:00",
                source = "AI",
                location = "Dubai, UAE",
                notes = "Bulk order, delivery included"
            ),
            Record(
                id = 0,
                date = "26 Apr 2026, 11:30 AM",
                account = "Sales Revenue",
                description = "Sale: acrylic logo sign",
                amount = -350.0,
                type = "sale",
                currency = "AED",
                counterpartyName = "Marco Retail Group",
                counterpartyType = "Customer",
                paymentMode = "cash",
                isPaid = true,
                referenceNumber = "INV-2026-002",
                vatApplicable = true,
                vatRate = 5.0,
                vatAmount = 17.5,
                taxCode = "S5",
                createdAt = "2026-04-26T11:30:00",
                updatedAt = "2026-04-26T11:30:00",
                source = "AI",
                location = "Sharjah, UAE",
                notes = null
            ),
            Record(
                id = 0,
                date = "26 Apr 2026, 01:45 PM",
                account = "Cost of Goods",
                description = "Purchase: plywood sheets (10 pcs)",
                amount = 250.0,
                type = "purchase",
                currency = "AED",
                counterpartyName = "Al Futtaim Building Materials",
                counterpartyType = "Supplier",
                paymentMode = "bank",
                isPaid = true,
                referenceNumber = "PO-2026-018",
                vatApplicable = true,
                vatRate = 5.0,
                vatAmount = 12.5,
                taxCode = "S5",
                createdAt = "2026-04-26T13:45:00",
                updatedAt = "2026-04-26T13:45:00",
                source = "AI",
                location = "Abu Dhabi, UAE",
                notes = "18mm birch ply"
            ),
            Record(
                id = 0,
                date = "27 Apr 2026, 09:10 AM",
                account = "Travel & Transport",
                description = "Expense: taxi to supplier meeting",
                amount = 80.0,
                type = "expense",
                currency = "AED",
                counterpartyName = null,
                counterpartyType = null,
                paymentMode = "cash",
                isPaid = true,
                referenceNumber = null,
                vatApplicable = false,
                vatRate = null,
                vatAmount = null,
                taxCode = "EX",
                createdAt = "2026-04-27T09:10:00",
                updatedAt = "2026-04-27T09:10:00",
                source = "AI",
                location = "Dubai, UAE",
                notes = "Careem ride to DIFC"
            ),
            Record(
                id = 0,
                date = "27 Apr 2026, 10:00 AM",
                account = "Income",
                description = "Income: freelance design payment",
                amount = -450.0,
                type = "income",
                currency = "AED",
                counterpartyName = "Ahmed Al Mansoori",
                counterpartyType = "Customer",
                paymentMode = "bank",
                isPaid = true,
                referenceNumber = "TXN-9921",
                vatApplicable = false,
                vatRate = null,
                vatAmount = null,
                taxCode = null,
                createdAt = "2026-04-27T10:00:00",
                updatedAt = "2026-04-27T10:00:00",
                source = "AI",
                location = null,
                notes = "Logo redesign project"
            ),
            Record(
                id = 0,
                date = "27 Apr 2026, 02:20 PM",
                account = "Packaging & Supplies",
                description = "Expense: bubble wrap and boxes",
                amount = 45.0,
                type = "expense",
                currency = "AED",
                counterpartyName = "PackItUAE",
                counterpartyType = "Supplier",
                paymentMode = "cash",
                isPaid = true,
                referenceNumber = null,
                vatApplicable = false,
                vatRate = null,
                vatAmount = null,
                taxCode = "EX",
                createdAt = "2026-04-27T14:20:00",
                updatedAt = "2026-04-27T14:20:00",
                source = "AI",
                location = "Ajman, UAE",
                notes = null
            ),
            Record(
                id = 0,
                date = "28 Apr 2026, 11:15 AM",
                account = "Cost of Goods",
                description = "Purchase: PLA filament spool 1kg",
                amount = 75.0,
                type = "purchase",
                currency = "AED",
                counterpartyName = "3D Supplies FZE",
                counterpartyType = "Supplier",
                paymentMode = "credit",
                isPaid = false,
                referenceNumber = "PO-2026-019",
                vatApplicable = true,
                vatRate = 5.0,
                vatAmount = 3.75,
                taxCode = "S5",
                createdAt = "2026-04-28T11:15:00",
                updatedAt = "2026-04-28T11:15:00",
                source = "AI",
                location = "Dubai Silicon Oasis",
                notes = "White PLA, 1.75mm"
            ),
            Record(
                id = 0,
                date = "28 Apr 2026, 04:30 PM",
                account = "Sales Revenue",
                description = "Sale: corporate gift box set (x5)",
                amount = -600.0,
                type = "sale",
                currency = "AED",
                counterpartyName = "Horizon Events Management",
                counterpartyType = "Customer",
                paymentMode = "cheque",
                isPaid = false,
                referenceNumber = "INV-2026-003",
                vatApplicable = true,
                vatRate = 5.0,
                vatAmount = 30.0,
                taxCode = "S5",
                createdAt = "2026-04-28T16:30:00",
                updatedAt = "2026-04-28T16:30:00",
                source = "AI",
                location = "Dubai, UAE",
                notes = "Cheque due 15 May 2026"
            ),
            Record(
                id = 0,
                date = "29 Apr 2026, 08:45 AM",
                account = "Sales Revenue",
                description = "Sale: custom rubber stamps (x8)",
                amount = -180.0,
                type = "sale",
                currency = "AED",
                counterpartyName = "Sunrise Pharmacy LLC",
                counterpartyType = "Customer",
                paymentMode = "bank",
                isPaid = true,
                referenceNumber = "INV-2026-004",
                vatApplicable = false,
                vatRate = 0.0,
                vatAmount = 0.0,
                taxCode = "ZR",
                createdAt = "2026-04-29T08:45:00",
                updatedAt = "2026-04-29T08:45:00",
                source = "AI",
                location = "Ras Al Khaimah",
                notes = "VAT exempt — medical supplies"
            ),
            Record(
                id = 0,
                date = "29 Apr 2026, 12:15 PM",
                account = "Entertainment",
                description = "Expense: client lunch meeting",
                amount = 35.0,
                type = "expense",
                currency = "AED",
                counterpartyName = "Nour Hassan",
                counterpartyType = "Customer",
                paymentMode = "cash",
                isPaid = true,
                referenceNumber = null,
                vatApplicable = false,
                vatRate = null,
                vatAmount = null,
                taxCode = "EX",
                createdAt = "2026-04-29T12:15:00",
                updatedAt = "2026-04-29T12:15:00",
                source = "AI",
                location = "Dubai Mall, Dubai",
                notes = "Discussion re. Q3 branding project"
            )
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
