package com.example.promptbooks

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvBankBalance: TextView
    private lateinit var tvSales: TextView
    private lateinit var containerTransactions: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var btnNewChat: TextView
    private lateinit var btnClearData: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvBankBalance = view.findViewById(R.id.tvBankBalance)
        tvSales = view.findViewById(R.id.tvSales)
        containerTransactions = view.findViewById(R.id.containerTransactions)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        btnNewChat = view.findViewById(R.id.btnNewChat)
        btnClearData = view.findViewById(R.id.btnClearData)

        btnNewChat.setOnClickListener {
            showNewChatConfirmation()
        }

        btnClearData.setOnClickListener {
            showClearDataConfirmation()
        }

        loadDashboardData()

        return view
    }

    private fun showNewChatConfirmation() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Start new chat?")
            .setMessage("This will clear the current chat messages but keep saved transactions.")
            .setPositiveButton("New Chat") { _, _ ->
                (activity as? MainActivity)?.getChatFragment()?.clearChatSession()
                Toast.makeText(context, "Chat cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
        
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
    }

    private fun showClearDataConfirmation() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Clear data?")
            .setMessage("This will delete all saved transactions.")
            .setPositiveButton("Clear") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).recordDao().deleteAll()
                    loadDashboardData()
                    Toast.makeText(context, "Data cleared", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            loadDashboardData()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    fun loadDashboardData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val records = db.recordDao().getAllRecords()

            if (records.isEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                tvBankBalance.text = "0 DHS"
                tvSales.text = "0 DHS"
                containerTransactions.removeAllViews()
                return@launch
            }

            tvEmptyState.visibility = View.GONE
            
            var totalSales = 0.0
            var bankBalance = 0.0

            containerTransactions.removeAllViews()

            records.forEach { record ->
                bankBalance += -record.amount
                if (record.description.startsWith("Sale:", ignoreCase = true)) {
                    totalSales += -record.amount
                }
                addTransactionRow(record)
            }

            tvBankBalance.text = "${formatNumber(bankBalance)} DHS"
            tvSales.text = "${formatNumber(totalSales)} DHS"
        }
    }

    private fun addTransactionRow(record: Record) {
        val context = requireContext()
        
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val density = resources.displayMetrics.density
            val px16 = (16 * density).toInt()
            val px12 = (12 * density).toInt()
            setPadding(px16, px12, px16, px12)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Icon/Indicator
        val indicator = View(context).apply {
            val size = (12 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = (16 * resources.displayMetrics.density).toInt()
            }
            val isPositive = record.amount < 0
            background = ContextCompat.getDrawable(context, if (isPositive) R.drawable.bg_dot_green else R.drawable.bg_dot_red)
        }

        val infoLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleText = TextView(context).apply {
            text = record.description
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setTypeface(null, Typeface.BOLD)
        }

        val dateText = TextView(context).apply {
            text = record.date
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        infoLayout.addView(titleText)
        infoLayout.addView(dateText)

        val amountText = TextView(context).apply {
            val displayAmount = if (record.amount < 0) -record.amount else record.amount
            text = "${if (record.amount < 0) "+" else "-"}${formatNumber(displayAmount)} DHS"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, if (record.amount < 0) R.color.income_green else R.color.expense_red))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
            setPadding(0, 0, 16, 0)
        }

        val moreBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_more_vert)
            val size = (32 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            setPadding(4, 4, 4, 4)
            background = ContextCompat.getDrawable(context, R.drawable.bg_ripple_borderless)
            isClickable = true
            isFocusable = true
            contentDescription = "Options"
            setOnClickListener { view ->
                showRowMenu(view, record)
            }
        }

        row.addView(indicator)
        row.addView(infoLayout)
        row.addView(amountText)
        row.addView(moreBtn)

        containerTransactions.addView(row)

        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * resources.displayMetrics.density).toInt()
            )
            setBackgroundColor(ContextCompat.getColor(context, R.color.divider_gray))
        }
        containerTransactions.addView(divider)
    }

    private fun showRowMenu(view: View, record: Record) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add("Delete")
        popup.setOnMenuItemClickListener {
            showDeleteConfirmation(record)
            true
        }
        popup.show()
    }

    private fun showDeleteConfirmation(record: Record) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Delete transaction?")
            .setMessage("This transaction will be removed.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).recordDao().deleteById(record.id)
                    loadDashboardData()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
        
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
    }

    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}
