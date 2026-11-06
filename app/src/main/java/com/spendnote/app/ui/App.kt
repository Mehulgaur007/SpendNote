@file:OptIn(ExperimentalMaterial3Api::class)

package com.spendnote.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendnote.app.MainViewModel
import com.spendnote.app.data.Txn

private data class EditTarget(val txn: Txn?)

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Insights("Insights", Icons.Filled.PieChart),
    Budget("Budget", Icons.Filled.AccountBalanceWallet),
    Settings("Settings", Icons.Filled.Settings)
}

@Composable
fun SpendNoteApp(vm: MainViewModel = viewModel()) {
    val txns by vm.transactions.collectAsStateWithLifecycle()
    val budgets by vm.budgets.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    val target = editing

    when {
        !settings.setupDone -> SetupScreen(settings.currency) { vm.setOpeningBalance(it) }

        target != null -> {
            BackHandler { editing = null }
            key(target) {
                AddEditScreen(
                    existing = target.txn,
                    currency = settings.currency,
                    onSave = { vm.save(it); editing = null },
                    onDelete = { vm.delete(it); editing = null },
                    onBack = { editing = null }
                )
            }
        }

        else -> Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tab == Tab.Home) {
                    ExtendedFloatingActionButton(
                        onClick = { editing = EditTarget(null) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("Add entry") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    Tab.Home -> HomeScreen(txns, budgets, settings, onOpen = { editing = EditTarget(it) })
                    Tab.Insights -> InsightsScreen(txns, settings.currency)
                    Tab.Budget -> BudgetScreen(
                        txns = txns,
                        budgets = budgets,
                        currency = settings.currency,
                        onSet = { c, a -> vm.setBudget(c, a) },
                        onRemove = { c -> vm.removeBudget(c) }
                    )
                    Tab.Settings -> SettingsScreen(vm, settings)
                }
            }
        }
    }
}

@Composable
fun SetupScreen(currency: String, onDone: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("👛", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Welcome to SpendNote",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "How much money do you have right now? Every expense you add is subtracted from this balance, and every income is added.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' } },
                prefix = { Text(currency) },
                label = { Text("Current balance") },
                placeholder = { Text("3000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onDone(text.toDoubleOrNull() ?: 0.0) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Start tracking")
            }
        }
    }
}
