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
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.OfflineSessionUser
import com.example.model.BankTransaction
import com.example.model.PaymentInstrument
import com.example.model.TransactionType
import com.example.model.UserPreferences
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalFinancialColors

private data class AccountSummary(
    val key: String,
    val title: String,
    val instrument: PaymentInstrument,
    val last4: String,
    val currency: String,
    val incomeCents: Long,
    val expenseCents: Long,
    val count: Int
)

@Composable
fun AccountsScreen(
    transactions: List<BankTransaction>,
    preferences: UserPreferences,
    sessionUser: OfflineSessionUser,
    onOpenBudgetSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val finColors = LocalFinancialColors.current
    val summaries = remember(transactions) {
        transactions
            .groupBy { tx ->
                val bank = tx.bankName.ifBlank { tx.resolvedInstrument().label }
                val inst = tx.resolvedInstrument()
                val last4 = tx.accountLast4
                Triple(bank, inst, last4)
            }
            .map { (groupKey, items) ->
                val (bank, inst, last4) = groupKey
                AccountSummary(
                    key = "${bank}_${inst.name}_$last4",
                    title = bank,
                    instrument = inst,
                    last4 = last4,
                    currency = items.firstOrNull()?.currency ?: preferences.defaultCurrency,
                    incomeCents = items.filter { it.resolvedType() == TransactionType.INCOME }.sumOf { it.amountCents },
                    expenseCents = items.filter { it.resolvedType() == TransactionType.EXPENSE }.sumOf { it.amountCents },
                    count = items.size
                )
            }
            .sortedByDescending { it.expenseCents + it.incomeCents }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Cards & Offline Vault",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = finColors.skyBlue
                )
                Text(
                    text = "Isolated local cards, accounts, and session controls for ${sessionUser.username}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (summaries.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, finColors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreditCard,
                            contentDescription = null,
                            tint = finColors.skyBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Text("No cards or bank accounts detected yet", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Once you scan bank SMS or add transactions, each card and account will be summarized here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(summaries, key = { it.key }) { acc ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, finColors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(finColors.skyBlueContainer)
                            ) {
                                Icon(
                                    imageVector = instrumentIcon(acc.instrument),
                                    contentDescription = acc.instrument.label,
                                    tint = finColors.skyBlue
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = buildString {
                                        append(acc.title)
                                        if (acc.last4.isNotBlank()) append(" ••${acc.last4}")
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${acc.instrument.label} • ${acc.count} transaction${if (acc.count == 1) "" else "s"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Credited / In",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = finColors.income
                                )
                                Text(
                                    text = formatCurrencyCents(acc.incomeCents, acc.currency),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = finColors.income,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Debited / Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = finColors.expense
                                )
                                Text(
                                    text = formatCurrencyCents(acc.expenseCents, acc.currency),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = finColors.expense,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, finColors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = finColors.skyBlue
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Budget & Currency", style = MaterialTheme.typography.titleSmall, color = finColors.skyBlue)
                            Text(
                                text = "Default: ${preferences.defaultCurrency}" +
                                    if (preferences.monthlyBudgetCents > 0L) {
                                        " • Monthly Goal: ${formatCurrencyCents(preferences.monthlyBudgetCents, preferences.defaultCurrency)}"
                                    } else {
                                        " • No budget cap set"
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenBudgetSettings,
                            border = BorderStroke(1.dp, finColors.skyBlue)
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, tint = finColors.skyBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit", color = finColors.skyBlue)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, finColors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(finColors.skyBlueContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = finColors.skyBlue
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${sessionUser.displayName} (@${sessionUser.username})",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = finColors.income,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "100% offline • Isolated from other accounts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onLogout,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = finColors.expense,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
