package com.example.promptbooks

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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

        btnNewChat.setOnClickListener { showNewChatConfirmation() }
        btnClearData.setOnClickListener { showClearDataConfirmation() }

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
            val size = (32 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            setPadding(4, 4, 4, 4)
            background = ContextCompat.getDrawable(context, R.drawable.bg_ripple_borderless)
            isClickable = true
            isFocusable = true
            contentDescription = "Options"
            setOnClickListener { view -> showRowMenu(view, record) }
        }

        row.addView(indicator)
        row.addView(infoLayout)
        row.addView(amountText)
        row.addView(moreBtn)

        containerTransactions.addView(row)

        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * density).toInt()
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
        .setTitle("Transaction Details")
        .setView(dialogView)
        .setPositiveButton("Save", null)
        .setNegativeButton("Cancel", null)
        .create()

    dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
    dialog.show()

    // Style the buttons
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(ctx, R.color.primary_blue))
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))

    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
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
