package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ChequeOptions
import com.example.model.DocCollectStatus
import com.example.model.LoanCalculatorEngine
import com.example.model.LoanDisbursementComputed
import com.example.model.LoanDisbursementInput
import com.example.model.LoanScheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalFinancialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanCalculatorScreen(
    savedSheets: List<LoanDisbursementInput>,
    currencyCode: String,
    onSaveSheet: (LoanDisbursementInput, (Long) -> Unit) -> Unit,
    onDeleteSheet: (Long) -> Unit
) {
    val context = LocalContext.current
    val finColors = LocalFinancialColors.current

    var viewMode by remember { mutableIntStateOf(0) } // 0 = Form View, 1 = Sheet View
    var activeSheetId by remember { mutableStateOf(0L) }
    var customerName by remember { mutableStateOf("") }
    var scheme by remember { mutableStateOf(LoanScheme.P) }
    var laText by remember { mutableStateOf("") }
    var skText by remember { mutableStateOf("") }
    var pfText by remember { mutableStateOf("") }
    var rtoText by remember { mutableStateOf("") }
    var stpText by remember { mutableStateOf("") }
    var vcText by remember { mutableStateOf("") }
    var fcText by remember { mutableStateOf("") }
    var rtoCashText by remember { mutableStateOf("") }
    var reducingRateText by remember { mutableStateOf("") }
    var tenureText by remember { mutableStateOf("36") }
    var disbursementDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var rcCollect by remember { mutableStateOf(DocCollectStatus.NIL) }
    var insurancePuc by remember { mutableStateOf(DocCollectStatus.NIL) }
    var lightBill by remember { mutableStateOf(DocCollectStatus.NIL) }
    var cheques by remember { mutableStateOf("NIL") }
    var showDatePicker by remember { mutableStateOf(false) }

    fun numVal(s: String): Double = s.replace(",", "").toDoubleOrNull() ?: 0.0
    fun fmtInput(v: Double): String = if (v == 0.0) "" else if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

    fun loadFromSaved(saved: LoanDisbursementInput) {
        activeSheetId = saved.id
        customerName = saved.customerName
        scheme = saved.scheme
        laText = fmtInput(saved.loanAmount)
        skText = fmtInput(saved.loanInsuranceSk)
        pfText = fmtInput(saved.pf)
        rtoText = fmtInput(saved.rto)
        stpText = fmtInput(saved.stp)
        vcText = fmtInput(saved.vc)
        fcText = fmtInput(saved.forcloseFc)
        rtoCashText = fmtInput(saved.rtoCash)
        reducingRateText = fmtInput(saved.reducingRateIt)
        tenureText = saved.tenureMonths.takeIf { it > 0 }?.toString() ?: "36"
        disbursementDateMillis = saved.disbursementDateMillis
        rcCollect = saved.rcCollect
        insurancePuc = saved.insurancePuc
        lightBill = saved.lightBill
        cheques = saved.cheques
    }

    fun resetForm() {
        activeSheetId = 0L
        customerName = ""
        scheme = LoanScheme.P
        laText = ""
        skText = ""
        pfText = ""
        rtoText = ""
        stpText = ""
        vcText = ""
        fcText = ""
        rtoCashText = ""
        reducingRateText = ""
        tenureText = "36"
        disbursementDateMillis = System.currentTimeMillis()
        rcCollect = DocCollectStatus.NIL
        insurancePuc = DocCollectStatus.NIL
        lightBill = DocCollectStatus.NIL
        cheques = "NIL"
    }

    val currentInput = LoanDisbursementInput(
        id = activeSheetId,
        customerName = customerName,
        scheme = scheme,
        loanAmount = numVal(laText),
        loanInsuranceSk = numVal(skText),
        pf = numVal(pfText),
        rto = numVal(rtoText),
        stp = numVal(stpText),
        vc = numVal(vcText),
        forcloseFc = if (scheme == LoanScheme.P) 0.0 else numVal(fcText),
        rtoCash = numVal(rtoCashText),
        reducingRateIt = numVal(reducingRateText),
        tenureMonths = tenureText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
        disbursementDateMillis = disbursementDateMillis,
        rcCollect = rcCollect,
        insurancePuc = insurancePuc,
        lightBill = lightBill,
        cheques = cheques
    )
    val computed = remember(currentInput) {
        LoanCalculatorEngine.computeSheet(currentInput)
    }

    val goldTextColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = finColors.gold,
        focusedLabelColor = finColors.gold,
        cursorColor = finColors.gold,
        unfocusedBorderColor = finColors.border
    )

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
                        text = "Loan Disbursement Sheet",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = finColors.gold
                    )
                    Text(
                        text = "Offline calculator • Form & Printable PDF Sheet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { resetForm() }) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = "New blank sheet",
                        tint = finColors.gold
                    )
                }
            }
        }

        if (savedSheets.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Saved Offline Sheets (${savedSheets.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = finColors.gold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        savedSheets.forEach { item ->
                            FilterChip(
                                selected = activeSheetId == item.id,
                                onClick = { loadFromSaved(item) },
                                label = {
                                    Text("${item.customerName.ifBlank { "Customer" }} (${item.scheme.code})")
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete saved sheet",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                if (activeSheetId == item.id) activeSheetId = 0L
                                                onDeleteSheet(item.id)
                                            }
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = finColors.goldContainer,
                                    selectedLabelColor = finColors.gold
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            val tabs = listOf("Form View", "Sheet View")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = viewMode == idx,
                        onClick = { viewMode = idx },
                        shape = SegmentedButtonDefaults.itemShape(idx, tabs.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = finColors.goldContainer,
                            activeContentColor = finColors.gold
                        ),
                        icon = {
                            Icon(
                                imageVector = if (idx == 0) Icons.Rounded.EditNote else Icons.Rounded.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("loan_mode_tab_$idx")
                    )
                }
            }
        }

        if (viewMode == 0) {
            // --- FORM VIEW ---
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, finColors.gold.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Customer & Scheme",
                            style = MaterialTheme.typography.titleSmall,
                            color = finColors.gold
                        )

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Customer Name") },
                            placeholder = { Text("Enter full name") },
                            singleLine = true,
                            colors = goldTextColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loan_customer_name_input")
                        )

                        Text("Scheme", style = MaterialTheme.typography.labelLarge)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            LoanScheme.entries.forEachIndexed { idx, s ->
                                SegmentedButton(
                                    selected = scheme == s,
                                    onClick = { scheme = s },
                                    shape = SegmentedButtonDefaults.itemShape(idx, LoanScheme.entries.size),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = finColors.goldContainer,
                                        activeContentColor = finColors.gold
                                    ),
                                    label = { Text(s.code, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("scheme_${s.code}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NumericGoldField(
                                value = laText,
                                onValueChange = { laText = it },
                                label = "LA (Loan Amount)",
                                colors = goldTextColors,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("loan_la_input")
                            )
                            NumericGoldField(
                                value = skText,
                                onValueChange = { skText = it },
                                label = "SK (Loan Insurance)",
                                colors = goldTextColors,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("loan_sk_input")
                            )
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
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bank Charges & Deductions",
                                style = MaterialTheme.typography.titleSmall,
                                color = finColors.gold
                            )
                            Text(
                                text = "Total: ${formatDoubleMoney(computed.bankCharges, currencyCode)}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = finColors.expense
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NumericGoldField(
                                value = pfText,
                                onValueChange = { pfText = it },
                                label = "PF",
                                colors = goldTextColors,
                                modifier = Modifier.weight(1f)
                            )
                            NumericGoldField(
                                value = rtoText,
                                onValueChange = { rtoText = it },
                                label = "RTO",
                                colors = goldTextColors,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NumericGoldField(
                                value = stpText,
                                onValueChange = { stpText = it },
                                label = "STP",
                                colors = goldTextColors,
                                modifier = Modifier.weight(1f)
                            )
                            NumericGoldField(
                                value = vcText,
                                onValueChange = { vcText = it },
                                label = "VC",
                                colors = goldTextColors,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (scheme != LoanScheme.P) {
                                NumericGoldField(
                                    value = fcText,
                                    onValueChange = { fcText = it },
                                    label = "FORCLOSE (FC)",
                                    colors = goldTextColors,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("loan_fc_input")
                                )
                            }
                            NumericGoldField(
                                value = rtoCashText,
                                onValueChange = { rtoCashText = it },
                                label = "RTO Cash",
                                colors = goldTextColors,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (scheme == LoanScheme.BT) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = finColors.goldContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "BT Scheme: 10% NOC Hold (${formatDoubleMoney(computed.nocHold, currencyCode)}) applied automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = finColors.gold,
                                    modifier = Modifier.padding(10.dp)
                                )
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
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Rate, Tenure & Disbursement Date",
                            style = MaterialTheme.typography.titleSmall,
                            color = finColors.gold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NumericGoldField(
                                value = reducingRateText,
                                onValueChange = { reducingRateText = it },
                                label = "Reducing Rate (IT %)",
                                colors = goldTextColors,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("loan_rate_input")
                            )
                            OutlinedTextField(
                                value = tenureText,
                                onValueChange = { tenureText = it.filter { ch -> ch.isDigit() }.take(3) },
                                label = { Text("Tenure (Months)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = goldTextColors,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("loan_tenure_input")
                            )
                        }

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, finColors.gold.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = finColors.gold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Disbursement Date: ${formatDateOnly(disbursementDateMillis)}  •  EMI Start: ${formatDateOnly(computed.emiStartDateMillis)}",
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Document Checklist",
                            style = MaterialTheme.typography.titleSmall,
                            color = finColors.gold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DropdownField(
                                label = "RC Collect",
                                selected = rcCollect.label,
                                options = DocCollectStatus.entries.map { it.label },
                                onSelect = { rcCollect = DocCollectStatus.fromRaw(it) },
                                colors = goldTextColors,
                                modifier = Modifier.weight(1f)
                            )
                            DropdownField(
                                label = "Insurance PUC",
                                selected = insurancePuc.label,
                                options = DocCollectStatus.entries.map { it.label },
                                onSelect = { insurancePuc = DocCollectStatus.fromRaw(it) },
                                colors = goldTextColors,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DropdownField(
                                label = "Light Bill",
                                selected = lightBill.label,
                                options = DocCollectStatus.entries.map { it.label },
                                onSelect = { lightBill = DocCollectStatus.fromRaw(it) },
                                colors = goldTextColors,
                                modifier = Modifier.weight(1f)
                            )
                            DropdownField(
                                label = "Cheques",
                                selected = cheques,
                                options = ChequeOptions.all,
                                onSelect = { cheques = it },
                                colors = goldTextColors,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                // Live Preview & Actions
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = finColors.goldContainer,
                    border = BorderStroke(1.dp, finColors.gold.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("EMI (Reducing on LA+SK)", style = MaterialTheme.typography.labelSmall, color = finColors.gold)
                            Text(
                                text = formatDoubleMoney(computed.emiReducing, currencyCode),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("In Hand", style = MaterialTheme.typography.labelSmall, color = finColors.gold)
                            Text(
                                text = formatDoubleMoney(computed.inHand, currencyCode),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = finColors.income
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSaveSheet(currentInput) { newId -> activeSheetId = newId }
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, finColors.gold),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, tint = finColors.gold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Sheet", color = finColors.gold)
                    }
                    Button(
                        onClick = { viewMode = 1 },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = finColors.gold,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .testTag("open_sheet_view_button")
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Sheet View", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // --- SHEET VIEW (Exact Order & Gold-on-Black Premium Style) ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { LoanPdfPrinter.printOrSavePdf(context, computed, currencyCode) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = finColors.gold,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("print_pdf_button")
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Print / Save as PDF", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            onSaveSheet(currentInput) { newId -> activeSheetId = newId }
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, finColors.gold),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, tint = finColors.gold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", color = finColors.gold)
                    }
                }
            }

            item {
                LoanSheetTableCard(
                    sheet = computed,
                    currencyCode = currencyCode
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = disbursementDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { disbursementDateMillis = it }
                        showDatePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun LoanSheetTableCard(
    sheet: LoanDisbursementComputed,
    currencyCode: String
) {
    val finColors = LocalFinancialColors.current
    val customerHeader = sheet.input.customerName.trim().ifEmpty { "CUSTOMER NAME" }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0C10)),
        border = BorderStroke(1.5.dp, finColors.gold.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Customer Name Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF2B2006), Color(0xFF14171F))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = customerHeader.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = finColors.gold
                    )
                    Text(
                        text = "Scheme: ${sheet.input.scheme.fullName}  •  Date: ${formatDateOnly(sheet.input.disbursementDateMillis)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            HorizontalDivider(color = finColors.gold.copy(alpha = 0.5f))

            // Exact Sheet Row Order
            SheetRow("Loan Amount", formatDoubleMoney(sheet.input.loanAmount, currencyCode), index = 0)
            SheetRow("Loan Insurance (SK)", formatDoubleMoney(sheet.input.loanInsuranceSk, currencyCode), index = 1)
            SheetRow("Total Loan", formatDoubleMoney(sheet.totalLoan, currencyCode), highlight = finColors.gold, index = 2)
            SheetRow("Loan Amount", formatDoubleMoney(sheet.input.loanAmount, currencyCode), index = 3)
            SheetRow("Bank Charges", formatDoubleMoney(sheet.bankCharges, currencyCode), highlight = finColors.expense, index = 4)
            SheetRow("Disbursment Amount", formatDoubleMoney(sheet.disbursementAmount, currencyCode), highlight = finColors.skyBlue, index = 5)
            SheetRow("NOC Hold", formatDoubleMoney(sheet.nocHold, currencyCode), index = 6)
            SheetRow("Forclose", formatDoubleMoney(sheet.forcloseAmount, currencyCode), index = 7)
            SheetRow("Inhand", formatDoubleMoney(sheet.inHand, currencyCode), highlight = finColors.income, index = 8)
            SheetRow("RTO Cash", formatDoubleMoney(sheet.input.rtoCash, currencyCode), index = 9)

            // PAYMENT TERM Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(finColors.goldContainer)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = "PAYMENT TERM",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = finColors.gold
                )
            }
            HorizontalDivider(color = finColors.gold.copy(alpha = 0.35f))

            SheetRow("First Payment by Bank", formatDoubleMoney(sheet.firstPaymentByBank, currencyCode), highlight = finColors.skyBlue, index = 10)
            SheetRow("NOC Hold Payment", formatDoubleMoney(sheet.nocHold, currencyCode), index = 11)
            SheetRow("Forclose", formatDoubleMoney(sheet.forcloseAmount, currencyCode), index = 12)
            SheetRow("In Hand", formatDoubleMoney(sheet.inHand, currencyCode), highlight = finColors.income, index = 13)

            HorizontalDivider(color = finColors.gold.copy(alpha = 0.4f))

            SheetRow("EMI", formatDoubleMoney(sheet.emiReducing, currencyCode), highlight = finColors.gold, index = 14)
            SheetRow("Month", "${sheet.input.tenureMonths}", index = 15)
            SheetRow("Reducing Rate", LoanCalculatorEngine.formatPercent(sheet.input.reducingRateIt), index = 16)
            SheetRow("Flat Rate", LoanCalculatorEngine.formatPercent(sheet.flatRate), index = 17)
            SheetRow("Per Lac Per Year", formatDoubleMoney(sheet.perLacPerYear, currencyCode), index = 18)
            SheetRow("Per Month", formatDoubleMoney(sheet.perMonth, currencyCode), index = 19)
            SheetRow("Per Month Interest", formatDoubleMoney(sheet.perMonthInterest, currencyCode), index = 20)
            SheetRow("EMI Start Date", formatDateOnly(sheet.emiStartDateMillis), highlight = finColors.skyBlue, index = 21)
            SheetRow("RC Collect", sheet.input.rcCollect.label, index = 22)
            SheetRow("Insurance PUC", sheet.input.insurancePuc.label, index = 23)
            SheetRow("Lightbill", sheet.input.lightBill.label, index = 24)
            SheetRow("Cheques", sheet.input.cheques, index = 25)

            // Mandatory Disbursement Note
            Surface(
                color = Color(0xFF141108),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.disbursement_note),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = finColors.gold,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SheetRow(
    label: String,
    value: String,
    highlight: Color? = null,
    index: Int
) {
    val bg = if (index % 2 == 1) Color(0xFF10131B) else Color(0xFF0A0C10)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight != null) FontWeight.SemiBold else FontWeight.Normal,
            color = if (highlight != null) Color.White else Color(0xFFCBD5E1)
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = highlight ?: Color.White
        )
    }
    HorizontalDivider(color = Color(0xFF1A202C), thickness = 0.5.dp)
}

@Composable
private fun NumericGoldField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    colors: androidx.compose.material3.TextFieldColors,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            onValueChange(raw.filter { it.isDigit() || it == '.' })
        },
        label = { Text(label) },
        placeholder = { Text("0") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        colors = colors,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = colors,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatDoubleMoney(amount: Double, currencyCode: String): String {
    val cents = (amount * 100.0).roundToLong()
    return formatCurrencyCents(cents, currencyCode)
}

private fun formatDateOnly(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
