package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.ParsedSmsCandidate
import com.example.model.TransactionCategories
import com.example.model.TransactionType
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalFinancialColors
import com.example.viewmodel.InboxScanUiState

@Composable
fun ScanScreen(
    inboxScanState: InboxScanUiState,
    pasteInput: String,
    pastedCandidates: List<ParsedSmsCandidate>,
    isMutating: Boolean,
    initialTab: Int = 0,
    onScanInbox: () -> Unit,
    onPasteInputChange: (String) -> Unit,
    onToggleCandidate: (fromInbox: Boolean, localKey: String) -> Unit,
    onUpdateCandidate: (fromInbox: Boolean, localKey: String, TransactionType, String, String) -> Unit,
    onImportSelected: (fromInbox: Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val finColors = LocalFinancialColors.current
    var selectedMode by remember(initialTab) { mutableIntStateOf(initialTab) }
    var permissionDeniedNote by remember { mutableStateOf(false) }
    var editingCandidate by remember { mutableStateOf<Pair<Boolean, ParsedSmsCandidate>?>(null) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = false
            onScanInbox()
        } else {
            permissionDeniedNote = true
        }
    }

    val modes = listOf("SMS Inbox Scan", "Paste Bank Text")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Scan Bank & Card Alerts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Detects debit, credit, UPI, and bank deposit texts while filtering out OTPs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedMode == idx,
                        onClick = { selectedMode = idx },
                        shape = SegmentedButtonDefaults.itemShape(idx, modes.size),
                        label = { Text(label) }
                    )
                }
            }
        }

        if (selectedMode == 0) {
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "On-Device SMS Parsing",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = stringResource(R.string.sms_permission_rationale),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (permissionDeniedNote) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "SMS permission was declined. You can grant it in Settings or switch to Paste Bank Text above.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_SMS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    permissionDeniedNote = false
                                    onScanInbox()
                                } else {
                                    smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                }
                            },
                            enabled = inboxScanState !is InboxScanUiState.Scanning,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("scan_inbox_sms_button")
                        ) {
                            if (inboxScanState is InboxScanUiState.Scanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Scanning SMS Inbox…")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Sms,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Bank & Card SMS")
                            }
                        }
                    }
                }
            }

            when (inboxScanState) {
                is InboxScanUiState.Idle, is InboxScanUiState.Scanning -> Unit
                is InboxScanUiState.Error -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = inboxScanState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                is InboxScanUiState.Completed -> {
                    item {
                        val selectedCount = inboxScanState.candidates.count { it.selected }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Checked ${inboxScanState.totalSmsChecked} SMS • ${inboxScanState.candidates.size} alerts found",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (selectedCount > 0) {
                                Button(
                                    onClick = { onImportSelected(true) },
                                    enabled = !isMutating,
                                    modifier = Modifier.testTag("import_inbox_selected_button")
                                ) {
                                    Text("Import ($selectedCount)")
                                }
                            }
                        }
                    }

                    if (inboxScanState.candidates.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
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
                                        imageVector = Icons.Rounded.MarkEmailRead,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "No bank transaction SMS found in inbox",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "If your messages are in another app or you want to test an alert format, switch to Paste Bank Text.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(onClick = { selectedMode = 1 }) {
                                        Text("Switch to Paste Bank Text")
                                    }
                                }
                            }
                        }
                    } else {
                        items(inboxScanState.candidates, key = { it.localKey }) { candidate ->
                            ParsedCandidateCard(
                                candidate = candidate,
                                onToggle = { onToggleCandidate(true, candidate.localKey) },
                                onEdit = { editingCandidate = true to candidate }
                            )
                        }
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Paste Bank / Debit / Credit Text", style = MaterialTheme.typography.titleSmall)
                            TextButton(
                                onClick = {
                                    val clip = clipboard.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        onPasteInputChange(clip)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = "Paste from clipboard",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Paste")
                            }
                        }

                        OutlinedTextField(
                            value = pasteInput,
                            onValueChange = onPasteInputChange,
                            placeholder = { Text(stringResource(R.string.paste_sms_placeholder)) },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("paste_sms_input")
                        )

                        Text(
                            text = "Tip: Paste multiple SMS alerts separated by blank lines. You can also Share any message directly to LedgerPulse.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (pasteInput.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { onPasteInputChange("") }) {
                                    Text("Clear")
                                }
                            }
                        }
                    }
                }
            }

            if (pastedCandidates.isNotEmpty()) {
                item {
                    val selectedCount = pastedCandidates.count { it.selected }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected ${pastedCandidates.size} transaction${if (pastedCandidates.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Button(
                            onClick = { onImportSelected(false) },
                            enabled = selectedCount > 0 && !isMutating,
                            modifier = Modifier.testTag("import_pasted_button")
                        ) {
                            Text("Save Selected ($selectedCount)")
                        }
                    }
                }

                items(pastedCandidates, key = { it.localKey }) { candidate ->
                    ParsedCandidateCard(
                        candidate = candidate,
                        onToggle = { onToggleCandidate(false, candidate.localKey) },
                        onEdit = { editingCandidate = false to candidate }
                    )
                }
            } else if (pasteInput.trim().length > 10) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No debit or credit alert recognized yet. Make sure the message includes an amount (e.g. $45.20 or Rs. 500) and a transaction action (debited, spent, credited, paid, received).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    editingCandidate?.let { (fromInbox, candidate) ->
        EditCandidateDialog(
            candidate = candidate,
            onDismiss = { editingCandidate = null },
            onSave = { newType, newCategory, newMerchant ->
                onUpdateCandidate(fromInbox, candidate.localKey, newType, newCategory, newMerchant)
                editingCandidate = null
            }
        )
    }
}

@Composable
private fun ParsedCandidateCard(
    candidate: ParsedSmsCandidate,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val finColors = LocalFinancialColors.current
    val isIncome = candidate.type == TransactionType.INCOME

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, finColors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = candidate.selected,
                    onCheckedChange = { onToggle() }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = candidate.merchant,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (candidate.alreadyImported) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Imported",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = buildString {
                            append(candidate.type.label)
                            append(" • ")
                            append(candidate.instrument.shortLabel)
                            if (candidate.accountLast4.isNotBlank()) append(" ••${candidate.accountLast4}")
                            append(" • ")
                            append(candidate.category)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatCurrencyCents(
                        cents = if (isIncome) candidate.amountCents else -candidate.amountCents,
                        currencyCode = candidate.currency,
                        showPlus = true
                    ),
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) finColors.income else finColors.expense,
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit parsed details",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = candidate.rawMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditCandidateDialog(
    candidate: ParsedSmsCandidate,
    onDismiss: () -> Unit,
    onSave: (TransactionType, String, String) -> Unit
) {
    var type by remember { mutableStateOf(candidate.type) }
    var category by remember { mutableStateOf(candidate.category) }
    var merchant by remember { mutableStateOf(candidate.merchant) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Parsed Alert") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TransactionType.entries.forEachIndexed { idx, opt ->
                        SegmentedButton(
                            selected = type == opt,
                            onClick = {
                                type = opt
                                val list = TransactionCategories.forType(opt)
                                if (category !in list) category = list.first()
                            },
                            shape = SegmentedButtonDefaults.itemShape(idx, TransactionType.entries.size),
                            label = { Text(opt.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it.take(120) },
                    label = { Text("Merchant / Source") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
            }
        },
        confirmButton = {
            Button(onClick = { onSave(type, category, merchant) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
