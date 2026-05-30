package com.example.promptbooks

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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.promptbooks.AppDatabase
import com.example.promptbooks.Record

class MainActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var tabChat: LinearLayout
    private lateinit var tabDashboard: LinearLayout
    private lateinit var iconChat: ImageView
    private lateinit var iconDashboard: ImageView
    private lateinit var labelChat: TextView
    private lateinit var labelDashboard: TextView

    private val chatFragment = ChatFragment()
    private val dashboardFragment = DashboardFragment()
    private var activeFragment: Fragment = chatFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge rendering
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        titleText = findViewById(R.id.title)
        tabChat = findViewById(R.id.tabChat)
        tabDashboard = findViewById(R.id.tabDashboard)
        iconChat = findViewById(R.id.iconChat)
        iconDashboard = findViewById(R.id.iconDashboard)
        labelChat = findViewById(R.id.labelChat)
        labelDashboard = findViewById(R.id.labelDashboard)

        // Apply window insets: status bar → header, nav bar → bottom tab bar
        val header = findViewById<LinearLayout>(R.id.header)
        val bottomTabBar = findViewById<LinearLayout>(R.id.bottomTabBar)

        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(bottomTabBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            insets
        }

        setupTitle()

        supportFragmentManager.beginTransaction().apply {
            add(R.id.container, dashboardFragment, "dashboard").hide(dashboardFragment)
            add(R.id.container, chatFragment, "chat")
            commit()
        }
        updateTabUI(isChatActive = true)

        tabChat.setOnClickListener {
            showFragment(chatFragment)
            updateTabUI(isChatActive = true)
        }

        tabDashboard.setOnClickListener {
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
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .hide(activeFragment)
                .show(fragment)
                .commit()
            activeFragment = fragment
        }
    }

    fun getChatFragment(): ChatFragment = chatFragment

    private fun showSupportDialog() {
        val options = arrayOf("Open LinkedIn", "Generate Sample Data", "Clear All Data", "Cancel")
        val dialog = AlertDialog.Builder(this)
            .setTitle("Support")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val link = "https://www.linkedin.com/in/joravarsingh/"
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        } catch (e: Exception) {
                            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> generateSampleData()
                    2 -> (dashboardFragment as? DashboardFragment)?.showClearDataConfirmation()
                    3 -> {}
                }
            }
            .create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
        dialog.show()
    }

    private fun generateSampleData() {
        val samples = listOf(
            Record(id = 0, date = "26 Apr 2026, 10:15 AM", account = "Sales Revenue", description = "Sale: custom notebooks (x20)", amount = -120.0, type = "sale", currency = "AED", counterpartyName = "Greenleaf Stationery LLC", counterpartyType = "Customer", paymentMode = "bank", isPaid = true, referenceNumber = "INV-2026-001", vatApplicable = true, vatRate = 5.0, vatAmount = 6.0, taxCode = "S5", createdAt = "2026-04-26T10:15:00", updatedAt = "2026-04-26T10:15:00", source = "AI", location = "Dubai, UAE", notes = "Bulk order, delivery included"),
            Record(id = 0, date = "26 Apr 2026, 11:30 AM", account = "Sales Revenue", description = "Sale: acrylic logo sign", amount = -350.0, type = "sale", currency = "AED", counterpartyName = "Marco Retail Group", counterpartyType = "Customer", paymentMode = "cash", isPaid = true, referenceNumber = "INV-2026-002", vatApplicable = true, vatRate = 5.0, vatAmount = 17.5, taxCode = "S5", createdAt = "2026-04-26T11:30:00", updatedAt = "2026-04-26T11:30:00", source = "AI", location = "Sharjah, UAE", notes = null),
            Record(id = 0, date = "26 Apr 2026, 01:45 PM", account = "Cost of Goods", description = "Purchase: plywood sheets (10 pcs)", amount = 250.0, type = "purchase", currency = "AED", counterpartyName = "Al Futtaim Building Materials", counterpartyType = "Supplier", paymentMode = "bank", isPaid = true, referenceNumber = "PO-2026-018", vatApplicable = true, vatRate = 5.0, vatAmount = 12.5, taxCode = "S5", createdAt = "2026-04-26T13:45:00", updatedAt = "2026-04-26T13:45:00", source = "AI", location = "Abu Dhabi, UAE", notes = "18mm birch ply"),
            Record(id = 0, date = "27 Apr 2026, 09:10 AM", account = "Travel & Transport", description = "Expense: taxi to supplier meeting", amount = 80.0, type = "expense", currency = "AED", counterpartyName = null, counterpartyType = null, paymentMode = "cash", isPaid = true, referenceNumber = null, vatApplicable = false, vatRate = null, vatAmount = null, taxCode = "EX", createdAt = "2026-04-27T09:10:00", updatedAt = "2026-04-27T09:10:00", source = "AI", location = "Dubai, UAE", notes = "Careem ride to DIFC"),
            Record(id = 0, date = "27 Apr 2026, 10:00 AM", account = "Income", description = "Income: freelance design payment", amount = -450.0, type = "income", currency = "AED", counterpartyName = "Ahmed Al Mansoori", counterpartyType = "Customer", paymentMode = "bank", isPaid = true, referenceNumber = "TXN-9921", vatApplicable = false, vatRate = null, vatAmount = null, taxCode = null, createdAt = "2026-04-27T10:00:00", updatedAt = "2026-04-27T10:00:00", source = "AI", location = null, notes = "Logo redesign project"),
            Record(id = 0, date = "27 Apr 2026, 02:20 PM", account = "Packaging & Supplies", description = "Expense: bubble wrap and boxes", amount = 45.0, type = "expense", currency = "AED", counterpartyName = "PackItUAE", counterpartyType = "Supplier", paymentMode = "cash", isPaid = true, referenceNumber = null, vatApplicable = false, vatRate = null, vatAmount = null, taxCode = "EX", createdAt = "2026-04-27T14:20:00", updatedAt = "2026-04-27T14:20:00", source = "AI", location = "Ajman, UAE", notes = null),
            Record(id = 0, date = "28 Apr 2026, 11:15 AM", account = "Cost of Goods", description = "Purchase: PLA filament spool 1kg", amount = 75.0, type = "purchase", currency = "AED", counterpartyName = "3D Supplies FZE", counterpartyType = "Supplier", paymentMode = "credit", isPaid = false, referenceNumber = "PO-2026-019", vatApplicable = true, vatRate = 5.0, vatAmount = 3.75, taxCode = "S5", createdAt = "2026-04-28T11:15:00", updatedAt = "2026-04-28T11:15:00", source = "AI", location = "Dubai Silicon Oasis", notes = "White PLA, 1.75mm"),
            Record(id = 0, date = "28 Apr 2026, 04:30 PM", account = "Sales Revenue", description = "Sale: corporate gift box set (x5)", amount = -600.0, type = "sale", currency = "AED", counterpartyName = "Horizon Events Management", counterpartyType = "Customer", paymentMode = "cheque", isPaid = false, referenceNumber = "INV-2026-003", vatApplicable = true, vatRate = 5.0, vatAmount = 30.0, taxCode = "S5", createdAt = "2026-04-28T16:30:00", updatedAt = "2026-04-28T16:30:00", source = "AI", location = "Dubai, UAE", notes = "Cheque due 15 May 2026"),
            Record(id = 0, date = "29 Apr 2026, 08:45 AM", account = "Sales Revenue", description = "Sale: custom rubber stamps (x8)", amount = -180.0, type = "sale", currency = "AED", counterpartyName = "Sunrise Pharmacy LLC", counterpartyType = "Customer", paymentMode = "bank", isPaid = true, referenceNumber = "INV-2026-004", vatApplicable = false, vatRate = 0.0, vatAmount = 0.0, taxCode = "ZR", createdAt = "2026-04-29T08:45:00", updatedAt = "2026-04-29T08:45:00", source = "AI", location = "Ras Al Khaimah", notes = "VAT exempt — medical supplies"),
            Record(id = 0, date = "29 Apr 2026, 12:15 PM", account = "Entertainment", description = "Expense: client lunch meeting", amount = 35.0, type = "expense", currency = "AED", counterpartyName = "Nour Hassan", counterpartyType = "Customer", paymentMode = "cash", isPaid = true, referenceNumber = null, vatApplicable = false, vatRate = null, vatAmount = null, taxCode = "EX", createdAt = "2026-04-29T12:15:00", updatedAt = "2026-04-29T12:15:00", source = "AI", location = "Dubai Mall, Dubai", notes = "Discussion re. Q3 branding project")
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@MainActivity)
            samples.forEach { db.recordDao().insertRecord(it) }
            Toast.makeText(this@MainActivity, "Sample data added", Toast.LENGTH_SHORT).show()
            if (activeFragment == dashboardFragment) {
                (dashboardFragment as? DashboardFragment)?.loadDashboardData()
            }
        }
    }

    private fun setupTitle() {
        val fullText = "PromptBooks"
        val spannable = SpannableString(fullText)
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_primary)),
            0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_blue)),
            6, fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        titleText.text = spannable
    }

    private fun updateTabUI(isChatActive: Boolean) {
        val activeColor = ContextCompat.getColor(this, R.color.tab_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.tab_inactive)

        if (isChatActive) {
            iconChat.setColorFilter(activeColor)
            labelChat.setTextColor(activeColor)
            labelChat.setTypeface(null, Typeface.BOLD)

            iconDashboard.setColorFilter(inactiveColor)
            labelDashboard.setTextColor(inactiveColor)
            labelDashboard.setTypeface(null, Typeface.NORMAL)
        } else {
            iconDashboard.setColorFilter(activeColor)
            labelDashboard.setTextColor(activeColor)
            labelDashboard.setTypeface(null, Typeface.BOLD)

            iconChat.setColorFilter(inactiveColor)
            labelChat.setTextColor(inactiveColor)
            labelChat.setTypeface(null, Typeface.NORMAL)
        }
    }
}
