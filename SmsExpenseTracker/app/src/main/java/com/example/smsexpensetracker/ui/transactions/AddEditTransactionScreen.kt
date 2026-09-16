package com.example.smsexpensetracker.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smsexpensetracker.data.model.TransactionCategory
import com.example.smsexpensetracker.data.model.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: TransactionViewModel,
    onDone: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Navigate back once saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    val isEditing = state.id > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Transaction" else "Add Transaction", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::save, enabled = !state.isLoading) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor     = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Amount ─────────────────────────────────────────────────────
            OutlinedTextField(
                value         = state.amountText,
                onValueChange = viewModel::onAmountChange,
                label         = { Text("Amount (₹)") },
                placeholder   = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError       = state.error != null,
                supportingText = state.error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            // ── Type (Debit / Credit) ──────────────────────────────────────
            SectionLabel("Transaction Type")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.values().forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick  = { viewModel.onTypeChange(type) },
                        label    = { Text(type.name) }
                    )
                }
            }

            // ── Category ───────────────────────────────────────────────────
            SectionLabel("Category")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Row 1
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TransactionCategory.UPI, TransactionCategory.ATM).forEach { cat ->
                        FilterChip(
                            selected = state.category == cat,
                            onClick  = { viewModel.onCategoryChange(cat) },
                            label    = { Text(cat.name.replace("_", " ")) }
                        )
                    }
                }
                // Row 2
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        TransactionCategory.CREDIT_CARD,
                        TransactionCategory.CREDIT_CARD_PAYMENT,
                        TransactionCategory.OTHER
                    ).forEach { cat ->
                        FilterChip(
                            selected = state.category == cat,
                            onClick  = { viewModel.onCategoryChange(cat) },
                            label    = { Text(cat.name.replace("_", " ")) }
                        )
                    }
                }
            }

            // ── Bank Name ──────────────────────────────────────────────────
            OutlinedTextField(
                value         = state.bankName,
                onValueChange = viewModel::onBankNameChange,
                label         = { Text("Bank Name (optional)") },
                placeholder   = { Text("e.g. HDFC, SBI") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            // ── Description ────────────────────────────────────────────────
            OutlinedTextField(
                value         = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label         = { Text("Description (optional)") },
                placeholder   = { Text("e.g. AMAZON, Petrol") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            // ── Date display ───────────────────────────────────────────────
            SectionLabel("Date")
            DatePickerRow(
                dateMillis = state.dateMillis,
                onDateSelected = viewModel::onDateChange
            )

            // ── Save button ────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = viewModel::save,
                enabled  = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isEditing) "Update Transaction" else "Save Transaction")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerRow(dateMillis: Long, onDateSelected: (Long) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val formatted  = Instant.ofEpochMilli(dateMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))

    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
        Text(formatted)
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateSelected(it) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}
