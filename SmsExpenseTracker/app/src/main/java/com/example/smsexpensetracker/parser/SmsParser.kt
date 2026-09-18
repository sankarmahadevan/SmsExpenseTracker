package com.example.smsexpensetracker.parser

import com.example.smsexpensetracker.data.model.*

/**
 * Parses Indian bank SMS messages into Transaction objects.
 * Supports: HDFC, SBI, ICICI, Axis, Kotak, BOI, Canara, PNB, IndusInd, Yes Bank, BOB
 * Channels: UPI (GPay/PhonePe/Paytm/BHIM), ATM, Credit Card, NEFT/IMPS
 */
object SmsParser {

    // Amount: Rs.1,234.56 | Rs 1234 | INR 1,234.56 | ₹1234
    private val AMOUNT_REGEX = Regex(
        """(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val BANK_PATTERNS = mapOf(
        "HDFC"     to Regex("""HDFC""", RegexOption.IGNORE_CASE),
        "SBI"      to Regex("""SBI|State Bank""", RegexOption.IGNORE_CASE),
        "ICICI"    to Regex("""ICICI""", RegexOption.IGNORE_CASE),
        "Axis"     to Regex("""Axis\s*Bank|AXISBANK""", RegexOption.IGNORE_CASE),
        "Kotak"    to Regex("""Kotak""", RegexOption.IGNORE_CASE),
        "BOI"      to Regex("""Bank of India|BOI""", RegexOption.IGNORE_CASE),
        "Canara"   to Regex("""Canara""", RegexOption.IGNORE_CASE),
        "PNB"      to Regex("""PNB|Punjab National""", RegexOption.IGNORE_CASE),
        "IndusInd" to Regex("""IndusInd""", RegexOption.IGNORE_CASE),
        "Yes Bank" to Regex("""Yes\s*Bank|YESBANK""", RegexOption.IGNORE_CASE),
        "IDBI"     to Regex("""IDBI""", RegexOption.IGNORE_CASE),
        "Federal"  to Regex("""Federal\s*Bank""", RegexOption.IGNORE_CASE),
        "BOB"      to Regex("""Bank of Baroda|BOB""", RegexOption.IGNORE_CASE),
        "UCO"      to Regex("""UCO""", RegexOption.IGNORE_CASE),
    )

    // ── Category Patterns ──────────────────────────────────────────────────

    private val CC_PAYMENT_PATTERNS = listOf(
        Regex("""(?:payment|paid).*?credit\s*card""", RegexOption.IGNORE_CASE),
        Regex("""credit\s*card.*?(?:payment|paid|received)""", RegexOption.IGNORE_CASE),
        Regex("""thank\s*you\s*for\s*pay.*?(?:card|credit)""", RegexOption.IGNORE_CASE),
        Regex("""card\s*bill.*?(?:paid|payment|received)""", RegexOption.IGNORE_CASE),
    )

    private val CC_DEBIT_PATTERNS = listOf(
        Regex("""credit\s*card.*?(?:spent|used|transaction|debited)""", RegexOption.IGNORE_CASE),
        Regex("""(?:spent|used|transaction).*?credit\s*card""", RegexOption.IGNORE_CASE),
        Regex("""card\s+(?:no\.?\s*)?[xX*]{2,}\d{4}.*?(?:spent|used|transaction)""", RegexOption.IGNORE_CASE),
        Regex("""(?:spent|used|transaction).*?card\s+(?:no\.?\s*)?[xX*]{2,}\d{4}""", RegexOption.IGNORE_CASE),
        Regex("""done\s+on\s+(?:your\s+)?(?:credit\s*)?card""", RegexOption.IGNORE_CASE),
        Regex("""(?:spent|purchase).*?(?:card|cc)""", RegexOption.IGNORE_CASE),
    )

    private val ATM_PATTERNS = listOf(
        Regex("""ATM\s*(?:withdrawal|withdraw|cash|txn|transaction)""", RegexOption.IGNORE_CASE),
        Regex("""(?:cash|withdrawn).*?ATM""", RegexOption.IGNORE_CASE),
        Regex("""ATM.*?(?:debited|withdrawn|cash)""", RegexOption.IGNORE_CASE),
        Regex("""ATM[/ ]DEBIT\s*CARD""", RegexOption.IGNORE_CASE),
        Regex("""cash\s+withdrawal""", RegexOption.IGNORE_CASE),
    )

    private val UPI_CREDIT_PATTERNS = listOf(
        Regex("""(?:credited|received).*?(?:UPI|BHIM|GPay|PhonePe|Paytm)""", RegexOption.IGNORE_CASE),
        Regex("""(?:UPI|BHIM|GPay|PhonePe|Paytm).*?(?:credited|received)""", RegexOption.IGNORE_CASE),
        Regex("""money\s+received.*?UPI""", RegexOption.IGNORE_CASE),
        Regex("""received.*?via\s+UPI""", RegexOption.IGNORE_CASE),
    )

    private val UPI_DEBIT_PATTERNS = listOf(
        Regex("""(?:debited|deducted|sent|paid).*?(?:UPI|BHIM|GPay|PhonePe|Paytm)""", RegexOption.IGNORE_CASE),
        Regex("""(?:UPI|BHIM|GPay|PhonePe|Paytm).*?(?:debited|deducted|sent|paid)""", RegexOption.IGNORE_CASE),
        Regex("""a[/\\]c.*?debited(?!.*ATM)""", RegexOption.IGNORE_CASE),
        Regex("""[a-z0-9.\-_]+@(?:okhdfc|oksbi|okhdfcbank|okaxis|ybl|upi|paytm|waicici|ibl|apl)""", RegexOption.IGNORE_CASE),
        Regex("""sent.*?via\s+UPI""", RegexOption.IGNORE_CASE),
        Regex("""paid.*?via\s+(?:UPI|PhonePe|GPay|Paytm)""", RegexOption.IGNORE_CASE),
    )

    private val SAVINGS_CREDIT_PATTERNS = listOf(
        Regex("""credited.*?(?:a[/\\]c|account|acct)""", RegexOption.IGNORE_CASE),
        Regex("""(?:a[/\\]c|account|acct).*?credited""", RegexOption.IGNORE_CASE),
        Regex("""received\s+in.*?(?:a[/\\]c|account)""", RegexOption.IGNORE_CASE),
        Regex("""(?:NEFT|IMPS|RTGS).*?credited""", RegexOption.IGNORE_CASE),
        Regex("""salary.*?credited""", RegexOption.IGNORE_CASE),
    )

    private val IGNORE_PATTERNS = listOf(
        Regex("""OTP|one.time.password""", RegexOption.IGNORE_CASE),
        Regex("""available\s+(?:balance|limit)\s+is""", RegexOption.IGNORE_CASE),
        Regex("""minimum\s+(?:due|payment)""", RegexOption.IGNORE_CASE),
        Regex("""statement\s+(?:is\s+)?(?:ready|generated|available)""", RegexOption.IGNORE_CASE),
        Regex("""pre.approved|loan\s+offer|apply\s+now""", RegexOption.IGNORE_CASE),
        Regex("""reward\s+point|cashback""", RegexOption.IGNORE_CASE),                  // widened: was "cashback (of|earned)"
        Regex("""your\s+(?:account|card)\s+(?:is\s+)?(?:activated|linked|registered)""", RegexOption.IGNORE_CASE),
        // ── new: telecom / utility / DTH bill-generation notices ──
        Regex("""bill\s+(?:for\s+your.*?)?(?:has\s+been\s+)?generated""", RegexOption.IGNORE_CASE),
        Regex("""current\s+month\s+payable\s+amount""", RegexOption.IGNORE_CASE),
        Regex("""plan\s+rental""", RegexOption.IGNORE_CASE),
        Regex("""bill\s+summary""", RegexOption.IGNORE_CASE),
    )

    // ── Public API ─────────────────────────────────────────────────────────

    fun parse(sender: String, body: String): Transaction? {
        if (!containsCurrencyMarker(body)) return null
        if (IGNORE_PATTERNS.any { it.containsMatchIn(body) }) return null
        val amountPaise = extractAmountPaise(body) ?: return null
        val bankName    = detectBank(sender, body)
        return classify(body, amountPaise, bankName)
    }

    fun extractAmountPaise(body: String): Long? {
        val match = AMOUNT_REGEX.find(body) ?: return null
        val raw   = match.groupValues[1].replace(",", "")
        val rupees = raw.toDoubleOrNull() ?: return null
        return (rupees * 100).toLong()
    }

    // ── Private Helpers ────────────────────────────────────────────────────

    private fun containsCurrencyMarker(body: String) =
        body.contains("Rs", ignoreCase = true) ||
        body.contains("INR", ignoreCase = true) ||
        body.contains("₹")

    private fun detectBank(sender: String, body: String): String {
        val text = "$sender $body"
        return BANK_PATTERNS.entries.firstOrNull { (_, rx) -> rx.containsMatchIn(text) }?.key ?: "Unknown"
    }

    private fun classify(body: String, amountPaise: Long, bankName: String): Transaction? {
        // Priority: CC payment > CC spend > ATM > UPI debit > UPI credit > savings credit
        return when {
            CC_PAYMENT_PATTERNS.any  { it.containsMatchIn(body) } -> Transaction(
                amountPaise = amountPaise, type = TransactionType.DEBIT,
                category = TransactionCategory.CREDIT_CARD_PAYMENT,
                accountType = AccountType.SAVINGS, bankName = bankName,
                description = extractDescription(body, "CC Bill Payment"), rawSms = body
            )
            CC_DEBIT_PATTERNS.any    { it.containsMatchIn(body) } -> Transaction(
                amountPaise = amountPaise, type = TransactionType.DEBIT,
                category = TransactionCategory.CREDIT_CARD,
                accountType = AccountType.CREDIT_CARD, bankName = bankName,
                description = extractDescription(body, "CC Spend"), rawSms = body
            )
            ATM_PATTERNS.any         { it.containsMatchIn(body) } -> Transaction(
                amountPaise = amountPaise, type = TransactionType.DEBIT,
                category = TransactionCategory.ATM,
                accountType = AccountType.SAVINGS, bankName = bankName,
                description = extractDescription(body, "ATM Withdrawal"), rawSms = body
            )
            // ── moved above UPI_CREDIT_PATTERNS ──
            UPI_DEBIT_PATTERNS.any   { it.containsMatchIn(body) } -> Transaction(
                amountPaise = amountPaise, type = TransactionType.DEBIT,
                category = TransactionCategory.UPI,
                accountType = AccountType.SAVINGS, bankName = bankName,
                description = extractDescription(body, "UPI Sent"), rawSms = body
            )
            UPI_CREDIT_PATTERNS.any  { it.containsMatchIn(body) } -> Transaction(
                amountPaise = amountPaise, type = TransactionType.CREDIT,
                category = TransactionCategory.UPI,
                accountType = AccountType.SAVINGS, bankName = bankName,
                description = extractDescription(body, "UPI Received"), rawSms = body
            )
            SAVINGS_CREDIT_PATTERNS.any { it.containsMatchIn(body) } -> Transaction(
                amountPaise = amountPaise, type = TransactionType.CREDIT,
                category = TransactionCategory.OTHER,
                accountType = AccountType.SAVINGS, bankName = bankName,
                description = extractDescription(body, "Credit Received"), rawSms = body
            )
            else -> null
        }
    }

    private fun extractDescription(body: String, fallback: String): String {
        Regex("""(?:at|@)\s+([A-Z0-9 &.'\-]{3,30})""")
            .find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        Regex("""to\s+([a-z0-9.\-_]+@[a-z]+)""", RegexOption.IGNORE_CASE)
            .find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        Regex("""(?:from|by)\s+([A-Z][A-Za-z\s]{2,20})""")
            .find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        return fallback
    }
}
