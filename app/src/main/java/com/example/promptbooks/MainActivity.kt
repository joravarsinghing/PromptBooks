package com.example.promptbooks

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var btnChat: TextView
    private lateinit var btnDashboard: TextView
    private lateinit var titleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        titleText = findViewById(R.id.title)
        btnChat = findViewById(R.id.btnChat)
        btnDashboard = findViewById(R.id.btnDashboard)

        setupTitle()

        // Load ChatFragment by default
        loadFragment(ChatFragment())
        updateTabUI(isChatActive = true)

        btnChat.setOnClickListener {
            loadFragment(ChatFragment())
            updateTabUI(isChatActive = true)
        }

        btnDashboard.setOnClickListener {
            loadFragment(DashboardFragment())
            updateTabUI(isChatActive = false)
        }

        findViewById<ImageView>(R.id.btnSupport).setOnClickListener {
            showSupportDialog()
        }
    }

    private fun showSupportDialog() {
        AlertDialog.Builder(this)
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
            .setNegativeButton("Cancel", null)
            .show()
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
        if (isChatActive) {
            btnChat.background = ContextCompat.getDrawable(this, R.drawable.bg_tab_selected)
            btnChat.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnChat.setTypeface(null, Typeface.BOLD)
            
            btnDashboard.background = null
            btnDashboard.setTextColor(ContextCompat.getColor(this, R.color.primary_blue))
            btnDashboard.setTypeface(null, Typeface.NORMAL)
        } else {
            btnDashboard.background = ContextCompat.getDrawable(this, R.drawable.bg_tab_selected)
            btnDashboard.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnDashboard.setTypeface(null, Typeface.BOLD)
            
            btnChat.background = null
            btnChat.setTextColor(ContextCompat.getColor(this, R.color.primary_blue))
            btnChat.setTypeface(null, Typeface.NORMAL)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
