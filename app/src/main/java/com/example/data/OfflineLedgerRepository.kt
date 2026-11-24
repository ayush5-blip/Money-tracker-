package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.LoanSheetEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserPreferencesEntity
import com.example.model.BankTransaction
import com.example.model.LoanDisbursementInput
import com.example.model.TransactionDraft
import com.example.model.TransactionUpdate
import com.example.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineLedgerRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val transactionDao = db.transactionDao()
    private val preferencesDao = db.userPreferencesDao()
    private val loanSheetDao = db.loanSheetDao()

    fun observeTransactions(userId: String): Flow<List<BankTransaction>> =
        transactionDao.observeTransactions(userId).map { list ->
            list.map { it.toDomain() }
        }

    fun observePreferences(userId: String): Flow<UserPreferences> =
        preferencesDao.observePreferences(userId).map { entity ->
            entity?.toDomain() ?: UserPreferences(
                userId = userId,
                defaultCurrency = "INR",
                monthlyBudgetCents = 0L
            )
        }

    fun observeLoanSheets(userId: String): Flow<List<LoanDisbursementInput>> =
        loanSheetDao.observeLoanSheets(userId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun addTransaction(userId: String, draft: TransactionDraft): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        require(draft.amountCents > 0L) { "Amount must be greater than zero." }
        transactionDao.insert(
            TransactionEntity(
                userId = userId,
                type = draft.type.name,
                instrument = draft.instrument.name,
                amountCents = draft.amountCents,
                currency = draft.currency.trim().uppercase().ifEmpty { "INR" },
                merchant = draft.merchant.trim().take(120).ifEmpty { "Transaction" },
                category = draft.category.trim().take(50).ifEmpty { "Other" },
                bankName = draft.bankName.trim().take(60),
                accountLast4 = draft.accountLast4.trim().take(12),
                source = draft.source.name,
                messageHash = draft.messageHash.trim().take(128),
                note = draft.note.trim().take(300),
                transactionTimeMillis = draft.transactionTimeMillis.coerceAtLeast(1L),
                loanAmountCents = draft.loanAmountCents.coerceAtLeast(0L),
                roiPercent = draft.roiPercent.coerceAtLeast(0.0),
                tenureMonths = draft.tenureMonths.coerceAtLeast(0),
                emiCents = draft.emiCents.coerceAtLeast(0L),
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        return@runCatching
    }

    suspend fun addTransactionsBatch(userId: String, drafts: List<TransactionDraft>): Result<Int> =
        runCatching {
            if (drafts.isEmpty()) return@runCatching 0
            val now = System.currentTimeMillis()
            val entities = drafts.filter { it.amountCents > 0L }.map { draft ->
                TransactionEntity(
                    userId = userId,
                    type = draft.type.name,
                    instrument = draft.instrument.name,
                    amountCents = draft.amountCents,
                    currency = draft.currency.trim().uppercase().ifEmpty { "INR" },
                    merchant = draft.merchant.trim().take(120).ifEmpty { "Bank Transaction" },
                    category = draft.category.trim().take(50).ifEmpty { "Other" },
                    bankName = draft.bankName.trim().take(60),
                    accountLast4 = draft.accountLast4.trim().take(12),
                    source = draft.source.name,
                    messageHash = draft.messageHash.trim().take(128),
                    note = draft.note.trim().take(300),
                    transactionTimeMillis = draft.transactionTimeMillis.coerceAtLeast(1L),
                    loanAmountCents = draft.loanAmountCents.coerceAtLeast(0L),
                    roiPercent = draft.roiPercent.coerceAtLeast(0.0),
                    tenureMonths = draft.tenureMonths.coerceAtLeast(0),
                    emiCents = draft.emiCents.coerceAtLeast(0L),
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            }
            transactionDao.insertAll(entities)
            entities.size
        }

    suspend fun updateTransaction(
        userId: String,
        transactionId: Long,
        update: TransactionUpdate
    ): Result<Unit> = runCatching {
        require(update.amountCents > 0L) { "Amount must be greater than zero." }
        transactionDao.updateOwned(
            id = transactionId,
            userId = userId,
            type = update.type.name,
            instrument = update.instrument.name,
            amountCents = update.amountCents,
            currency = update.currency.trim().uppercase().ifEmpty { "INR" },
            merchant = update.merchant.trim().take(120).ifEmpty { "Transaction" },
            category = update.category.trim().take(50).ifEmpty { "Other" },
            bankName = update.bankName.trim().take(60),
            accountLast4 = update.accountLast4.trim().take(12),
            note = update.note.trim().take(300),
            transactionTimeMillis = update.transactionTimeMillis.coerceAtLeast(1L),
            loanAmountCents = update.loanAmountCents.coerceAtLeast(0L),
            roiPercent = update.roiPercent.coerceAtLeast(0.0),
            tenureMonths = update.tenureMonths.coerceAtLeast(0),
            emiCents = update.emiCents.coerceAtLeast(0L),
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    suspend fun deleteTransaction(userId: String, transactionId: Long): Result<Unit> = runCatching {
        transactionDao.deleteOwned(transactionId, userId)
    }

    suspend fun savePreferences(
        userId: String,
        defaultCurrency: String,
        monthlyBudgetCents: Long
    ): Result<Unit> = runCatching {
        preferencesDao.upsert(
            UserPreferencesEntity(
                userId = userId,
                defaultCurrency = defaultCurrency.trim().uppercase().ifEmpty { "INR" },
                monthlyBudgetCents = monthlyBudgetCents.coerceAtLeast(0L)
            )
        )
    }

    suspend fun saveLoanSheet(userId: String, input: LoanDisbursementInput): Result<Long> =
        runCatching {
            loanSheetDao.upsert(
                LoanSheetEntity(
                    id = input.id,
                    userId = userId,
                    customerName = input.customerName.trim().ifEmpty { "Customer" },
                    scheme = input.scheme.code,
                    loanAmount = input.loanAmount,
                    loanInsuranceSk = input.loanInsuranceSk,
                    pf = input.pf,
                    rto = input.rto,
                    stp = input.stp,
                    vc = input.vc,
                    forcloseFc = if (input.scheme.code == "P") 0.0 else input.forcloseFc,
                    rtoCash = input.rtoCash,
                    reducingRateIt = input.reducingRateIt,
                    tenureMonths = input.tenureMonths,
                    disbursementDateMillis = input.disbursementDateMillis,
                    rcCollect = input.rcCollect.label,
                    insurancePuc = input.insurancePuc.label,
                    lightBill = input.lightBill.label,
                    cheques = input.cheques,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }

    suspend fun deleteLoanSheet(userId: String, sheetId: Long): Result<Unit> = runCatching {
        loanSheetDao.deleteOwned(sheetId, userId)
    }
}
