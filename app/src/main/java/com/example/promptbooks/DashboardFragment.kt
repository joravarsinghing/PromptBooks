package com.example.promptbooks

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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

        loadDashboardData()

        return view
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val records = db.recordDao().getAllRecords()

            if (records.isEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                tvBankBalance.text = "0 DHS"
                tvSales.text = "0 DHS"
                return@launch
            }

            tvEmptyState.visibility = View.GONE
            
            var totalSales = 0.0
            var bankBalance = 0.0

            containerTransactions.removeAllViews()

            records.forEach { record ->
                // Calculation logic based on ChatFragment save logic:
                // Income/Sale are saved as negative amounts (e.g., -500 for a 500 sale)
                // Expenses/Purchase are saved as positive amounts (e.g., 200 for a 200 expense)
                // To get the real balance: bankBalance = sum of (-amount)
                
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
            setPadding(16, 24, 16, 24)
        }

        // Icon/Indicator
        val indicator = View(context).apply {
            val size = (12 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginEnd = (16 * resources.displayMetrics.density).toInt()
            }
            val isPositive = record.amount < 0 // Income/Sale are negative in DB
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
        }

        row.addView(indicator)
        row.addView(infoLayout)
        row.addView(amountText)

        containerTransactions.addView(row)

        // Divider
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * resources.displayMetrics.density).toInt()
            )
            setBackgroundColor(ContextCompat.getColor(context, R.color.divider_gray))
        }
        containerTransactions.addView(divider)
    }

    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}
