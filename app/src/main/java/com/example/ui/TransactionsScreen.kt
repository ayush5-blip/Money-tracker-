package com.example.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BankTransaction
import com.example.model.PaymentInstrument
import com.example.model.TransactionType

@Composable
fun TransactionsScreen(
    transactions: List<BankTransaction>,
    searchQuery: String,
    typeFilter: TransactionType?,
    instrumentFilter: PaymentInstrument?,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onInstrumentFilterChange: (PaymentInstrument?) -> Unit,
    onTransactionClick: (BankTransaction) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Transaction Ledger",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search merchant, bank, card, or category…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ledger_search_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = typeFilter == null && instrumentFilter == null,
                    onClick = {
                        onTypeFilterChange(null)
                        onInstrumentFilterChange(null)
                    },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = typeFilter == TransactionType.EXPENSE,
                    onClick = {
                        onTypeFilterChange(
                            if (typeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                        )
                    },
                    label = { Text("Expenses") }
                )
                FilterChip(
                    selected = typeFilter == TransactionType.INCOME,
                    onClick = {
                        onTypeFilterChange(
                            if (typeFilter == TransactionType.INCOME) null else TransactionType.INCOME
                        )
                    },
                    label = { Text("Income") }
                )
                FilterChip(
                    selected = instrumentFilter == PaymentInstrument.DEBIT_CARD,
                    onClick = {
                        onInstrumentFilterChange(
                            if (instrumentFilter == PaymentInstrument.DEBIT_CARD) null else PaymentInstrument.DEBIT_CARD
                        )
                    },
                    label = { Text("Debit Card") }
                )
                FilterChip(
                    selected = instrumentFilter == PaymentInstrument.CREDIT_CARD,
                    onClick = {
                        onInstrumentFilterChange(
                            if (instrumentFilter == PaymentInstrument.CREDIT_CARD) null else PaymentInstrument.CREDIT_CARD
                        )
                    },
                    label = { Text("Credit Card") }
                )
                FilterChip(
                    selected = instrumentFilter == PaymentInstrument.BANK_ACCOUNT,
                    onClick = {
                        onInstrumentFilterChange(
                            if (instrumentFilter == PaymentInstrument.BANK_ACCOUNT) null else PaymentInstrument.BANK_ACCOUNT
                        )
                    },
                    label = { Text("Bank Account") }
                )
            }
        }

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No matching transactions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Try adjusting filters or scan/log a transaction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRowCard(
                        transaction = tx,
                        onClick = { onTransactionClick(tx) }
                    )
                }
                item { Spacer(modifier = Modifier.height(76.dp)) }
            }
        }
    }
}
