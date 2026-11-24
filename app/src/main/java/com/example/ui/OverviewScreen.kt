package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BankTransaction
import com.example.model.LoanDisbursementInput
import com.example.model.PaymentInstrument
import com.example.model.TransactionType
import com.example.model.UiState
import com.example.model.UserPreferences
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalFinancialColors
import com.example.viewmodel.PeriodFilter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun OverviewScreen(
    username: String,
    transactionsState: UiState<List<BankTransaction>>,
    savedLoanSheets: List<LoanDisbursementInput>,
    preferences: UserPreferences,
    periodFilter: PeriodFilter,
    onPeriodChange: (PeriodFilter) -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToLoanSheet: () -> Unit,
    onOpenManualAdd: () -> Unit,
    onOpenBudgetSettings: () -> Unit,
    onLogout: () -> Unit,
    onTransactionClick: (BankTransaction) -> Unit
) {
    val finColors = LocalFinancialColors.current

    when (transactionsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = finColors.skyBlue)
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = transactionsState.message,
                    color = finColors.expense,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        is UiState.Success -> {
            val allTransactions = transactionsState.data
            val filtered = remember(allTransactions, periodFilter) {
                filterByPeriod(allTransactions, periodFilter)
            }
            val displayCurrency = remember(filtered, preferences) {
                filtered.firstOrNull()?.currency ?: preferences.defaultCurrency
            }
            val totalIncome = remember(filtered) {
                filtered.filter { it.resolvedType() == TransactionType.INCOME }.sumOf { it.amountCents }
            }
            val totalExpense = remember(filtered) {
                filtered.filter { it.resolvedType() == TransactionType.EXPENSE }.sumOf { it.amountCents }
            }
            val netBalance = totalIncome - totalExpense

            val debitSpend = remember(filtered) {
                filtered.filter {
                    it.resolvedType() == TransactionType.EXPENSE &&
                        it.resolvedInstrument() == PaymentInstrument.DEBIT_CARD
                }.sumOf { it.amountCents }
            }
            val creditSpend = remember(filtered) {
                filtered.filter {
                    it.resolvedType() == TransactionType.EXPENSE &&
                        it.resolvedInstrument() == PaymentInstrument.CREDIT_CARD
                }.sumOf { it.amountCents }
            }
            val bankSpend = remember(filtered) {
                filtered.filter {
                    it.resolvedType() == TransactionType.EXPENSE &&
                        (it.resolvedInstrument() == PaymentInstrument.BANK_ACCOUNT ||
                            it.resolvedInstrument() == PaymentInstrument.UPI_WALLET)
                }.sumOf { it.amountCents }
            }

            val totalTrackedEmiCents = remember(allTransactions) {
                allTransactions.sumOf { it.emiCents }
            }
            val totalTrackedLoanCents = remember(allTransactions) {
                allTransactions.sumOf { it.loanAmountCents }
            }

            val topCategories = remember(filtered) {
                filtered.filter { it.resolvedType() == TransactionType.EXPENSE }
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amountCents } }
                    .entries
                    .sortedByDescending { it.value }
                    .take(4)
            }

            val chartPoints = remember(filtered) {
                buildLast5DayPoints(filtered)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = finColors.skyBlue
                            )
                            Text(
                                text = "Offline Vault: $username",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(
                                onClick = onOpenBudgetSettings,
                                modifier = Modifier.testTag("open_budget_settings")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = "Currency and budget settings",
                                    tint = finColors.skyBlue
                                )
                            }
                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier.testTag("header_logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = "Logout",
                                    tint = finColors.expense
                                )
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PeriodFilter.entries.forEach { period ->
                            FilterChip(
                                selected = periodFilter == period,
                                onClick = { onPeriodChange(period) },
                                label = { Text(period.label) }
                            )
                        }
                    }
                }

                item {
                    // Hero Net Cashflow Card (Black + SkyBlue + Green Income + Red Expense)
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, finColors.skyBlue.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(finColors.heroStart, finColors.heroEnd)
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Net Cashflow (${periodFilter.label})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = finColors.skyBlue
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = finColors.skyBlueContainer
                                    ) {
                                        Text(
                                            text = "${filtered.size} txns",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = finColors.skyBlue,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = formatCurrencyCents(netBalance, displayCurrency, showPlus = netBalance > 0),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = if (netBalance >= 0) finColors.income else finColors.expense
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = finColors.incomeContainer,
                                        border = BorderStroke(1.dp, finColors.income.copy(alpha = 0.4f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(finColors.income.copy(alpha = 0.2f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ArrowDownward,
                                                    contentDescription = "Income",
                                                    tint = finColors.income,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Income",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = finColors.income
                                                )
                                                Text(
                                                    text = formatCurrencyCents(totalIncome, displayCurrency),
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = finColors.expenseContainer,
                                        border = BorderStroke(1.dp, finColors.expense.copy(alpha = 0.4f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(finColors.expense.copy(alpha = 0.2f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ArrowUpward,
                                                    contentDescription = "Expenses",
                                                    tint = finColors.expense,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Expenses",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = finColors.expense
                                                )
                                                Text(
                                                    text = formatCurrencyCents(totalExpense, displayCurrency),
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToScan,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = finColors.skyBlue,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .testTag("quick_scan_sms_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sms,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan SMS", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenManualAdd,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, finColors.skyBlue),
                            modifier = Modifier
                                .weight(0.95f)
                                .height(48.dp)
                                .testTag("quick_manual_add_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = finColors.skyBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Entry", color = finColors.skyBlue)
                        }
                    }
                }

                // Loan & EMI / Disbursement Sheet Quick Banner
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, finColors.skyBlue.copy(alpha = 0.45f)),
                        onClick = onNavigateToLoanSheet,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(finColors.skyBlueContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Calculate,
                                    contentDescription = "Loan Disbursement Sheet",
                                    tint = finColors.skyBlue
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Loan Disbursement Sheet & EMI Calc",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = finColors.skyBlue
                                )
                                Text(
                                    text = buildString {
                                        append("${savedLoanSheets.size} saved sheet${if (savedLoanSheets.size == 1) "" else "s"}")
                                        if (totalTrackedLoanCents > 0L) {
                                            append(" • Active Loan ${formatCurrencyCents(totalTrackedLoanCents, displayCurrency)}")
                                        }
                                        if (totalTrackedEmiCents > 0L) {
                                            append(" • EMI ${formatCurrencyCents(totalTrackedEmiCents, displayCurrency)}")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("Open", color = finColors.skyBlue, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                if (preferences.monthlyBudgetCents > 0L) {
                    item {
                        val monthExpense = filterByPeriod(allTransactions, PeriodFilter.THIS_MONTH)
                            .filter { it.resolvedType() == TransactionType.EXPENSE }
                            .sumOf { it.amountCents }
                        val progress = (monthExpense.toFloat() / preferences.monthlyBudgetCents.toFloat()).coerceIn(0f, 1f)
                        val overBudget = monthExpense > preferences.monthlyBudgetCents

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, finColors.border)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Monthly Budget", style = MaterialTheme.typography.titleSmall, color = finColors.skyBlue)
                                    Text(
                                        text = "${formatCurrencyCents(monthExpense, displayCurrency)} / ${formatCurrencyCents(preferences.monthlyBudgetCents, displayCurrency)}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (overBudget) finColors.expense else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = if (overBudget) finColors.expense else finColors.skyBlue,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }

                // Debit / Credit / Bank Breakdown Row (SkyBlue accents)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InstrumentMiniCard(
                            title = "Debit Card",
                            amount = formatCurrencyCents(debitSpend, displayCurrency),
                            accent = finColors.skyBlue,
                            icon = Icons.Rounded.CreditCard,
                            modifier = Modifier.weight(1f)
                        )
                        InstrumentMiniCard(
                            title = "Credit Card",
                            amount = formatCurrencyCents(creditSpend, displayCurrency),
                            accent = finColors.skyBlue,
                            icon = Icons.Rounded.CreditCard,
                            modifier = Modifier.weight(1f)
                        )
                        InstrumentMiniCard(
                            title = "Bank / UPI",
                            amount = formatCurrencyCents(bankSpend, displayCurrency),
                            accent = finColors.skyBlue,
                            icon = Icons.Rounded.AccountBalance,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (allTransactions.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, finColors.border)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(finColors.skyBlueContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sms,
                                        contentDescription = null,
                                        tint = finColors.skyBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.empty_ledger_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.empty_ledger_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, finColors.border)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("5-Day Cashflow Pulse", style = MaterialTheme.typography.titleSmall, color = finColors.skyBlue)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        LegendDot("Income", finColors.income)
                                        LegendDot("Expense", finColors.expense)
                                    }
                                }
                                IncomeExpenseBarChart(points = chartPoints)
                            }
                        }
                    }

                    if (topCategories.isNotEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, finColors.border)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Top Expense Categories", style = MaterialTheme.typography.titleSmall, color = finColors.skyBlue)
                                    topCategories.forEach { (cat, cents) ->
                                        val pct = if (totalExpense > 0L) (cents.toFloat() / totalExpense.toFloat()) else 0f
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(cat, style = MaterialTheme.typography.bodySmall)
                                                Text(
                                                    text = "${formatCurrencyCents(cents, displayCurrency)} (${(pct * 100).toInt()}%)",
                                                    fontFamily = JetBrainsMonoFamily,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = finColors.expense
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { pct.coerceIn(0f, 1f) },
                                                color = finColors.expense,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Offline Entries", style = MaterialTheme.typography.titleMedium, color = finColors.skyBlue)
                            TextButton(onClick = onNavigateToLedger) {
                                Text("View All", color = finColors.skyBlue)
                            }
                        }
                    }

                    items(filtered.take(5), key = { it.id }) { tx ->
                        TransactionRowCard(
                            transaction = tx,
                            onClick = { onTransactionClick(tx) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun InstrumentMiniCard(
    title: String,
    amount: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val finColors = LocalFinancialColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, finColors.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amount,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun filterByPeriod(items: List<BankTransaction>, period: PeriodFilter): List<BankTransaction> {
    if (period == PeriodFilter.ALL_TIME) return items
    val cal = Calendar.getInstance()
    val cutoff = when (period) {
        PeriodFilter.THIS_MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        PeriodFilter.LAST_30_DAYS -> System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L
        PeriodFilter.ALL_TIME -> 0L
    }
    return items.filter { it.transactionTimeMillis >= cutoff }
}

private fun buildLast5DayPoints(items: List<BankTransaction>): List<DailyBarPoint> {
    val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val dayMs = 24L * 60L * 60L * 1000L

    return (4 downTo 0).map { daysAgo ->
        val start = cal.timeInMillis - daysAgo * dayMs
        val end = start + dayMs
        val dayItems = items.filter { it.transactionTimeMillis in start until end }
        DailyBarPoint(
            label = dayFmt.format(Date(start)),
            incomeCents = dayItems.filter { it.resolvedType() == TransactionType.INCOME }.sumOf { it.amountCents },
            expenseCents = dayItems.filter { it.resolvedType() == TransactionType.EXPENSE }.sumOf { it.amountCents }
        )
    }
}
