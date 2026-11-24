package com.example.scanner

import com.example.model.ParsedSmsCandidate
import com.example.model.PaymentInstrument
import com.example.model.TransactionType
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.roundToLong

object SmsParserEngine {

    private val otpOrSpamSignals = listOf(
        "otp", "one time password", "one-time password", "verification code",
        "security code", "auth code", "do not share", "valid for", "login code",
        "pin is", "cvv", "pre-approved", "apply now", "loan offer", "credit limit increase"
    )

    private val debitVerbs = listOf(
        "debited", "debit", "spent", "paid", "charged", "withdrawn", "withdrawal",
        "purchase", "purchased", "sent", "deducted", "used at", "txn of",
        "transaction of", "pos ", "emi ", "auto-debit", "autodebit", "transferred to",
        "payment of", "bill payment"
    )

    private val creditVerbs = listOf(
        "credited", "credit", "received", "deposited", "deposit", "salary",
        "refund", "refunded", "cashback", "reversed", "reversal", "added to",
        "interest credited", "dividend", "payout", "transferred from", "direct deposit"
    )

    private val bankContextSignals = listOf(
        "a/c", "acct", "account", "bank", "card", "debit", "credit", "upi",
        "vpa", "atm", "pos", "neft", "imps", "rtgs", "ach", "wire", "zelle",
        "venmo", "avl bal", "available balance", "bal:", "balance", "txn", "ending"
    )

    // Matches currency symbol/code before or after number: $42.50, Rs. 1,299.00, INR 500, USD 120.45, €19.99, £10.00
    private val currencyAmountRegex = Regex(
        """(?i)(?:\b(USD|INR|EUR|GBP|AED|CAD|AUD|SGD|RS\.?)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)|([\$₹€£])\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)|([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(USD|INR|EUR|GBP|AED|CAD|AUD|SGD)\b)"""
    )

    private val balancePhraseRegex = Regex(
        """(?i)(?:avl\.?\s*bal(?:ance)?|available\s+bal(?:ance)?|curr(?:ent)?\s*bal(?:ance)?|bal(?:ance)?)\s*(?:is|:)?\s*(?:(?:USD|INR|EUR|GBP|AED|CAD|AUD|RS\.?|[\$₹€£])\s*)?[0-9][0-9,]*(?:\.[0-9]{1,2})?"""
    )

    private val accountLast4Regex = Regex(
        """(?i)(?:a/c|acct|account|card|ending(?:\s+in)?|no\.?)\s*(?:is|no\.?|ending|with)?\s*[:#*\-xX]*([0-9]{4})\b|[xX*]{2,}(\d{4})\b"""
    )

    private val merchantAtToRegex = Regex(
        """(?i)\b(?:at|to|from|via|towards|info[:\s]+|merchant[:\s]+)\s+([A-Za-z0-9][A-Za-z0-9 &*._/'\-]{1,42}?)(?=\s+(?:on|dated|dt|ref|upi|avl|bal|available|not you|if not|call|\d{2}[/-]\d{2})|[.,;\n]|$)"""
    )

    private val knownBanks = listOf(
        "Chase" to listOf("chase"),
        "Bank of America" to listOf("bank of america", "bofa", "bankofamerica"),
        "Citi" to listOf("citibank", "citi "),
        "Wells Fargo" to listOf("wells fargo", "wellsfargo"),
        "Capital One" to listOf("capital one", "capitalone"),
        "American Express" to listOf("american express", "amex"),
        "Discover" to listOf("discover"),
        "HDFC Bank" to listOf("hdfc", "hdfcbk"),
        "ICICI Bank" to listOf("icici", "icicib"),
        "SBI" to listOf("sbi", "sbiinb", "state bank"),
        "Axis Bank" to listOf("axis bank", "axisbk"),
        "Kotak Bank" to listOf("kotak"),
        "HSBC" to listOf("hsbc"),
        "Barclays" to listOf("barclays"),
        "Monzo" to listOf("monzo"),
        "Revolut" to listOf("revolut"),
        "PayPal" to listOf("paypal"),
        "Venmo" to listOf("venmo"),
        "Zelle" to listOf("zelle")
    )

    fun parseBatchText(
        rawBlock: String,
        defaultCurrency: String = "USD",
        existingHashes: Set<String> = emptySet()
    ): List<ParsedSmsCandidate> {
        val chunks = splitIntoMessages(rawBlock)
        val now = System.currentTimeMillis()
        return chunks.mapIndexedNotNull { idx, chunk ->
            parseMessage(
                rawMessage = chunk,
                sender = "",
                timestampMillis = now - (idx * 1000L),
                defaultCurrency = defaultCurrency,
                existingHashes = existingHashes
            )
        }
    }

    fun parseMessage(
        rawMessage: String,
        sender: String = "",
        timestampMillis: Long = System.currentTimeMillis(),
        defaultCurrency: String = "USD",
        existingHashes: Set<String> = emptySet()
    ): ParsedSmsCandidate? {
        val trimmed = rawMessage.trim()
        if (trimmed.length < 10) return null
        val lower = trimmed.lowercase(Locale.ROOT)
        val senderLower = sender.lowercase(Locale.ROOT)

        // Exclude OTP & promotional messages unless clearly a completed transaction
        val hasOtpSignal = otpOrSpamSignals.any { lower.contains(it) }
        val hasCompletedVerb = debitVerbs.any { lower.contains(it) } || creditVerbs.any { lower.contains(it) }
        if (hasOtpSignal && !lower.contains("debited") && !lower.contains("credited") && !lower.contains("spent")) {
            return null
        }

        val hasBankSignal = bankContextSignals.any { lower.contains(it) } ||
            knownBanks.any { (_, keys) -> keys.any { lower.contains(it) || senderLower.contains(it) } }
        if (!hasBankSignal || !hasCompletedVerb) return null

        // Strip available balance phrase before searching for transaction amount
        val balanceMatch = balancePhraseRegex.find(trimmed)
        val availableBalanceNote = balanceMatch?.value?.trim()?.replace(Regex("\\s+"), " ") ?: ""
        val textWithoutBalance = if (balanceMatch != null) {
            trimmed.removeRange(balanceMatch.range)
        } else {
            trimmed
        }

        val amountExtraction = extractAmountAndCurrency(textWithoutBalance, defaultCurrency)
            ?: extractAmountAndCurrency(trimmed, defaultCurrency)
            ?: return null

        val (amountCents, currency) = amountExtraction
        if (amountCents <= 0L) return null

        val type = classifyTransactionType(lower)
        val instrument = classifyInstrument(lower, senderLower)
        val accountLast4 = extractLast4(trimmed)
        val bankName = detectBankName(lower, sender)
        val merchant = extractMerchant(trimmed, bankName, type)
        val category = inferCategory(lower, merchant, type)
        val hash = sha256("${sender.trim().lowercase(Locale.ROOT)}|${normalizeForHash(trimmed)}")

        return ParsedSmsCandidate(
            localKey = "${hash.take(16)}_$timestampMillis",
            sender = sender.ifBlank { bankName },
            rawMessage = trimmed,
            type = type,
            instrument = instrument,
            amountCents = amountCents,
            currency = currency,
            merchant = merchant,
            category = category,
            bankName = bankName,
            accountLast4 = accountLast4,
            availableBalanceNote = availableBalanceNote,
            transactionTimeMillis = timestampMillis,
            messageHash = hash,
            alreadyImported = existingHashes.contains(hash),
            selected = !existingHashes.contains(hash)
        )
    }

    private fun splitIntoMessages(rawBlock: String): List<String> {
        val cleaned = rawBlock.trim()
        if (cleaned.isEmpty()) return emptyList()
        val paragraphSplit = cleaned.split(Regex("""\n\s*\n+""")).map { it.trim() }.filter { it.isNotEmpty() }
        if (paragraphSplit.size > 1) return paragraphSplit
        val lines = cleaned.lines().map { it.trim() }.filter { it.isNotEmpty() }
        // If multiple single-line alerts were pasted line by line, parse each line that looks like its own alert
        if (lines.size > 1 && lines.count { currencyAmountRegex.containsMatchIn(it) } > 1) {
            return lines
        }
        return listOf(cleaned)
    }

    private fun extractAmountAndCurrency(text: String, fallbackCurrency: String): Pair<Long, String>? {
        val match = currencyAmountRegex.find(text) ?: return null
        val rawCurrency: String
        val rawNumber: String
        when {
            match.groupValues[1].isNotEmpty() -> {
                rawCurrency = match.groupValues[1]
                rawNumber = match.groupValues[2]
            }
            match.groupValues[3].isNotEmpty() -> {
                rawCurrency = match.groupValues[3]
                rawNumber = match.groupValues[4]
            }
            else -> {
                rawNumber = match.groupValues[5]
                rawCurrency = match.groupValues[6]
            }
        }
        val normalizedCurrency = normalizeCurrency(rawCurrency, fallbackCurrency)
        val parsedDouble = rawNumber.replace(",", "").toDoubleOrNull() ?: return null
        val cents = (parsedDouble * 100.0).roundToLong()
        return cents to normalizedCurrency
    }

    private fun normalizeCurrency(token: String, fallback: String): String {
        return when (token.trim().uppercase(Locale.ROOT).removeSuffix(".")) {
            "$", "USD" -> "USD"
            "₹", "RS", "INR" -> "INR"
            "€", "EUR" -> "EUR"
            "£", "GBP" -> "GBP"
            "AED" -> "AED"
            "CAD" -> "CAD"
            "AUD" -> "AUD"
            "SGD" -> "SGD"
            else -> fallback
        }
    }

    private fun classifyTransactionType(lower: String): TransactionType {
        // Explicit "credited to your credit card" is a card bill payment (still money paid or refund)
        if (lower.contains("payment received") && lower.contains("credit card")) {
            return TransactionType.INCOME
        }
        if (lower.contains("refund") || lower.contains("cashback") || lower.contains("reversed") ||
            lower.contains("salary") || lower.contains("deposited") || lower.contains("direct deposit")
        ) {
            return TransactionType.INCOME
        }
        val debitScore = debitVerbs.count { lower.contains(it) }
        val creditScore = creditVerbs.count { lower.contains(it) }
        return when {
            lower.contains("debited") || lower.contains("spent") || lower.contains("withdrawn") ||
                lower.contains("charged") || lower.contains("purchased") -> TransactionType.EXPENSE
            lower.contains("credited") || lower.contains("received") -> TransactionType.INCOME
            debitScore >= creditScore -> TransactionType.EXPENSE
            else -> TransactionType.INCOME
        }
    }

    private fun classifyInstrument(lower: String, senderLower: String): PaymentInstrument {
        return when {
            lower.contains("credit card") || lower.contains("amex") ||
                lower.contains("visa credit") || lower.contains("mastercard") ||
                Regex("""\bcc\b""").containsMatchIn(lower) -> PaymentInstrument.CREDIT_CARD
            lower.contains("debit card") || lower.contains("atm") ||
                lower.contains("pos ") || Regex("""\bdc\b""").containsMatchIn(lower) -> PaymentInstrument.DEBIT_CARD
            lower.contains("upi") || lower.contains("vpa") || lower.contains("zelle") ||
                lower.contains("venmo") || lower.contains("paypal") || lower.contains("gpay") ||
                lower.contains("phonepe") || lower.contains("paytm") || senderLower.contains("paypal") -> PaymentInstrument.UPI_WALLET
            else -> PaymentInstrument.BANK_ACCOUNT
        }
    }

    private fun extractLast4(text: String): String {
        val match = accountLast4Regex.find(text) ?: return ""
        return (match.groupValues[1].ifBlank { match.groupValues[2] }).take(4)
    }

    private fun detectBankName(lower: String, sender: String): String {
        val senderLower = sender.lowercase(Locale.ROOT)
        for ((display, keywords) in knownBanks) {
            if (keywords.any { lower.contains(it) || senderLower.contains(it) }) {
                return display
            }
        }
        val cleanedSender = sender.trim()
            .replace(Regex("^[A-Z]{2}-"), "") // Strip Indian DLT prefix e.g. AD-HDFCBK
            .trim()
        if (cleanedSender.isNotBlank() && cleanedSender.any { it.isLetter() }) {
            return cleanedSender.take(24)
        }
        return "Bank Alert"
    }

    private fun extractMerchant(text: String, bankName: String, type: TransactionType): String {
        val match = merchantAtToRegex.find(text)
        val candidate = match?.groupValues?.getOrNull(1)
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.trimEnd('*', '-', '.', ':', '/')
        if (!candidate.isNullOrBlank() && candidate.length >= 2 &&
            !candidate.lowercase(Locale.ROOT).startsWith("your a/c") &&
            !candidate.lowercase(Locale.ROOT).startsWith("your account")
        ) {
            return candidate.take(60)
        }
        return when {
            text.lowercase(Locale.ROOT).contains("atm") -> "ATM Withdrawal"
            text.lowercase(Locale.ROOT).contains("salary") -> "Salary Deposit"
            text.lowercase(Locale.ROOT).contains("interest") -> "Bank Interest"
            type == TransactionType.INCOME -> "$bankName Credit"
            else -> "$bankName Purchase"
        }
    }

    private fun inferCategory(lower: String, merchant: String, type: TransactionType): String {
        val combined = "$lower ${merchant.lowercase(Locale.ROOT)}"
        if (type == TransactionType.INCOME) {
            return when {
                combined.contains("salary") || combined.contains("payroll") || combined.contains("wages") -> "Salary"
                combined.contains("refund") || combined.contains("cashback") || combined.contains("reversal") -> "Refund & Cashback"
                combined.contains("dividend") || combined.contains("interest") -> "Investment"
                combined.contains("transfer") || combined.contains("upi") || combined.contains("zelle") || combined.contains("venmo") -> "Transfer In"
                else -> "Other Income"
            }
        }
        return when {
            combined.containsAny("whole foods", "trader joe", "walmart", "grocery", "supermarket", "aldi", "kroger", "bigbasket", "blinkit", "instacart") -> "Groceries"
            combined.containsAny("starbucks", "coffee", "cafe", "restaurant", "uber eats", "doordash", "swiggy", "zomato", "mcdonald", "chipotle", "dining", "pizza", "burger") -> "Food & Dining"
            combined.containsAny("uber", "lyft", "ola", "shell", "chevron", "exxon", "fuel", "petrol", "gas", "metro", "transit", "airline", "flight", "parking") -> "Transport & Fuel"
            combined.containsAny("amazon", "target", "best buy", "flipkart", "myntra", "nike", "apple", "store", "mall", "shop") -> "Shopping"
            combined.containsAny("electric", "water", "internet", "verizon", "at&t", "t-mobile", "airtel", "jio", "utility", "bill", "insurance", "rent") -> "Bills & Utilities"
            combined.containsAny("netflix", "spotify", "hulu", "disney", "cinema", "movie", "steam", "playstation", "concert") -> "Entertainment"
            combined.containsAny("pharmacy", "cvs", "walgreens", "hospital", "clinic", "doctor", "dental", "medical", "apollo") -> "Health"
            combined.containsAny("atm", "transfer", "neft", "imps", "ach", "zelle", "venmo", "upi") -> "Transfer"
            else -> "Other"
        }
    }

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any { this.contains(it) }

    private fun normalizeForHash(text: String): String =
        text.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
