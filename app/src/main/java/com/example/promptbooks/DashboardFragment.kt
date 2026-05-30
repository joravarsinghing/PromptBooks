package com.example.promptbooks

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvNetProfit: TextView
    private lateinit var tvSales: TextView
    private lateinit var tvCurrentBalance: TextView
    private lateinit var chartBalanceTrend: LineChart
    private lateinit var containerTransactions: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var btnNewChat: TextView
    private lateinit var btnExportData: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvNetProfit = view.findViewById(R.id.tvNetProfit)
        tvSales = view.findViewById(R.id.tvSales)
        tvCurrentBalance = view.findViewById(R.id.tvCurrentBalance)
        chartBalanceTrend = view.findViewById(R.id.chartBalanceTrend)
        containerTransactions = view.findViewById(R.id.containerTransactions)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        btnNewChat = view.findViewById(R.id.btnNewChat)
        btnExportData = view.findViewById(R.id.btnExportData)

        btnNewChat.setOnClickListener { showNewChatConfirmation() }
        btnExportData.setOnClickListener { exportData() }

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

    fun showClearDataConfirmation() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Clear all transactions?")
            .setMessage("This will delete all saved transactions. This cannot be undone.")
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

    private fun exportData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val records = db.recordDao().getAllRecords()

            if (records.isEmpty()) {
                Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "promptbooks_export_$timestamp.json"
                
                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonString = gson.toJson(records)

                val exportDir = File(requireContext().cacheDir, "exports")
                if (!exportDir.exists()) exportDir.mkdirs()
                
                val file = File(exportDir, fileName)
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { 
                        it.write(jsonString.toByteArray())
                    }
                }

                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(Intent.createChooser(intent, "Export Transactions"))
                Toast.makeText(context, "Export ready", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) loadDashboardData()
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
                tvNetProfit.text = "0 AED"
                tvSales.text = "0 AED"
                tvCurrentBalance.text = "0 AED"
                containerTransactions.removeAllViews()
                chartBalanceTrend.clear()
                chartBalanceTrend.setNoDataText("No transactions to show")
                return@launch
            }

            tvEmptyState.visibility = View.GONE

            val totalIncome = records.filter { it.amount < 0 }.sumOf { -it.amount }
            val totalExpense = records.filter { it.amount > 0 }.sumOf { it.amount }
            val netProfit = totalIncome - totalExpense

            tvSales.text = "${totalIncome.toInt()} AED"
            tvNetProfit.text = "${netProfit.toInt()} AED"
            tvNetProfit.setTextColor(ContextCompat.getColor(requireContext(), 
                if (netProfit >= 0) R.color.income_green else R.color.expense_red))

            containerTransactions.removeAllViews()
            records.forEach { addTransactionRow(it) }

            loadBalanceTrend(records)
        }
    }

    private fun loadBalanceTrend(records: List<Record>) {
        if (records.isEmpty()) return

        val sortedRecords = records.sortedBy { parseDate(it.createdAt ?: it.date) }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayBalances = mutableMapOf<String, Double>()
        
        var runningBalance = 0.0
        sortedRecords.forEach { record ->
            runningBalance += -record.amount // amount is negative for income
            val dateKey = dateFormat.format(parseDate(record.createdAt ?: record.date))
            dayBalances[dateKey] = runningBalance
        }

        tvCurrentBalance.text = String.format("%,.0f AED", runningBalance)

        val sortedDates = dayBalances.keys.sorted()
        val last30Dates = if (sortedDates.size > 30) sortedDates.takeLast(30) else sortedDates
        
        val entries = last30Dates.mapIndexed { index, date ->
            Entry(index.toFloat(), dayBalances[date]?.toFloat() ?: 0f)
        }

        val accentColor = ContextCompat.getColor(requireContext(), R.color.primary_blue)
        val gradientFill = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                android.graphics.Color.argb(60, 52, 132, 169),
                android.graphics.Color.argb(0, 52, 132, 169)
            )
        )

        val dataSet = LineDataSet(entries, "Balance").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable = gradientFill
            color = accentColor
            lineWidth = 2f
            setDrawValues(false)
            setDrawCircles(false)
        }

        chartBalanceTrend.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setExtraOffsets(0f, 8f, 0f, 0f)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                val displayLabels = last30Dates.map { dateStr ->
                    try {
                        val date = dateFormat.parse(dateStr)
                        SimpleDateFormat("dd-MM", Locale.getDefault()).format(date!!)
                    } catch (e: Exception) { dateStr.substring(5) }
                }
                valueFormatter = IndexAxisValueFormatter(displayLabels)
                setDrawGridLines(false)
                granularity = 1f
                setAvoidFirstLastClipping(true)
                axisMinimum = -0.1f
                axisMaximum = (entries.size - 1).toFloat() + 0.5f
                textColor = ContextCompat.getColor(requireContext(), R.color.text_tertiary)
                textSize = 10f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.divider_gray)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_tertiary)
                textSize = 10f
            }
            axisRight.isEnabled = false

            invalidate()
        }
    }

    private fun parseDate(dateStr: String?): Date {
        if (dateStr == null) return Date()
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()),
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        )
        for (format in formats) {
            try {
                return format.parse(dateStr) ?: continue
            } catch (e: Exception) {}
        }
        return Date()
    }

    private fun addTransactionRow(record: Record) {
        val context = requireContext()
        val density = resources.displayMetrics.density

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(context, R.drawable.bg_ripple_borderless)
            setOnClickListener { showTransactionDetail(record) }
        }

        val indicator = View(context).apply {
            val size = (12 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = (16 * density).toInt()
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
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setTypeface(null, Typeface.NORMAL)
        }

        val dateText = TextView(context).apply {
            text = record.date
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, 2, 0, 0)
        }

        infoLayout.addView(titleText)
        infoLayout.addView(dateText)

        val amountText = TextView(context).apply {
            val displayAmount = if (record.amount < 0) -record.amount else record.amount
            text = "${if (record.amount < 0) "+" else "−"}${formatNumber(displayAmount)} AED"
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, if (record.amount < 0) R.color.income_green else R.color.expense_red))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
        }

        row.addView(indicator)
        row.addView(infoLayout)
        row.addView(amountText)

        containerTransactions.addView(row)

        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply { marginStart = (52 * density).toInt() }
            setBackgroundColor(ContextCompat.getColor(context, R.color.divider_gray))
        }
        containerTransactions.addView(divider)
    }

    private fun showTransactionDetail(record: Record) {
        val ctx = requireContext()
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_transaction_detail, null)

        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerType)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etCurrency = dialogView.findViewById<EditText>(R.id.etCurrency)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val etCounterpartyName = dialogView.findViewById<EditText>(R.id.etCounterpartyName)
        val spinnerCounterpartyType = dialogView.findViewById<Spinner>(R.id.spinnerCounterpartyType)
        val spinnerPaymentMode = dialogView.findViewById<Spinner>(R.id.spinnerPaymentMode)
        val etAccount = dialogView.findViewById<EditText>(R.id.etAccount)
        val switchIsPaid = dialogView.findViewById<Switch>(R.id.switchIsPaid)
        val etReferenceNumber = dialogView.findViewById<EditText>(R.id.etReferenceNumber)
        val switchVatApplicable = dialogView.findViewById<Switch>(R.id.switchVatApplicable)
        val etVatRate = dialogView.findViewById<EditText>(R.id.etVatRate)
        val etVatAmount = dialogView.findViewById<EditText>(R.id.etVatAmount)
        val etTaxCode = dialogView.findViewById<EditText>(R.id.etTaxCode)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etLocation = dialogView.findViewById<EditText>(R.id.etLocation)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)
        val etAttachmentUri = dialogView.findViewById<EditText>(R.id.etAttachmentUri)

        val btnDelete = dialogView.findViewById<Button>(R.id.btnDelete)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // Spinner data
        val typeOptions = listOf("", "income", "expense", "sale", "purchase")
        val counterpartyTypeOptions = listOf("", "Customer", "Supplier", "Employee", "Other")
        val paymentModeOptions = listOf("", "cash", "bank", "credit", "cheque")

        // Create custom spinner adapter that shows a dropdown arrow
        class ArrowSpinnerAdapter(items: List<String>) : ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setPadding(12, 16, 40, 16)  // Extra right padding for arrow
                view.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                view.textSize = 14f
                
                // Add chevron arrow
                val arrow = ContextCompat.getDrawable(ctx, R.drawable.ic_chevron_down)
                arrow?.setBounds(0, 0, arrow.intrinsicWidth, arrow.intrinsicHeight)
                view.setCompoundDrawablesWithIntrinsicBounds(null, null, arrow, null)
                view.compoundDrawablePadding = 8
                
                // Capitalize text
                val originalText = getItem(position) ?: ""
                view.text = originalText.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setPadding(16, 16, 16, 16)
                view.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                view.textSize = 14f
                
                // Capitalize text
                val originalText = getItem(position) ?: ""
                view.text = originalText.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                return view
            }
        }

        // Apply to all three spinners
        spinnerType.adapter = ArrowSpinnerAdapter(typeOptions)
        spinnerType.setPopupBackgroundResource(R.drawable.bg_rounded_dropdown)

        spinnerCounterpartyType.adapter = ArrowSpinnerAdapter(counterpartyTypeOptions)
        spinnerCounterpartyType.setPopupBackgroundResource(R.drawable.bg_rounded_dropdown)

        spinnerPaymentMode.adapter = ArrowSpinnerAdapter(paymentModeOptions)
        spinnerPaymentMode.setPopupBackgroundResource(R.drawable.bg_rounded_dropdown)
        
        // Pre-fill fields
        spinnerType.setSelection(typeOptions.indexOfFirst { it.equals(record.type, ignoreCase = true) }.coerceAtLeast(0))
        val displayAmount = if (record.amount < 0) -record.amount else record.amount
        etAmount.setText(formatNumber(displayAmount))
        etCurrency.setText(record.currency ?: "AED")
        etDescription.setText(record.description)
        etCounterpartyName.setText(record.counterpartyName ?: "")
        spinnerCounterpartyType.setSelection(counterpartyTypeOptions.indexOfFirst { it.equals(record.counterpartyType, ignoreCase = true) }.coerceAtLeast(0))
        spinnerPaymentMode.setSelection(paymentModeOptions.indexOfFirst { it.equals(record.paymentMode, ignoreCase = true) }.coerceAtLeast(0))
        etAccount.setText(record.account)
        switchIsPaid.isChecked = record.isPaid
        etReferenceNumber.setText(record.referenceNumber ?: "")
        switchVatApplicable.isChecked = record.vatApplicable
        etVatRate.setText(if (record.vatRate != null) formatNumber(record.vatRate) else "")
        etVatAmount.setText(if (record.vatAmount != null) formatNumber(record.vatAmount) else "")
        etTaxCode.setText(record.taxCode ?: "")
        etDate.setText(record.date)
        etLocation.setText(record.location ?: "")
        etNotes.setText(record.notes ?: "")
        etAttachmentUri.setText(record.attachmentUri ?: "")

        val dialog = AlertDialog.Builder(ctx)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
        dialog.show()

        btnDelete.setOnClickListener {
            AlertDialog.Builder(ctx)
                .setTitle("Delete transaction?")
                .setMessage("Delete this transaction? Cannot undo.")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        AppDatabase.getDatabase(ctx).recordDao().deleteById(record.id)
                        loadDashboardData()
                        dialog.dismiss()
                        Toast.makeText(ctx, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
                .window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val amountText = etAmount.text.toString().trim()
            val amountValue = amountText.toDoubleOrNull()

            if (amountValue == null || amountValue <= 0.0) {
                Toast.makeText(ctx, "Amount must be a number greater than 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedType = spinnerType.selectedItem?.toString()?.takeIf { it.isNotBlank() }
            val isIncome = selectedType?.lowercase() == "income" || selectedType?.lowercase() == "sale"
            val storedAmount = if (isIncome) -amountValue else amountValue

            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

            val updated = record.copy(
                date = etDate.text.toString().trim().takeIf { it.isNotBlank() } ?: record.date,
                account = etAccount.text.toString().trim().takeIf { it.isNotBlank() } ?: record.account,
                description = etDescription.text.toString().trim().takeIf { it.isNotBlank() } ?: record.description,
                amount = storedAmount,
                type = selectedType,
                currency = etCurrency.text.toString().trim().takeIf { it.isNotBlank() } ?: "AED",
                counterpartyName = etCounterpartyName.text.toString().trim().takeIf { it.isNotBlank() },
                counterpartyType = spinnerCounterpartyType.selectedItem?.toString()?.takeIf { it.isNotBlank() },
                paymentMode = spinnerPaymentMode.selectedItem?.toString()?.takeIf { it.isNotBlank() },
                isPaid = switchIsPaid.isChecked,
                referenceNumber = etReferenceNumber.text.toString().trim().takeIf { it.isNotBlank() },
                vatApplicable = switchVatApplicable.isChecked,
                vatRate = etVatRate.text.toString().trim().toDoubleOrNull(),
                vatAmount = etVatAmount.text.toString().trim().toDoubleOrNull(),
                taxCode = etTaxCode.text.toString().trim().takeIf { it.isNotBlank() },
                location = etLocation.text.toString().trim().takeIf { it.isNotBlank() },
                notes = etNotes.text.toString().trim().takeIf { it.isNotBlank() },
                attachmentUri = etAttachmentUri.text.toString().trim().takeIf { it.isNotBlank() },
                updatedAt = nowIso
            )

            lifecycleScope.launch {
                AppDatabase.getDatabase(ctx).recordDao().updateRecord(updated)
                loadDashboardData()
                Toast.makeText(ctx, "Transaction updated", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}
