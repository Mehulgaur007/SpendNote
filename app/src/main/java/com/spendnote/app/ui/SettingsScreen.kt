@file:OptIn(ExperimentalMaterial3Api::class)

package com.spendnote.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendnote.app.AppSettings
import com.spendnote.app.MainViewModel
import com.spendnote.app.ui.theme.ExpenseRed
import com.spendnote.app.util.Money
import java.time.LocalDate

private val currencies = listOf("₹", "\$", "€", "£", "¥", "AED ", "SAR ")

@Composable
fun SettingsScreen(vm: MainViewModel, settings: AppSettings) {
    val context = LocalContext.current
    var editOpening by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            vm.exportCsv(uri) { ok ->
                Toast.makeText(context, if (ok) "Backup saved" else "Couldn't save the backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.importCsv(uri) { n ->
                Toast.makeText(
                    context,
                    if (n >= 0) "Imported $n entries" else "Couldn't read that file. Pick a SpendNote CSV backup.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Panel {
            SettingRow(
                title = "Opening balance",
                subtitle = "The money you had when you started",
                value = Money.fmt(settings.openingBalance, settings.currency)
            ) { editOpening = true }
        }

        Panel {
            Text("Currency symbol", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currencies.forEach { c ->
                    FilterChip(
                        selected = settings.currency == c,
                        onClick = { vm.setCurrency(c) },
                        label = { Text(c.trim()) }
                    )
                }
            }
        }

        Panel {
            Text("Backup", fontWeight = FontWeight.Medium)
            SettingRow(
                title = "Export to CSV",
                subtitle = "Save all entries as a file. Opens in Excel or Google Sheets."
            ) { exportLauncher.launch("spendnote-backup-${LocalDate.now()}.csv") }
            SettingRow(
                title = "Import from CSV",
                subtitle = "Restore entries from a SpendNote backup. Adds to existing entries."
            ) {
                importLauncher.launch(arrayOf("text/*", "text/csv", "application/csv", "application/vnd.ms-excel", "application/octet-stream"))
            }
        }

        Panel {
            SettingRow(
                title = "Delete all data",
                subtitle = "Removes every entry, budget and setting from this phone",
                titleColor = ExpenseRed
            ) { confirmClear = true }
        }

        Text(
            "SpendNote works fully offline. Your data stays on this phone unless you export it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }

    if (editOpening) {
        AmountDialog(
            title = "Opening balance",
            initial = settings.openingBalance,
            currency = settings.currency,
            allowNegative = true,
            onDismiss = { editOpening = false },
            onConfirm = { vm.setOpeningBalance(it); editOpening = false }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete all data?") },
            text = { Text("This can't be undone. Export a backup first if you want to keep your entries.") },
            confirmButton = {
                TextButton(onClick = { vm.clearAll(); confirmClear = false }) { Text("Delete everything", color = ExpenseRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }
}
