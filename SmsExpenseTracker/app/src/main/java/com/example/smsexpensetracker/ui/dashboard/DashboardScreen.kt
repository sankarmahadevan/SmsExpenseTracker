package com.example.smsexpensetracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.TransactionCategory
import com.example.smsexpensetracker.data.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Tracker", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor     = MaterialTheme.colorScheme.primary,
                    titleContentColor  = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(padding),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Balance Cards ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BalanceCard(
                        modifier  = Modifier.weight(1f),
                        label     = "Savings Balance",
                        amount    = state.savingsBalancePaise,
                        icon      = "🏦",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    BalanceCard(
                        modifier  = Modifier.weight(1f),
                        label     = "CC Spent (cycle)",
                        amount    = state.ccSpentPaise,
                        icon      = "💳",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor   = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // ── Month Picker ───────────────────────────────────────────────
            item {
                MonthPicker(
                    selectedMonth = state.selectedMonth,
                    onPrevious    = viewModel::previousMonth,
                    onNext        = viewModel::nextMonth
                )
            }

            // ── Month Summary ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryChip(
                        modifier = Modifier.weight(1f),
                        label    = "Total Out",
                        amount   = state.monthlyDebitPaise,
                        color    = MaterialTheme.colorScheme.error
                    )
                    SummaryChip(
                        modifier = Modifier.weight(1f),
                        label    = "Total In",
                        amount   = state.monthlyCreditPaise,
                        color    = Color(0xFF2E7D32)
                    )
                }
            }

            // ── Transaction List header ────────────────────────────────────
            item {
                Text(
                    "Transactions",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier   = Modifier.padding(top = 4.dp)
                )
            }

            if (state.transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No transactions this month",
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "SMS transactions are captured automatically",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(state.transactions, key = { it.id }) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onEdit      = { onEditTransaction(transaction.id) },
                        onDelete    = { viewModel.deleteTransaction(transaction) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }  // FAB clearance
        }
    }
}

// ── Balance Card ──────────────────────────────────────────────────────────────
@Composable
private fun BalanceCard(
    modifier: Modifier,
    label: String,
    amount: Long,
    icon: String,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 24.sp)
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
            Text(
                formatRupees(amount),
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = contentColor
            )
        }
    }
}

// ── Month Picker ──────────────────────────────────────────────────────────────
@Composable
private fun MonthPicker(selectedMonth: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    val fmt = DateTimeFormatter.ofPattern("MMMM yyyy")
    val isNow = selectedMonth == YearMonth.now()

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Text(selectedMonth.format(fmt), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onNext, enabled = !isNow) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next month",
                    tint = if (isNow) MaterialTheme.colorScheme.outline else LocalContentColor.current
                )
            }
        }
    }
}

// ── Summary Chip ──────────────────────────────────────────────────────────────
@Composable
private fun SummaryChip(modifier: Modifier, label: String, amount: Long, color: Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.1f)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(formatRupees(amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// ── Transaction Item ──────────────────────────────────────────────────────────
@Composable
private fun TransactionItem(transaction: Transaction, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val icon = when (transaction.category) {
        TransactionCategory.UPI                 -> "📱"
        TransactionCategory.ATM                 -> "🏧"
        TransactionCategory.CREDIT_CARD         -> "💳"
        TransactionCategory.CREDIT_CARD_PAYMENT -> "💰"
        TransactionCategory.OTHER               -> "🔄"
    }
    val amountColor  = if (transaction.type == TransactionType.DEBIT)
        MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
    val amountPrefix = if (transaction.type == TransactionType.DEBIT) "−" else "+"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 26.sp, modifier = Modifier.padding(end = 12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.description.ifBlank { transaction.category.name.replace("_", " ") },
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (transaction.bankName.isNotBlank() && transaction.bankName != "Unknown") {
                        Text(transaction.bankName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    if (transaction.isManual) {
                        Text("manual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text(formatTimestamp(transaction.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$amountPrefix${formatRupees(transaction.amountPaise)}",
                    fontWeight = FontWeight.Bold,
                    color      = amountColor,
                    style      = MaterialTheme.typography.bodyLarge
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete transaction?") },
            text    = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Formatters ────────────────────────────────────────────────────────────────
fun formatRupees(paise: Long): String = "₹${"%.2f".format(paise / 100.0)}"

private fun formatTimestamp(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a"))
