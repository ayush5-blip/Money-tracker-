package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.OfflineLedgerRepository
import com.example.data.OfflineSessionUser
import com.example.model.BankTransaction
import com.example.model.TransactionUpdate
import com.example.model.UiState
import com.example.ui.theme.LocalFinancialColors
import com.example.viewmodel.LedgerViewModel

const val ROUTE_OVERVIEW = "overview"
const val ROUTE_SCAN = "scan"
const val ROUTE_LEDGER = "ledger"
const val ROUTE_LOAN = "loan"
const val ROUTE_ACCOUNTS = "accounts"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navTabs = listOf(
    NavTab(ROUTE_OVERVIEW, R.string.nav_overview, Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    NavTab(ROUTE_SCAN, R.string.nav_scan, Icons.Rounded.Sms, Icons.Outlined.Sms),
    NavTab(ROUTE_LEDGER, R.string.nav_ledger, Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    NavTab(ROUTE_LOAN, R.string.nav_loan, Icons.Rounded.Calculate, Icons.Outlined.Calculate),
    NavTab(ROUTE_ACCOUNTS, R.string.nav_accounts, Icons.Rounded.CreditCard, Icons.Outlined.CreditCard)
)

@Composable
fun LedgerAuthenticatedApp(
    sessionUser: OfflineSessionUser,
    sharedIncomingText: String?,
    onConsumedSharedText: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val finColors = LocalFinancialColors.current

    val viewModel: LedgerViewModel = viewModel(
        key = sessionUser.userId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                LedgerViewModel(OfflineLedgerRepository(app), sessionUser.userId)
            }
        }
    )

    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredLedgerTransactions.collectAsStateWithLifecycle()
    val preferences by viewModel.preferencesState.collectAsStateWithLifecycle()
    val savedLoanSheets by viewModel.savedLoanSheets.collectAsStateWithLifecycle()
    val periodFilter by viewModel.periodFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val instrumentFilter by viewModel.instrumentFilter.collectAsStateWithLifecycle()
    val pasteInput by viewModel.pasteInput.collectAsStateWithLifecycle()
    val pastedCandidates by viewModel.pastedCandidates.collectAsStateWithLifecycle()
    val inboxScanState by viewModel.inboxScanState.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    var showManualSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<BankTransaction?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var scanInitialTab by remember { mutableStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(sharedIncomingText) {
        if (!sharedIncomingText.isNullOrBlank()) {
            viewModel.updatePasteInput(sharedIncomingText)
            scanInitialTab = 1
            navController.navigate(ROUTE_SCAN) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onConsumedSharedText()
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                val activeRoute = currentDestination?.route
                if (activeRoute == ROUTE_OVERVIEW || activeRoute == ROUTE_LEDGER) {
                    FloatingActionButton(
                        onClick = { showManualSheet = true },
                        containerColor = finColors.skyBlue,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_transaction")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Log manual transaction"
                        )
                    }
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = Color(0xFF07090D)) {
                        navTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = finColors.skyBlue,
                                    selectedTextColor = finColors.skyBlue,
                                    indicatorColor = finColors.skyBlueContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = Color(0xFF07090D)) {
                        navTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_OVERVIEW,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_OVERVIEW) {
                        OverviewScreen(
                            username = sessionUser.username,
                            transactionsState = transactionsState,
                            savedLoanSheets = savedLoanSheets,
                            preferences = preferences,
                            periodFilter = periodFilter,
                            onPeriodChange = viewModel::setPeriodFilter,
                            onNavigateToScan = {
                                scanInitialTab = 0
                                navigateToTab(ROUTE_SCAN)
                            },
                            onNavigateToLedger = { navigateToTab(ROUTE_LEDGER) },
                            onNavigateToLoanSheet = { navigateToTab(ROUTE_LOAN) },
                            onOpenManualAdd = { showManualSheet = true },
                            onOpenBudgetSettings = { showBudgetDialog = true },
                            onLogout = onLogout,
                            onTransactionClick = { editingTransaction = it }
                        )
                    }
                    composable(ROUTE_SCAN) {
                        ScanScreen(
                            inboxScanState = inboxScanState,
                            pasteInput = pasteInput,
                            pastedCandidates = pastedCandidates,
                            isMutating = isMutating,
                            initialTab = scanInitialTab,
                            onScanInbox = { viewModel.scanSmsInbox(context) },
                            onPasteInputChange = viewModel::updatePasteInput,
                            onToggleCandidate = viewModel::toggleCandidateSelection,
                            onUpdateCandidate = viewModel::updateCandidateDetails,
                            onImportSelected = viewModel::importSelectedCandidates
                        )
                    }
                    composable(ROUTE_LEDGER) {
                        TransactionsScreen(
                            transactions = filteredTransactions,
                            searchQuery = searchQuery,
                            typeFilter = typeFilter,
                            instrumentFilter = instrumentFilter,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onTypeFilterChange = viewModel::setTypeFilter,
                            onInstrumentFilterChange = viewModel::setInstrumentFilter,
                            onTransactionClick = { editingTransaction = it }
                        )
                    }
                    composable(ROUTE_LOAN) {
                        LoanCalculatorScreen(
                            savedSheets = savedLoanSheets,
                            currencyCode = preferences.defaultCurrency,
                            onSaveSheet = viewModel::saveLoanSheet,
                            onDeleteSheet = viewModel::deleteLoanSheet
                        )
                    }
                    composable(ROUTE_ACCOUNTS) {
                        val allItems = (transactionsState as? UiState.Success)?.data.orEmpty()
                        AccountsScreen(
                            transactions = allItems,
                            preferences = preferences,
                            sessionUser = sessionUser,
                            onOpenBudgetSettings = { showBudgetDialog = true },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }

    if (showManualSheet) {
        AddEditTransactionSheet(
            existing = null,
            defaultCurrency = preferences.defaultCurrency,
            onDismiss = { showManualSheet = false },
            onSave = { type, inst, amountCents, currency, merchant, category, bank, last4, note, timeMs, loanCents, roi, tenure, emiCents ->
                viewModel.addManualTransaction(
                    type = type,
                    instrument = inst,
                    amountCents = amountCents,
                    currency = currency,
                    merchant = merchant,
                    category = category,
                    bankName = bank,
                    accountLast4 = last4,
                    note = note,
                    transactionTimeMillis = timeMs,
                    loanAmountCents = loanCents,
                    roiPercent = roi,
                    tenureMonths = tenure,
                    emiCents = emiCents
                )
            }
        )
    }

    editingTransaction?.let { tx ->
        AddEditTransactionSheet(
            existing = tx,
            defaultCurrency = preferences.defaultCurrency,
            onDismiss = { editingTransaction = null },
            onSave = { type, inst, amountCents, currency, merchant, category, bank, last4, note, timeMs, loanCents, roi, tenure, emiCents ->
                viewModel.updateTransaction(
                    transactionId = tx.id,
                    update = TransactionUpdate(
                        type = type,
                        instrument = inst,
                        amountCents = amountCents,
                        currency = currency,
                        merchant = merchant,
                        category = category,
                        bankName = bank,
                        accountLast4 = last4,
                        note = note,
                        transactionTimeMillis = timeMs,
                        loanAmountCents = loanCents,
                        roiPercent = roi,
                        tenureMonths = tenure,
                        emiCents = emiCents
                    )
                )
            },
            onDelete = { id ->
                viewModel.deleteTransaction(id)
            }
        )
    }

    if (showBudgetDialog) {
        BudgetSettingsDialog(
            currentCurrency = preferences.defaultCurrency,
            currentBudgetCents = preferences.monthlyBudgetCents,
            onDismiss = { showBudgetDialog = false },
            onSave = { currency, budgetCents ->
                viewModel.savePreferences(currency, budgetCents)
            }
        )
    }
}
