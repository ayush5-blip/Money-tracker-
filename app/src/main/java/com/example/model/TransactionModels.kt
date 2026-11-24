package com.example.model

import java.util.Calendar
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income");

    companion object {
        fun fromRaw(raw: String?): TransactionType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

enum class PaymentInstrument(val label: String, val shortLabel: String) {
    DEBIT_CARD("Debit Card", "Debit"),
    CREDIT_CARD("Credit Card", "Credit"),
    BANK_ACCOUNT("Bank Account", "Bank"),
    UPI_WALLET("UPI / Wallet", "Wallet"),
    CASH("Cash", "Cash");

    companion object {
        fun fromRaw(raw: String?): PaymentInstrument =
            entries.firstOrNull { it.name == raw } ?: BANK_ACCOUNT
    }
}

enum class TransactionSource(val label: String) {
    SMS_INBOX("SMS Scan"),
    TEXT_PASTE("Text Parse"),
    MANUAL("Manual");

    companion object {
        fun fromRaw(raw: String?): TransactionSource =
            entries.firstOrNull { it.name == raw } ?: MANUAL
    }
}

object TransactionCategories {
    val expenseCategories = listOf(
        "Food & Dining",
        "Groceries",
        "Shopping",
        "Transport & Fuel",
        "Bills & Utilities",
        "Loan EMI",
        "Entertainment",
        "Health",
        "Transfer",
        "Other"
    )

    val incomeCategories = listOf(
        "Salary",
        "Loan Disbursement",
        "Refund & Cashback",
        "Transfer In",
        "Investment",
        "Other Income"
    )

    fun forType(type: TransactionType): List<String> =
        if (type == TransactionType.EXPENSE) expenseCategories else incomeCategories
}

data class BankTransaction(
    val id: Long = 0L,
    val userId: String = "",
    val type: String = TransactionType.EXPENSE.name,
    val instrument: String = PaymentInstrument.BANK_ACCOUNT.name,
    val amountCents: Long = 0L,
    val currency: String = "INR",
    val merchant: String = "",
    val category: String = "Other",
    val bankName: String = "",
    val accountLast4: String = "",
    val source: String = TransactionSource.MANUAL.name,
    val messageHash: String = "",
    val note: String = "",
    val transactionTimeMillis: Long = 0L,
    val loanAmountCents: Long = 0L,
    val roiPercent: Double = 0.0,
    val tenureMonths: Int = 0,
    val emiCents: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun resolvedType(): TransactionType = TransactionType.fromRaw(type)
    fun resolvedInstrument(): PaymentInstrument = PaymentInstrument.fromRaw(instrument)
    fun resolvedSource(): TransactionSource = TransactionSource.fromRaw(source)
    fun hasLoanMetadata(): Boolean = loanAmountCents > 0L || emiCents > 0L || roiPercent > 0.0 || tenureMonths > 0
}

data class UserPreferences(
    val userId: String = "",
    val defaultCurrency: String = "INR",
    val monthlyBudgetCents: Long = 0L
)

data class TransactionDraft(
    val type: TransactionType,
    val instrument: PaymentInstrument,
    val amountCents: Long,
    val currency: String,
    val merchant: String,
    val category: String,
    val bankName: String,
    val accountLast4: String,
    val source: TransactionSource,
    val messageHash: String,
    val note: String,
    val transactionTimeMillis: Long,
    val loanAmountCents: Long = 0L,
    val roiPercent: Double = 0.0,
    val tenureMonths: Int = 0,
    val emiCents: Long = 0L
)

data class TransactionUpdate(
    val type: TransactionType,
    val instrument: PaymentInstrument,
    val amountCents: Long,
    val currency: String,
    val merchant: String,
    val category: String,
    val bankName: String,
    val accountLast4: String,
    val note: String,
    val transactionTimeMillis: Long,
    val loanAmountCents: Long = 0L,
    val roiPercent: Double = 0.0,
    val tenureMonths: Int = 0,
    val emiCents: Long = 0L
)

data class ParsedSmsCandidate(
    val localKey: String,
    val sender: String,
    val rawMessage: String,
    val type: TransactionType,
    val instrument: PaymentInstrument,
    val amountCents: Long,
    val currency: String,
    val merchant: String,
    val category: String,
    val bankName: String,
    val accountLast4: String,
    val availableBalanceNote: String,
    val transactionTimeMillis: Long,
    val messageHash: String,
    val alreadyImported: Boolean = false,
    val selected: Boolean = true
) {
    fun toDraft(source: TransactionSource): TransactionDraft {
        val summaryNote = buildString {
            append(instrument.shortLabel)
            if (accountLast4.isNotBlank()) append(" ••$accountLast4")
            if (availableBalanceNote.isNotBlank()) append(" • $availableBalanceNote")
        }.take(300)
        return TransactionDraft(
            type = type,
            instrument = instrument,
            amountCents = amountCents,
            currency = currency,
            merchant = merchant.take(120).ifBlank { bankName.ifBlank { "Bank Alert" } },
            category = category.take(50),
            bankName = bankName.take(60),
            accountLast4 = accountLast4.take(12),
            source = source,
            messageHash = messageHash.take(128),
            note = summaryNote,
            transactionTimeMillis = transactionTimeMillis
        )
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

// --- Loan Disbursement Sheet Models & Calculator Engine ---

enum class LoanScheme(val code: String, val fullName: String) {
    P("P", "Purchase (P)"),
    R("R", "Refinance (R)"),
    BT("BT", "Balance Transfer (BT)"),
    TU("TU", "Top-Up (TU)");

    companion object {
        fun fromCode(code: String?): LoanScheme =
            entries.firstOrNull { it.code == code } ?: P
    }
}

enum class DocCollectStatus(val label: String) {
    NIL("NIL"),
    YES("YES"),
    NO("NO");

    companion object {
        fun fromRaw(raw: String?): DocCollectStatus =
            entries.firstOrNull { it.label == raw } ?: NIL
    }
}

object ChequeOptions {
    val all = listOf("NIL") + (1..10).map { it.toString() }
}

data class LoanDisbursementInput(
    val id: Long = 0L,
    val customerName: String = "",
    val scheme: LoanScheme = LoanScheme.P,
    val loanAmount: Double = 0.0,
    val loanInsuranceSk: Double = 0.0,
    val pf: Double = 0.0,
    val rto: Double = 0.0,
    val stp: Double = 0.0,
    val vc: Double = 0.0,
    val forcloseFc: Double = 0.0,
    val rtoCash: Double = 0.0,
    val reducingRateIt: Double = 0.0,
    val tenureMonths: Int = 36,
    val disbursementDateMillis: Long = System.currentTimeMillis(),
    val rcCollect: DocCollectStatus = DocCollectStatus.NIL,
    val insurancePuc: DocCollectStatus = DocCollectStatus.NIL,
    val lightBill: DocCollectStatus = DocCollectStatus.NIL,
    val cheques: String = "NIL"
)

data class LoanDisbursementComputed(
    val input: LoanDisbursementInput,
    val totalLoan: Double,
    val bankCharges: Double,
    val disbursementAmount: Double,
    val nocHold: Double,
    val forcloseAmount: Double,
    val firstPaymentByBank: Double,
    val inHand: Double,
    val emiReducing: Double,
    val flatRate: Double,
    val perLacPerYear: Double,
    val perMonth: Double,
    val perMonthInterest: Double,
    val emiStartDateMillis: Long
)

object LoanCalculatorEngine {

    fun calculateReducingEmiCents(
        principalCents: Long,
        annualRatePercent: Double,
        tenureMonths: Int
    ): Long {
        if (principalCents <= 0L || tenureMonths <= 0) return 0L
        val emi = calculateReducingEmi(principalCents / 100.0, annualRatePercent, tenureMonths)
        return (emi * 100.0).roundToLong()
    }

    fun calculateReducingEmi(
        principal: Double,
        annualRatePercent: Double,
        tenureMonths: Int
    ): Double {
        if (principal <= 0.0 || tenureMonths <= 0) return 0.0
        val monthlyRate = annualRatePercent / 12.0 / 100.0
        if (monthlyRate <= 0.0) return principal / tenureMonths.toDouble()
        val factor = (1.0 + monthlyRate).pow(tenureMonths.toDouble())
        val denom = factor - 1.0
        if (denom == 0.0) return principal / tenureMonths.toDouble()
        return (principal * monthlyRate * factor) / denom
    }

    fun computeSheet(input: LoanDisbursementInput): LoanDisbursementComputed {
        val la = input.loanAmount.coerceAtLeast(0.0)
        val sk = input.loanInsuranceSk.coerceAtLeast(0.0)
        val totalLoan = la + sk

        val bankCharges = (input.pf.coerceAtLeast(0.0) +
            input.rto.coerceAtLeast(0.0) +
            input.stp.coerceAtLeast(0.0) +
            input.vc.coerceAtLeast(0.0))

        val disbursementAmount = la - bankCharges
        val nocHold = if (input.scheme == LoanScheme.BT) la * 0.10 else 0.0
        val forcloseAmount = if (input.scheme == LoanScheme.P) 0.0 else input.forcloseFc.coerceAtLeast(0.0)

        val firstPaymentByBank = disbursementAmount - nocHold - forcloseAmount
        val inHand = firstPaymentByBank - input.rtoCash.coerceAtLeast(0.0)

        val reducingRate = input.reducingRateIt.coerceAtLeast(0.0)
        val flatRate = reducingRate * 0.5
        val tenure = input.tenureMonths.coerceAtLeast(0)
        val emiReducing = calculateReducingEmi(totalLoan, reducingRate, tenure)

        val perLacPerYear = 100_000.0 * (flatRate / 100.0)
        val perMonth = perLacPerYear / 12.0
        val totalInterest = if (totalLoan > 0.0 && tenure > 0) {
            ((emiReducing * tenure) - totalLoan).coerceAtLeast(0.0)
        } else {
            0.0
        }
        val perMonthInterest = if (tenure > 0) totalInterest / tenure.toDouble() else 0.0

        val cal = Calendar.getInstance().apply {
            timeInMillis = input.disbursementDateMillis
            add(Calendar.MONTH, 1)
        }

        return LoanDisbursementComputed(
            input = input,
            totalLoan = totalLoan,
            bankCharges = bankCharges,
            disbursementAmount = disbursementAmount,
            nocHold = nocHold,
            forcloseAmount = forcloseAmount,
            firstPaymentByBank = firstPaymentByBank,
            inHand = inHand,
            emiReducing = emiReducing,
            flatRate = flatRate,
            perLacPerYear = perLacPerYear,
            perMonth = perMonth,
            perMonthInterest = perMonthInterest,
            emiStartDateMillis = cal.timeInMillis
        )
    }

    fun formatPercent(value: Double): String =
        String.format(Locale.US, "%.2f%%", value)
}
