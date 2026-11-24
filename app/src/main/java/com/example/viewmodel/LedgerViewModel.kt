package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OfflineLedgerRepository
import com.example.model.BankTransaction
import com.example.model.LoanDisbursementInput
import com.example.model.ParsedSmsCandidate
import com.example.model.PaymentInstrument
import com.example.model.TransactionCategories
import com.example.model.TransactionDraft
import com.example.model.TransactionSource
import com.example.model.TransactionType
import com.example.model.TransactionUpdate
import com.example.model.UiState
import com.example.model.UserPreferences
import com.example.scanner.SmsInboxReader
import com.example.scanner.SmsParserEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PeriodFilter(val label: String) {
    THIS_MONTH("This Month"),
    LAST_30_DAYS("30 Days"),
    ALL_TIME("All Time")
}

sealed interface InboxScanUiState {
    data object Idle : InboxScanUiState
    data object Scanning : InboxScanUiState
    data class Completed(
        val totalSmsChecked: Int,
        val candidates: List<ParsedSmsCandidate>
    ) : InboxScanUiState
    data class Error(val message: String) : InboxScanUiState
}

class LedgerViewModel(
    private val repository: OfflineLedgerRepository,
    private val currentUserId: String
) : ViewModel() {

    val transactionsState: StateFlow<UiState<List<BankTransaction>>> =
        repository.observeTransactions(currentUserId)
            .map<List<BankTransaction>, UiState<List<BankTransaction>>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing offline transactions", error)
                emit(UiState.Error(error.message ?: "Failed to load transactions"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val preferencesState: StateFlow<UserPreferences> =
        repository.observePreferences(currentUserId)
            .catch { error ->
                if (error is CancellationException) throw error
                emit(UserPreferences(userId = currentUserId, defaultCurrency = "INR", monthlyBudgetCents = 0L))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UserPreferences(userId = currentUserId, defaultCurrency = "INR", monthlyBudgetCents = 0L)
            )

    val savedLoanSheets: StateFlow<List<LoanDisbursementInput>> =
        repository.observeLoanSheets(currentUserId)
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    private val _periodFilter = MutableStateFlow(PeriodFilter.THIS_MONTH)
    val periodFilter: StateFlow<PeriodFilter> = _periodFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<TransactionType?>(null)
    val typeFilter: StateFlow<TransactionType?> = _typeFilter.asStateFlow()

    private val _instrumentFilter = MutableStateFlow<PaymentInstrument?>(null)
    val instrumentFilter: StateFlow<PaymentInstrument?> = _instrumentFilter.asStateFlow()

    private val _pasteInput = MutableStateFlow("")
    val pasteInput: StateFlow<String> = _pasteInput.asStateFlow()

    private val _pastedCandidates = MutableStateFlow<List<ParsedSmsCandidate>>(emptyList())
    val pastedCandidates: StateFlow<List<ParsedSmsCandidate>> = _pastedCandidates.asStateFlow()

    private val _inboxScanState = MutableStateFlow<InboxScanUiState>(InboxScanUiState.Idle)
    val inboxScanState: StateFlow<InboxScanUiState> = _inboxScanState.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val existingHashes: StateFlow<Set<String>> = transactionsState
        .map { state ->
            when (state) {
                is UiState.Success -> state.data.mapNotNull { tx ->
                    tx.messageHash.takeIf { it.isNotBlank() }
                }.toSet()
                else -> emptySet()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet()
        )

    val filteredLedgerTransactions: StateFlow<List<BankTransaction>> = combine(
        transactionsState,
        _searchQuery,
        _typeFilter,
        _instrumentFilter
    ) { state, query, typeF, instF ->
        val items = (state as? UiState.Success)?.data.orEmpty()
        val q = query.trim().lowercase()
        items.filter { tx ->
            val matchesType = typeF == null || tx.resolvedType() == typeF
            val matchesInstrument = instF == null || tx.resolvedInstrument() == instF
            val matchesQuery = q.isEmpty() ||
                tx.merchant.lowercase().contains(q) ||
                tx.bankName.lowercase().contains(q) ||
                tx.category.lowercase().contains(q) ||
                tx.accountLast4.lowercase().contains(q) ||
                tx.note.lowercase().contains(q)
            matchesType && matchesInstrument && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    fun setPeriodFilter(filter: PeriodFilter) {
        _periodFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: TransactionType?) {
        _typeFilter.value = type
    }

    fun setInstrumentFilter(instrument: PaymentInstrument?) {
        _instrumentFilter.value = instrument
    }

    fun updatePasteInput(rawText: String) {
        _pasteInput.value = rawText
        val parsed = SmsParserEngine.parseBatchText(
            rawBlock = rawText,
            defaultCurrency = preferencesState.value.defaultCurrency,
            existingHashes = existingHashes.value
        )
        _pastedCandidates.value = parsed
    }

    fun scanSmsInbox(context: Context) {
        if (_inboxScanState.value is InboxScanUiState.Scanning) return
        val appContext = context.applicationContext
        viewModelScope.launch {
            _inboxScanState.value = InboxScanUiState.Scanning
            try {
                val result = SmsInboxReader.scanInbox(
                    context = appContext,
                    defaultCurrency = preferencesState.value.defaultCurrency,
                    existingHashes = existingHashes.value
                )
                _inboxScanState.value = InboxScanUiState.Completed(
                    totalSmsChecked = result.totalMessagesScanned,
                    candidates = result.detectedTransactions
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                Log.w(TAG, "READ_SMS permission denied during inbox scan", e)
                _inboxScanState.value = InboxScanUiState.Error(
                    "SMS read permission is required to scan your inbox. Grant permission or use Paste Bank Text."
                )
            } catch (e: Exception) {
                Log.e(TAG, "SMS inbox scan failed", e)
                _inboxScanState.value = InboxScanUiState.Error(
                    e.localizedMessage ?: "Unable to read SMS inbox."
                )
            }
        }
    }

    fun toggleCandidateSelection(fromInbox: Boolean, localKey: String) {
        if (fromInbox) {
            val current = _inboxScanState.value as? InboxScanUiState.Completed ?: return
            _inboxScanState.value = current.copy(
                candidates = current.candidates.map {
                    if (it.localKey == localKey) it.copy(selected = !it.selected) else it
                }
            )
        } else {
            _pastedCandidates.update { list ->
                list.map { if (it.localKey == localKey) it.copy(selected = !it.selected) else it }
            }
        }
    }

    fun updateCandidateDetails(
        fromInbox: Boolean,
        localKey: String,
        newType: TransactionType,
        newCategory: String,
        newMerchant: String
    ) {
        val mapper: (ParsedSmsCandidate) -> ParsedSmsCandidate = { c ->
            if (c.localKey == localKey) {
                val validCategory = if (newCategory in TransactionCategories.forType(newType)) {
                    newCategory
                } else {
                    TransactionCategories.forType(newType).first()
                }
                c.copy(
                    type = newType,
                    category = validCategory,
                    merchant = newMerchant.trim().ifBlank { c.merchant }
                )
            } else {
                c
            }
        }
        if (fromInbox) {
            val current = _inboxScanState.value as? InboxScanUiState.Completed ?: return
            _inboxScanState.value = current.copy(candidates = current.candidates.map(mapper))
        } else {
            _pastedCandidates.update { it.map(mapper) }
        }
    }

    fun importSelectedCandidates(fromInbox: Boolean) {
        val candidates = if (fromInbox) {
            (_inboxScanState.value as? InboxScanUiState.Completed)?.candidates.orEmpty()
        } else {
            _pastedCandidates.value
        }
        val toImport = candidates.filter { it.selected }
        if (toImport.isEmpty()) {
            _snackbarMessage.tryEmit("Select at least one parsed alert to import.")
            return
        }
        val source = if (fromInbox) TransactionSource.SMS_INBOX else TransactionSource.TEXT_PASTE
        val drafts = toImport.map { it.toDraft(source) }

        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.addTransactionsBatch(currentUserId, drafts)
            _isMutating.value = false
            result.onSuccess { count ->
                val importedHashes = toImport.map { it.messageHash }.toSet()
                if (fromInbox) {
                    val current = _inboxScanState.value as? InboxScanUiState.Completed
                    if (current != null) {
                        _inboxScanState.value = current.copy(
                            candidates = current.candidates.map {
                                if (it.messageHash in importedHashes) {
                                    it.copy(alreadyImported = true, selected = false)
                                } else {
                                    it
                                }
                            }
                        )
                    }
                } else {
                    _pasteInput.value = ""
                    _pastedCandidates.value = emptyList()
                }
                _snackbarMessage.tryEmit("Saved $count transaction${if (count == 1) "" else "s"} offline.")
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Failed to import transactions.")
            }
        }
    }

    fun addManualTransaction(
        type: TransactionType,
        instrument: PaymentInstrument,
        amountCents: Long,
        currency: String,
        merchant: String,
        category: String,
        bankName: String,
        accountLast4: String,
        note: String,
        transactionTimeMillis: Long,
        loanAmountCents: Long = 0L,
        roiPercent: Double = 0.0,
        tenureMonths: Int = 0,
        emiCents: Long = 0L
    ) {
        val draft = TransactionDraft(
            type = type,
            instrument = instrument,
            amountCents = amountCents,
            currency = currency,
            merchant = merchant,
            category = category,
            bankName = bankName,
            accountLast4 = accountLast4,
            source = TransactionSource.MANUAL,
            messageHash = "",
            note = note,
            transactionTimeMillis = transactionTimeMillis,
            loanAmountCents = loanAmountCents,
            roiPercent = roiPercent,
            tenureMonths = tenureMonths,
            emiCents = emiCents
        )
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.addTransaction(currentUserId, draft)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.tryEmit("${type.label} saved offline.")
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Failed to save transaction.")
            }
        }
    }

    fun updateTransaction(
        transactionId: Long,
        update: TransactionUpdate
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateTransaction(currentUserId, transactionId, update)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.tryEmit("Transaction updated.")
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Failed to update transaction.")
            }
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteTransaction(currentUserId, transactionId)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.tryEmit("Transaction deleted.")
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Failed to delete transaction.")
            }
        }
    }

    fun savePreferences(defaultCurrency: String, monthlyBudgetCents: Long) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.savePreferences(currentUserId, defaultCurrency, monthlyBudgetCents)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.tryEmit("Preferences updated.")
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Failed to save preferences.")
            }
        }
    }

    fun saveLoanSheet(input: LoanDisbursementInput, onSavedId: (Long) -> Unit = {}) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.saveLoanSheet(currentUserId, input)
            _isMutating.value = false
            result.onSuccess { id ->
                onSavedId(id)
                _snackbarMessage.tryEmit("Loan sheet saved offline for ${input.customerName.ifBlank { "Customer" }}.")
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Failed to save loan sheet.")
            }
        }
    }

    fun deleteLoanSheet(sheetId: Long) {
        viewModelScope.launch {
            val result = repository.deleteLoanSheet(currentUserId, sheetId)
            result.onSuccess {
                _snackbarMessage.tryEmit("Loan sheet deleted.")
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Failed to delete loan sheet.")
            }
        }
    }

    fun emitMessage(message: String) {
        _snackbarMessage.tryEmit(message)
    }

    private companion object {
        const val TAG = "LedgerVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
