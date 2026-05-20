# Roadmap Progress — Milestone 2

**Last updated:** 2026-05-20  
**Status:** ✅ All 9 sections implemented + UI Enhancements Complete

---

## Completed Steps

### 1. AI Inference Schema ✅
- Defined full JSON schema in `AiTransactionResponse.kt`
- All fields: type, amount, currency, description, counterpartyName, counterpartyType, paymentMode, account, isPaid, referenceNumber, vatApplicable, vatRate, vatAmount, taxCode, date, location, notes, attachmentUri, attachmentType
- All fields nullable

### 2. App → AI Request Update ✅
- `ChatFragment.kt` updated with new structured system prompt
- Prompt instructs AI to extract only what's present, return JSON only, leave missing fields as null
- Uses same model: `meta-llama/llama-3-8b-instruct`
- Guardrail unchanged

### 3. App Saves AI JSON Directly ✅
- `ChatFragment.kt` parses full `AiTransactionResponse`
- Maps all fields to `Record` entity
- Sets `source = "AI"`, `createdAt`, `updatedAt` as ISO timestamp
- No field modification beyond null-cleaning

### 4–5. Dashboard Row Tap → Detail Popup ✅
- `DashboardFragment.kt`: each transaction row is now clickable (ripple feedback)
- Row tap calls `showTransactionDetail(record)`
- Popup inflates `dialog_transaction_detail.xml`
- All schema fields shown: Spinners for type/counterpartyType/paymentMode, Switches for isPaid/vatApplicable, EditTexts for all other fields
- 3-dot delete menu unchanged

### 6. Save / Cancel Behavior ✅
- Save button validates amount > 0 and numeric
- Updates `Record` via `RecordDao.updateRecord()`
- Sets `updatedAt` timestamp
- Dismisses popup
- Calls `loadDashboardData()` to refresh list and totals

### 7. Dashboard Refresh ✅
- `loadDashboardData()` called after save
- Bank Balance and Sales recalculate from updated records
- No formula logic changed

### 8. Sample Data Update ✅
- `MainActivity.kt` `generateSampleData()` now creates 10 rich records with:
  - Varied types (sale, purchase, expense, income)
  - counterpartyName + counterpartyType
  - paymentMode (cash, bank, credit, cheque)
  - referenceNumber on all invoices
  - VAT fields at 0%, 5%, and exempt (ZR)
  - Realistic dates and ISO createdAt/updatedAt
  - notes and locations
  - isPaid true/false mix
  - source = "AI"

### 9. Database Migration ✅
- `AppDatabase.kt` bumped to version 2
- `MIGRATION_1_2` adds all 18 new columns via `ALTER TABLE`
- Existing records preserved, new columns default to null/0

### 10. UI Enhancement – Transaction Detail Popup ✅
- **Segmented layout** with emoji headers (💰 Basic Info, 👤 Counterparty, 📊 Financial, 🧾 VAT, 📍 Additional)
- **Rounded corners** on all input fields and dropdowns using `bg_rounded_spinner.xml`
- **Custom dropdown adapter** (`ArrowSpinnerAdapter`) with:
  - Visible chevron arrow on all spinners
  - Capitalized text (Income, Expense, Sale, Purchase, etc.)
  - Rounded dropdown popup background (`bg_rounded_dropdown.xml`)
- **Unified input styling** – EditTexts now use same white rounded box as spinners
- **Softer placeholder text** color (`#BDBDBD`) for better visibility
- **Fixed ripple effect** – changed from oval to rectangle with rounded corners
- **Removed double arrow** bug in spinners

---

## Files Modified

| File | Change |
|------|--------|
| `Record.kt` | Added 18 new nullable fields |
| `AppDatabase.kt` | Version 2 + MIGRATION_1_2 |
| `RecordDao.kt` | Added `@Update updateRecord()` |
| `model/AiTransactionResponse.kt` | Full schema (19 fields) |
| `ChatFragment.kt` | New AI prompt, full field mapping, source="AI" |
| `DashboardFragment.kt` | Row tap → detail popup with all editable fields, custom ArrowSpinnerAdapter |
| `MainActivity.kt` | Rich 10-record sample data |

## Files Created

| File | Purpose |
|------|---------|
| `res/layout/dialog_transaction_detail.xml` | Scrollable popup with all editable form fields |
| `res/drawable/bg_rounded_grey.xml` | Rounded grey background for form sections |
| `res/drawable/bg_rounded_spinner.xml` | Rounded white background for inputs |
| `res/drawable/bg_rounded_dropdown.xml` | Rounded background for dropdown popups |
| `res/drawable/ic_chevron_down.xml` | Chevron arrow icon for spinners |
| `res/drawable/bg_ripple_borderless.xml` | Fixed ripple effect (rectangle with rounded corners) |

---

## Verification Checklist

- [x] AI returns structured JSON (new prompt with full schema)
- [x] App saves JSON correctly (all fields mapped in ChatFragment)
- [x] Row tap opens detail popup (DashboardFragment)
- [x] Editing amount updates totals (loadDashboardData after save)
- [x] Editing counterparty/location/notes persists (updateRecord)
- [x] VAT fields save correctly (vatRate, vatAmount, vatApplicable)
- [x] Blank fields remain blank (null safety throughout)
- [x] Delete still works (showRowMenu unchanged)
- [x] Sample data shows richer bookkeeping records
- [x] No regressions in chat flow or dashboard behavior
- [x] Spinners show visible dropdown arrows
- [x] Spinner dropdowns have rounded corners
- [x] Text fields match spinner styling (white rounded boxes)
- [x] Placeholder text is visible (`#BDBDBD`)
- [x] Ripple effect no longer stretched (rectangle mask)

---

## UI Enhancement Details

### Spinner Dropdown Arrow Fix
Custom `ArrowSpinnerAdapter` class adds chevron drawable to the right side of all spinners with proper padding.

### Rounded Corners Everywhere
- Input fields: `bg_rounded_spinner.xml` (8dp radius, white bg, light grey border)
- Dropdown popup: `bg_rounded_dropdown.xml` (12dp radius, white bg)
- Form sections: `bg_rounded_grey.xml` (12dp radius, light grey bg)

### Text Capitalization
Dropdown options automatically capitalized:
- `income` → `Income`
- `expense` → `Expense`
- `sale` → `Sale`
- `purchase` → `Purchase`
- `cash` → `Cash`
- `bank` → `Bank`
- `credit` → `Credit`
- `cheque` → `Cheque`

---

## If Resuming This Work

Nothing is incomplete. If extending further:
- Consider adding DatePickerDialog for the date field in the popup
- Add attachment support (camera/file picker) when voice/upload is enabled
- `TransactionResponse.kt` in `model/` is an unused legacy file — can be deleted
- Add dark mode support for the transaction popup
- Implement form field validation with visual feedback

---

# 10 Quality Testing Prompts for PromptBooks

| # | Prompt | What It Tests |
|---|--------|----------------|
| 1 | `I sold 3 chairs to ABC Corp for $450 cash. Invoice INV-001.` | Basic sale extraction: type, amount, counterparty, paymentMode, referenceNumber, and null handling for missing fields |
| 2 | `Bought office supplies from Staples for $120 + 5% VAT. Paid by card. Downtown branch.` | VAT fields (rate, applicability), location extraction, and paymentMode mapping to schema enums |
| 3 | `Paid electricity bill $85. Date next Monday.` | Expense type, future date extraction, and null defaults for missing counterparty/paymentMode |
| 4 | `Refund from Amazon $29.99 for returned headphones.` | Schema enum constraint – "refund" not allowed; AI must map to closest valid type (`income`) |
| 5 | `I bought something yesterday.` | App-side guardrail: blocks vague transactions with no amount or counterparty before API call |
| 6 | `Paid €500 to Freelancer John via PayPal for website design.` | Non-USD currency handling and unsupported paymentMode (`PayPal`) → graceful fallback to `null` |
| 7 | `Sold 2 laptops to Dell for $2000, and bought a printer from HP for $350 cash.` | **Limitation test:** Single-transaction schema cannot handle bulk entry; expects graceful failure or first transaction only |
| 8 | `Paid rent $1500 to Landlord Ltd. Receipt attached.` | Attachment fields remain `null` (placeholder UX), and `isPaid` is not auto-inferred by AI |
| 9 | `Expense: taxi to airport, $45. Note: 'Client meeting – invoice #T123'.` | Special characters in notes, description extraction, and counterpartyType null handling |
| 10 | `I bought 10 units at $25 each, plus 10% VAT. What's my total?` | **Critical boundary test:** AI must not calculate total; only extract `amount: 250`, no arithmetic hallucination |