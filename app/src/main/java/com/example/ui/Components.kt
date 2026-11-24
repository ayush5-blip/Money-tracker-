package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BankTransaction
import com.example.model.PaymentInstrument
import com.example.model.TransactionSource
import com.example.model.TransactionType
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalFinancialColors
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.math.max

fun formatCurrencyCents(cents: Long, currencyCode: String, showPlus: Boolean = false): String {
    val absAmount = kotlin.math.abs(cents) / 100.0
    val formatted = try {
        val fmt = NumberFormat.getCurrencyInstance(Locale.getDefault())
        fmt.currency = Currency.getInstance(currencyCode.uppercase(Locale.ROOT))
        fmt.format(absAmount)
    } catch (_: Exception) {
        val symbol = currencySymbol(currencyCode)
        "$symbol${String.format(Locale.US, "%,.2f", absAmount)}"
    }
    return when {
        cents < 0 -> "-$formatted"
        showPlus && cents > 0 -> "+$formatted"
        else -> formatted
    }
}

fun currencySymbol(currencyCode: String): String = when (currencyCode.uppercase(Locale.ROOT)) {
    "USD", "CAD", "AUD", "SGD" -> "$"
    "INR" -> "₹"
    "EUR" -> "€"
    "GBP" -> "£"
    "AED" -> "AED "
    else -> "$currencyCode "
}

fun formatShortDate(millis: Long): String {
    if (millis <= 0L) return "Today"
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun categoryIcon(category: String): ImageVector = when (category) {
    "Food & Dining" -> Icons.Rounded.Restaurant
    "Groceries" -> Icons.Rounded.ShoppingCart
    "Shopping" -> Icons.Rounded.ShoppingBag
    "Transport & Fuel" -> Icons.Rounded.DirectionsCar
    "Bills & Utilities" -> Icons.Rounded.Bolt
    "Loan EMI", "Loan Disbursement" -> Icons.Rounded.Calculate
    "Entertainment" -> Icons.Rounded.Movie
    "Health" -> Icons.Rounded.LocalHospital
    "Transfer", "Transfer In" -> Icons.Rounded.SwapHoriz
    "Salary" -> Icons.Rounded.Payments
    "Refund & Cashback", "Investment", "Other Income" -> Icons.Rounded.Savings
    else -> Icons.Rounded.AccountBalanceWallet
}

fun instrumentIcon(instrument: PaymentInstrument): ImageVector = when (instrument) {
    PaymentInstrument.DEBIT_CARD, PaymentInstrument.CREDIT_CARD -> Icons.Rounded.CreditCard
    PaymentInstrument.BANK_ACCOUNT -> Icons.Rounded.AccountBalance
    PaymentInstrument.UPI_WALLET -> Icons.Rounded.AccountBalanceWallet
    PaymentInstrument.CASH -> Icons.Rounded.Payments
}

@Composable
fun TransactionRowCard(
    transaction: BankTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val finColors = LocalFinancialColors.current
    val type = transaction.resolvedType()
    val instrument = transaction.resolvedInstrument()
    val source = transaction.resolvedSource()

    val isIncome = type == TransactionType.INCOME
    val amountColor = if (isIncome) finColors.income else finColors.expense
    val badgeBg = finColors.skyBlueContainer
    val badgeFg = finColors.skyBlue

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, finColors.border),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isIncome) finColors.incomeContainer else finColors.expenseContainer
                        )
                ) {
                    Icon(
                        imageVector = categoryIcon(transaction.category),
                        contentDescription = transaction.category,
                        tint = amountColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = transaction.merchant.ifBlank { "Transaction" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBg
                        ) {
                            val accountLabel = buildString {
                                append(instrument.shortLabel)
                                if (transaction.accountLast4.isNotBlank()) {
                                    append(" ••${transaction.accountLast4}")
                                }
                            }
                            Text(
                                text = accountLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeFg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = buildString {
                            if (transaction.bankName.isNotBlank()) {
                                append(transaction.bankName)
                                append(" • ")
                            }
                            append(formatShortDate(transaction.transactionTimeMillis))
                            if (source != TransactionSource.MANUAL) {
                                append(" • ${source.label}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = formatCurrencyCents(
                        cents = if (isIncome) transaction.amountCents else -transaction.amountCents,
                        currencyCode = transaction.currency,
                        showPlus = true
                    ),
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = amountColor
                )
            }

            if (transaction.hasLoanMetadata()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = finColors.skyBlueContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val parts = buildList {
                        if (transaction.loanAmountCents > 0L) {
                            add("Loan: ${formatCurrencyCents(transaction.loanAmountCents, transaction.currency)}")
                        }
                        if (transaction.roiPercent > 0.0) {
                            add("ROI: ${String.format(Locale.US, "%.2f%%", transaction.roiPercent)}")
                        }
                        if (transaction.tenureMonths > 0) {
                            add("Tenure: ${transaction.tenureMonths}m")
                        }
                        if (transaction.emiCents > 0L) {
                            add("EMI: ${formatCurrencyCents(transaction.emiCents, transaction.currency)}")
                        }
                    }
                    Text(
                        text = parts.joinToString("  •  "),
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = finColors.skyBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

data class DailyBarPoint(
    val label: String,
    val incomeCents: Long,
    val expenseCents: Long
)

@Composable
fun IncomeExpenseBarChart(
    points: List<DailyBarPoint>,
    modifier: Modifier = Modifier
) {
    val finColors = LocalFinancialColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxVal = max(
        1L,
        points.maxOfOrNull { max(it.incomeCents, it.expenseCents) } ?: 1L
    ).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val w = size.width
            val h = size.height
            drawLine(
                color = gridColor,
                start = Offset(0f, h * 0.5f),
                end = Offset(w, h * 0.5f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, h - 1.dp.toPx()),
                end = Offset(w, h - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )

            if (points.isEmpty()) return@Canvas
            val slotWidth = w / points.size.toFloat()
            val barWidth = (slotWidth * 0.28f).coerceAtMost(14.dp.toPx())
            val gap = 3.dp.toPx()

            points.forEachIndexed { idx, pt ->
                val centerX = idx * slotWidth + slotWidth / 2f
                val incomeHeight = (pt.incomeCents / maxVal) * (h - 10.dp.toPx())
                val expenseHeight = (pt.expenseCents / maxVal) * (h - 10.dp.toPx())

                if (incomeHeight > 0f) {
                    drawRoundRect(
                        color = finColors.income,
                        topLeft = Offset(centerX - barWidth - gap / 2f, h - incomeHeight),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
                if (expenseHeight > 0f) {
                    drawRoundRect(
                        color = finColors.expense,
                        topLeft = Offset(centerX + gap / 2f, h - expenseHeight),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
