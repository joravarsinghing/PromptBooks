# Milestone 2 — AI‑Assisted Bookkeeping‑Grade Transaction Records

Patch **transaction model**, **DAO/update functions**, **AI request/response models**, **Dashboard list**, **detail popup**, and **sample data generator** only.  
Do **not** modify chat UI, guardrails, networking structure, status bar, splash/launcher, support popup, or dashboard formulas except refreshing after edits.  
Delete behavior must remain unchanged.

---

## Goal
Upgrade PromptBooks from basic AI extraction to **rich, AI‑assisted bookkeeping records**.  
AI should infer *all possible fields* from natural language and return a structured JSON object.  
The app stores this JSON as a transaction, and the user can later open the record to review and complete missing fields.

---

## 1) AI Inference Schema (JSON Contract)
Define a **strict JSON schema** that AI must fill.  
All fields are **nullable**.  
AI must **not guess**; it fills only what is explicitly present or strongly implied.

### Core
- `type`
- `amount`
- `currency` (default "AED")
- `description`

### Counterparty
- `counterpartyName`
- `counterpartyType` (Customer / Supplier / Employee / Other)

### Payment & Reconciliation
- `paymentMode`
- `account`
- `isPaid`
- `referenceNumber`

### VAT / Tax
- `vatApplicable`
- `vatRate`
- `vatAmount`
- `taxCode`

### Metadata
- `date`
- `createdAt`
- `updatedAt`
- `source` (AI / Manual / Attachment OCR / Imported)

### Context
- `location`
- `notes`
- `attachmentUri`
- `attachmentType`

Blank fields must remain blank.  
Never display `"null"`, `"unknown"`, `"n/a"`, or `"-"`.

---

## 2) App → AI Request Update
Modify the AI request to include:

- The **full inference schema**
- Clear instructions:
  - extract only what is present  
  - do not invent data  
  - return JSON only  
  - leave missing fields as null  

Guardrail stays the same:  
Only send to AI if input is transaction‑like.

---

## 3) App Saves AI JSON Directly
The app:

- Parses the JSON  
- Saves it into the Room entity  
- Does not modify or “fix” fields  
- Sets `source = "AI"`  

This ensures clean auditability.

---

## 4) Dashboard Row Tap → Detail Popup
Tapping a transaction row opens a **rounded modal popup** titled: `Transaction Details`


The existing 3‑dot delete menu must continue working.

---

## 5) Editable Popup Fields
The popup must allow editing of **all fields** in the schema:

- type  
- amount  
- currency  
- description  
- counterpartyName  
- counterpartyType  
- paymentMode  
- account  
- isPaid  
- referenceNumber  
- vatApplicable  
- vatRate  
- vatAmount  
- taxCode  
- location  
- notes  
- date/time  
- attachment placeholder (URI only)

Use simple EditTexts, Spinners, Switches.

---

## 6) Save / Cancel Behavior

### Save
- Validate amount is numeric and > 0  
- Update the Room transaction  
- Update `updatedAt`  
- Close popup  
- Refresh dashboard list and totals  

### Cancel
- Close popup without saving

---

## 7) Dashboard Refresh Rules
After saving edits:

- Transaction row updates  
- Bank Balance recalculates  
- Sales recalculates  
- No changes to formulas except reading updated fields  

---

## 8) Sample Data Update
Sample data must include:

- varied transaction types  
- counterpartyName + counterpartyType  
- paymentMode + account  
- reference numbers  
- VAT fields (0%, 5%, exempt)  
- realistic dates/times  
- notes + locations  
- attachmentUri placeholders  

---

## 9) Verification Checklist
- AI returns structured JSON  
- App saves JSON correctly  
- Row tap opens detail popup  
- Editing amount updates totals  
- Editing counterparty/location/notes persists  
- VAT fields save correctly  
- Blank fields remain blank  
- Delete still works  
- Sample data shows richer bookkeeping records  
- No regressions in chat flow or dashboard behavior