package com.example.smsexpensetracker.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── Savings Account ────────────────────────────────────────────
            SettingsSection(title = "🏦  Savings Account") {
                OutlinedTextField(
                    value         = state.initialBalanceText,
                    onValueChange = viewModel::onBalanceChange,
                    label         = { Text("Initial Balance (₹)") },
                    placeholder   = { Text("e.g. 50000.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError       = state.balanceError != null,
                    supportingText = state.balanceError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    } ?: { Text("One-time setup. All SMS debits/credits are applied on top of this.") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            }

            // ── Credit Card ────────────────────────────────────────────────
            SettingsSection(title = "💳  Credit Card") {
                Text(
                    "Billing cycle reset day (1–28)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Day ${state.creditCardResetDay} of each month",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value         = state.creditCardResetDay.toFloat(),
                    onValueChange = { viewModel.onResetDayChange(it.toInt()) },
                    valueRange    = 1f..28f,
                    steps         = 26,     // 28 positions → 26 interior steps
                    modifier      = Modifier.fillMaxWidth()
                )
                state.resetDayError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    "The CC spent amount resets on this day each month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // ── Privacy note ───────────────────────────────────────────────
            SettingsSection(title = "🔒  Privacy") {
                Text(
                    "This app has NO internet permission. All your data (transactions, " +
                    "settings, SMS history) is stored exclusively in a local SQLite database " +
                    "on your device. Nothing is sent to any server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Android 10+ note ──────────────────────────────────────────
            SettingsSection(title = "ℹ️  SMS Capture Note") {
                Text(
                    "Due to Android 10+ restrictions, only NEW incoming SMS are captured " +
                    "automatically. Past SMS cannot be read unless this app is set as the " +
                    "default SMS app (not recommended). Use the + button to add past " +
                    "transactions manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Save button ────────────────────────────────────────────────
            Button(
                onClick  = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled  = !state.isLoading
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            content()
        }
    }
}
