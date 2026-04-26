package com.example.promptbooks

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.promptbooks.model.AiTransactionResponse
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatFragment : Fragment() {

    private lateinit var chatInput: EditText
    private lateinit var sendIcon: ImageView
    private lateinit var micIcon: ImageView
    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScroll: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        chatInput = view.findViewById(R.id.chatInput)
        sendIcon = view.findViewById(R.id.sendIcon)
        micIcon = view.findViewById(R.id.micIcon)
        chatContainer = view.findViewById(R.id.chatContainer)
        chatScroll = view.findViewById(R.id.chatScroll)

        chatInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString().trim()
                sendIcon.visibility = if (inputText.isEmpty()) View.GONE else View.VISIBLE
                micIcon.visibility = if (inputText.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        sendIcon.setOnClickListener {
            val userMessage = chatInput.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                addMessage("You: $userMessage")
                chatInput.setText("")
                callGPT(userMessage)
            }
        }

        return view
    }

    suspend fun buildExpenseSummary(): String {
        val records = AppDatabase.getDatabase(requireContext()).recordDao().getAllRecords()
        if (records.isEmpty()) {
            return "No previous expenses recorded. Total so far: 0 DHS"
        }

        val summary = StringBuilder("Previous expenses:\n")
        var total = 0.0
        records.forEach {
            summary.append("- ${it.date}: ${it.description} - ${it.amount} DHS\n")
            total += it.amount
        }
        summary.append("Total so far: $total DHS")
        return summary.toString()
    }

    private fun addMessage(message: String) {
        val introLayout = view?.findViewById<LinearLayout>(R.id.chatIntro)
        introLayout?.visibility = View.GONE

        val isUser = message.startsWith("You:")
        val cleanMessage = when {
            isUser -> message.substring(4).trim()
            message.startsWith("PromptBooks:") -> message.substring(12).trim()
            message.startsWith("PB:") -> message.substring(3).trim()
            else -> message
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 12
            bottomMargin = 4
            gravity = if (isUser) Gravity.END else Gravity.START
            if (isUser) marginStart = 80 else marginEnd = 80
        }

        val messageContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = params
        }

        val textView = TextView(requireContext()).apply {
            text = cleanMessage
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, if (isUser) R.color.text_on_user else R.color.text_on_bot))
            background = ContextCompat.getDrawable(context, if (isUser) R.drawable.bg_message_user else R.drawable.bg_message_bot)
            setPadding(32, 20, 32, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = if (isUser) Gravity.END else Gravity.START
            }
        }

        val timeFormat = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
        val timestamp = timeFormat.format(Date())

        val timeView = TextView(requireContext()).apply {
            text = timestamp
            textSize = 10f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(8, 4, 8, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = if (isUser) Gravity.END else Gravity.START
            }
        }

        messageContainer.addView(textView)
        messageContainer.addView(timeView)
        chatContainer.addView(messageContainer)

        chatScroll.post {
            chatScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun formatNumber(value: Double?): String {
        if (value == null) return ""
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    private fun normalizeTransactionResponse(
        raw: AiTransactionResponse,
        userPrompt: String
    ): AiTransactionResponse {
        fun cleanStr(s: String?): String? {
            val trimmed = s?.trim() ?: return null
            if (trimmed.isEmpty()) return null
            val lowers = trimmed.lowercase()
            if (lowers == "unknown" || lowers == "null" || lowers == "n/a" || lowers == "-") return null
            return trimmed
        }

        val intent = cleanStr(raw.intent)?.lowercase() ?: "unknown"
        val item = cleanStr(raw.item)
        val quantity = raw.quantity
        val amount = raw.amount

        // Payment mode rule: only if in original user message
        val promptLowers = userPrompt.lowercase()
        val paymentMode = when {
            promptLowers.contains("cash") -> "cash"
            promptLowers.contains("bank") -> "bank"
            promptLowers.contains("credit") -> "credit"
            else -> null
        }

        val counterparty = cleanStr(raw.counterparty)
        val date = cleanStr(raw.date) ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return AiTransactionResponse(
            intent = intent,
            item = item,
            quantity = quantity,
            amount = amount,
            payment_mode = paymentMode,
            counterparty = counterparty,
            date = date
        )
    }

    private fun validateTransaction(res: AiTransactionResponse): Boolean {
        if (res.intent == "unknown") return false
        if (res.amount == null || res.amount <= 0.0) return false
        return true
    }

    private fun buildConfirmationMessage(res: AiTransactionResponse): String {
        val amountStr = formatNumber(res.amount)
        val qtyStr = formatNumber(res.quantity)
        val item = res.item?.trim()
        val counterparty = res.counterparty?.trim()
        val mode = if (!res.payment_mode.isNullOrBlank()) " (${res.payment_mode.trim()})" else ""

        return when (res.intent) {
            "purchase" -> {
                val sb = StringBuilder("Saved purchase: ")
                if (qtyStr.isNotEmpty()) sb.append("$qtyStr ")
                sb.append("${item ?: "items"} for $amountStr DHS")
                if (!counterparty.isNullOrBlank()) sb.append(" from $counterparty")
                sb.append(".")
                sb.toString()
            }
            "sale" -> {
                val sb = StringBuilder("Saved sale: ")
                if (qtyStr.isNotEmpty()) sb.append("$qtyStr ")
                sb.append("${item ?: "items"} for $amountStr DHS")
                if (!counterparty.isNullOrBlank()) sb.append(" to $counterparty")
                sb.append(mode).append(".")
                sb.toString()
            }
            "expense" -> {
                val sb = StringBuilder("Saved expense: $amountStr DHS")
                if (!item.isNullOrBlank()) sb.append(" for $item")
                sb.append(".")
                sb.toString()
            }
            "income" -> {
                val sb = StringBuilder("Saved income: $amountStr DHS")
                if (!counterparty.isNullOrBlank()) sb.append(" from $counterparty")
                sb.append(mode).append(".")
                sb.toString()
            }
            else -> "Saved transaction: $amountStr DHS."
        }
    }

    private fun isInputReadyForAi(input: String): Boolean {
        val lowers = input.lowercase()
        val keywords = listOf(
            "buy", "bought", "purchase", "purchased",
            "sell", "sold", "sale",
            "pay", "paid", "expense", "spend", "spent",
            "receive", "received", "income"
        )
        val hasKeyword = keywords.any { lowers.contains(it) }
        val hasNumber = input.any { it.isDigit() }

        return if (hasKeyword && hasNumber) {
            android.util.Log.d("ChatFragment", "AI guardrail passed")
            true
        } else {
            android.util.Log.d("ChatFragment", "AI guardrail blocked vague input")
            false
        }
    }

    private fun callGPT(userPrompt: String) {
        if (userPrompt.contains("remove the first", ignoreCase = true)) {
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(requireContext())
                db.recordDao().deleteFirst()
                addMessage("PromptBooks: The first record has been removed.")
            }
            return
        }

        if (userPrompt.contains("remove the entry for", ignoreCase = true)) {
            val keyword = userPrompt.substringAfter("remove the entry for").trim()
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(requireContext())
                db.recordDao().deleteByDescription(keyword)
                addMessage("PromptBooks: Records matching \"$keyword\" have been removed.")
            }
            return
        }

        // AI Guardrail: stop vague inputs early
        if (!isInputReadyForAi(userPrompt)) {
            addMessage("PromptBooks: I need a bit more detail. Please include the amount and whether it was a purchase, sale, expense, or income.")
            return
        }

        lifecycleScope.launch {
            val summaryMessage = buildExpenseSummary()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://openrouter.ai/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(GPTService::class.java)

            if (BuildConfig.OPENROUTER_API_KEY.isBlank()) {
                addMessage("PromptBooks: Missing API key configuration.")
                return@launch
            }

            val request = GPTRequest(
                model = "meta-llama/llama-3-8b-instruct",
                messages = listOf(
                    Message("system", "You are a data extraction bot. Return ONLY valid JSON. No sentences, no explanation. Use keys: intent, item, quantity, amount, payment_mode, counterparty. If a value is unknown, use null. Possible intents: 'purchase', 'sale', 'expense', 'income', 'unknown'."),
                    Message("user", userPrompt)
                ),
                stream = false
            )

            val authHeader = "Bearer ${BuildConfig.OPENROUTER_API_KEY}"
            android.util.Log.d("ChatFragment", "Sending OpenRouter request. Key present: ${BuildConfig.OPENROUTER_API_KEY.isNotBlank()}")
            val call = service.sendPrompt(authHeader, request)

            call.enqueue(object : Callback<GPTResponse> {
                override fun onResponse(call: Call<GPTResponse>, response: Response<GPTResponse>) {
                    if (response.isSuccessful) {
                        val botReply = response.body()?.choices?.firstOrNull()?.message?.content
                        if (botReply == null) {
                            addMessage("PromptBooks: I'm sorry, I couldn't get a response.")
                            return
                        }

                        try {
                            android.util.Log.d("ChatFragment", "AI JSON response: $botReply")
                            val gson = Gson()
                            val aiResponseRaw = gson.fromJson(botReply, AiTransactionResponse::class.java)

                            val normalized = normalizeTransactionResponse(aiResponseRaw, userPrompt)

                            if (validateTransaction(normalized)) {
                                // Save to DB
                                val amount = normalized.amount!!
                                val record = Record(
                                    id = 0,
                                    date = normalized.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    account = if (normalized.intent == "income" || normalized.intent == "sale") "Income" else "Expenses",
                                    description = "${normalized.intent.replaceFirstChar { it.uppercase() }}: ${normalized.item ?: "General"}",
                                    amount = if (normalized.intent == "income" || normalized.intent == "sale") -amount else amount
                                )

                                lifecycleScope.launch {
                                    val db = AppDatabase.getDatabase(requireContext())
                                    db.recordDao().insertRecord(record)
                                }

                                // Show App-Generated Message
                                val displayMsg = buildConfirmationMessage(normalized)
                                addMessage("PromptBooks: $displayMsg")
                            } else {
                                addMessage("PromptBooks: I need a bit more detail. Please include the amount and whether it was a purchase, sale, expense, or income.")
                            }

                        } catch (e: Exception) {
                            android.util.Log.e("ChatFragment", "Error parsing AI JSON", e)
                            addMessage("PromptBooks: I need a bit more detail. Please include the amount and whether it was a purchase, sale, expense, or income.")
                        }
                    } else {
                        android.util.Log.e("ChatFragment", "OpenRouter failed: ${response.code()}")
                        addMessage("PromptBooks: Failed to get response. (${response.code()})")
                    }
                }

                override fun onFailure(call: Call<GPTResponse>, t: Throwable) {
                    addMessage("PromptBooks: Error - ${t.localizedMessage}")
                }
            })
        }
    }
}
