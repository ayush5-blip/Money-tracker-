package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BankTransaction
import com.example.model.LoanCalculatorEngine
import com.example.model.PaymentInstrument
import com.example.model.TransactionCategories
import com.example.model.TransactionType
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalFinancialColors
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionSheet(
    existing: BankTransaction?,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (
        type: TransactionType,
        instrument: PaymentInstrument,
        amountCents: Long,
        currency: String,
        merchant: String,
        category: String,
        bankName: String,
        accountLast4: String,
        note: String,
        timeMillis: Long,
        loanAmountCents: Long,
        roiPercent: Double,
        tenureMonths: Int,
        emiCents: Long
    ) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val finColors = LocalFinancialColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember(existing) {
        mutableStateOf(existing?.resolvedType() ?: TransactionType.EXPENSE)
    }
    var instrument by remember(existing) {
        mutableStateOf(existing?.resolvedInstrument() ?: PaymentInstrument.DEBIT_CARD)
    }
    var amountText by remember(existing) {
        mutableStateOf(
            if (existing != null && existing.amountCents > 0) {
                String.format(Locale.US, "%.2f", existing.amountCents / 100.0)
            } else {
                ""
            }
        )
    }
    var currency by remember(existing) {
        mutableStateOf(existing?.currency ?: defaultCurrency)
    }
    var merchant by remember(existing) { mutableStateOf(existing?.merchant ?: "") }
    var category by remember(existing) {
        mutableStateOf(existing?.category ?: TransactionCategories.forType(type).first())
    }
    var bankName by remember(existing) { mutableStateOf(existing?.bankName ?: "") }
    var accountLast4 by remember(existing) { mutableStateOf(existing?.accountLast4 ?: "") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }

    // Loan / EMI / ROI / Tenure fields
    var includeLoanDetails by remember(existing) {
        mutableStateOf(existing?.hasLoanMetadata() == true)
    }
    var loanAmountText by remember(existing) {
        mutableStateOf(
            if (existing != null && existing.loanAmountCents > 0L) {
                String.format(Locale.US, "%.2f", existing.loanAmountCents / 100.0)
            } else {
                ""
            }
        )
    }
    var roiText by remember(existing) {
        mutableStateOf(
            if (existing != null && existing.roiPercent > 0.0) {
                existing.roiPercent.toString()
            } else {
                ""
            }
        )
    }
    var tenureText by remember(existing) {
        mutableStateOf(
            if (existing != null && existing.tenureMonths > 0) {
                existing.tenureMonths.toString()
            } else {
                ""
            }
        )
    }

    val loanAmountCentsPreview = ((loanAmountText.replace(",", "").toDoubleOrNull() ?: 0.0) * 100.0).roundToLong()
    val roiPreview = roiText.toDoubleOrNull() ?: 0.0
    val tenurePreview = tenureText.toIntOrNull() ?: 0
    val calculatedEmiCents = remember(loanAmountCentsPreview, roiPreview, tenurePreview) {
        LoanCalculatorEngine.calculateReducingEmiCents(loanAmountCentsPreview, roiPreview, tenurePreview)
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing == null) "Log Transaction" else "Edit Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = finColors.skyBlue
                )
                if (existing != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(existing.id)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("delete_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete transaction",
                            tint = finColors.expense
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = type == option,
                        onClick = {
                            type = option
                            val allowed = TransactionCategories.forType(option)
                            if (category !in allowed) category = allowed.first()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, TransactionType.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = if (option == TransactionType.INCOME) finColors.incomeContainer else finColors.expenseContainer,
                            activeContentColor = if (option == TransactionType.INCOME) finColors.income else finColors.expense
                        ),
                        label = { Text(option.label, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.7f)
                        .testTag("amount_input")
                )
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it.uppercase().take(5) },
                    label = { Text("Currency") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it.take(120) },
                label = { Text(if (type == TransactionType.EXPENSE) "Merchant / Paid To" else "Source / Received From") },
                placeholder = { Text("e.g. HDFC Loan EMI or Salary") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("merchant_input")
            )

            // Loan Amount, ROI %, Tenure & Auto-Calculated EMI Section
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, finColors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Loan, ROI, Tenure & EMI",
                                style = MaterialTheme.typography.titleSmall,
                                color = finColors.skyBlue
                            )
                            Text(
                                text = "Attach loan terms and auto-compute EMI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = includeLoanDetails,
                            onCheckedChange = { includeLoanDetails = it }
                        )
                    }

                    AnimatedVisibility(visible = includeLoanDetails) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = loanAmountText,
                                onValueChange = { loanAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Loan Amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = roiText,
                                    onValueChange = { roiText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                    label = { Text("ROI (%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = tenureText,
                                    onValueChange = { tenureText = it.filter { ch -> ch.isDigit() }.take(3) },
                                    label = { Text("Tenure (Months)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (calculatedEmiCents > 0L) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Calculated Monthly EMI", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = formatCurrencyCents(calculatedEmiCents, currency),
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = finColors.gold
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            amountText = String.format(Locale.US, "%.2f", calculatedEmiCents / 100.0)
                                        }
                                    ) {
                                        Text("Use EMI as Amount", color = finColors.skyBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text("Payment Method", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PaymentInstrument.entries.forEach { inst ->
                    FilterChip(
                        selected = instrument == inst,
                        onClick = { instrument = inst },
                        label = { Text(inst.label) }
                    )
                }
            }

            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TransactionCategories.forType(type).forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it.take(60) },
                    label = { Text("Bank / Issuer") },
                    placeholder = { Text("e.g. HDFC") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = accountLast4,
                    onValueChange = { accountLast4 = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Last 4") },
                    placeholder = { Text("4821") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(300) },
                label = { Text("Note (optional)") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (validationError != null) {
                Text(
                    text = validationError ?: "",
                    color = finColors.expense,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val parsedDouble = amountText.replace(",", "").toDoubleOrNull()
                    val cents = if (parsedDouble != null) (parsedDouble * 100.0).roundToLong() else 0L
                    if (cents <= 0L) {
                        validationError = "Please enter an amount greater than 0."
                        return@Button
                    }
                    if (merchant.trim().isEmpty()) {
                        validationError = "Please enter a merchant or source name."
                        return@Button
                    }
                    onSave(
                        type,
                        instrument,
                        cents,
                        currency.trim().ifEmpty { defaultCurrency },
                        merchant.trim(),
                        category,
                        bankName.trim(),
                        accountLast4.trim(),
                        note.trim(),
                        existing?.transactionTimeMillis?.takeIf { it > 0L } ?: System.currentTimeMillis(),
                        if (includeLoanDetails) loanAmountCentsPreview else 0L,
                        if (includeLoanDetails) roiPreview else 0.0,
                        if (includeLoanDetails) tenurePreview else 0,
                        if (includeLoanDetails) calculatedEmiCents else 0L
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = finColors.skyBlue,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_transaction_button")
            ) {
                Text(if (existing == null) "Save Offline" else "Update Transaction", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BudgetSettingsDialog(
    currentCurrency: String,
    currentBudgetCents: Long,
    onDismiss: () -> Unit,
    onSave: (currency: String, budgetCents: Long) -> Unit
) {
    val currencies = listOf("INR", "USD", "EUR", "GBP", "AED", "CAD", "AUD")
    var selectedCurrency by remember { mutableStateOf(currentCurrency) }
    var budgetText by remember {
        mutableStateOf(
            if (currentBudgetCents > 0L) (currentBudgetCents / 100L).toString() else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Currency & Monthly Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Default Currency", style = MaterialTheme.typography.labelLarge)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currencies.forEach { code ->
                        FilterChip(
                            selected = selectedCurrency == code,
                            onClick = { selectedCurrency = code },
                            label = { Text(code) }
                        )
                    }
                }
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Monthly Expense Budget (0 to disable)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = budgetText.toDoubleOrNull() ?: 0.0
                    val cents = (amount * 100.0).roundToLong().coerceAtLeast(0L)
                    onSave(selectedCurrency, cents)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
