@file:OptIn(ExperimentalMaterial3Api::class)

package com.spendnote.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendnote.app.data.Categories
import com.spendnote.app.data.QuickParse
import com.spendnote.app.data.Txn
import com.spendnote.app.ui.theme.ExpenseRed
import com.spendnote.app.ui.theme.IncomeGreen
import com.spendnote.app.util.Dates
import com.spendnote.app.util.Money
import java.time.LocalDate

@Composable
fun AddEditScreen(
    existing: Txn?,
    currency: String,
    onSave: (Txn) -> Unit,
    onDelete: (Txn) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isExpense by remember { mutableStateOf(existing?.isExpense ?: true) }
    var amountText by remember { mutableStateOf(existing?.let { Money.plain(it.amount) } ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: Categories.expense.first().name) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var date by remember { mutableStateOf(existing?.let { LocalDate.ofEpochDay(it.epochDay) } ?: LocalDate.now()) }
    var quick by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val cats = if (isExpense) Categories.expense else Categories.income

    fun switchType(expense: Boolean) {
        isExpense = expense
        val list = if (expense) Categories.expense else Categories.income
        if (list.none { it.name == category }) category = list.first().name
    }

    fun applyQuick() {
        if (quick.isBlank()) return
        val p = QuickParse.parse(quick)
        p.amount?.let { amountText = Money.plain(it) }
        isExpense = p.isExpense
        category = p.category
        if (p.note.isNotBlank()) note = p.note
        quick = ""
        error = if (p.amount == null) "No amount found. Type it in the amount box." else null
    }

    fun trySave() {
        val amt = amountText.toDoubleOrNull()
        if (amt == null || amt <= 0) {
            error = "Enter an amount greater than 0"
            return
        }
        onSave(
            Txn(
                id = existing?.id ?: 0,
                amount = amt,
                isExpense = isExpense,
                category = category,
                note = note.trim(),
                epochDay = date.toEpochDay(),
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New entry" else "Edit entry") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (existing == null) {
                Panel {
                    Text("Quick entry", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Type something like “Tea 20” or “Salary 15000” and tap Fill.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quick,
                        onValueChange = { quick = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Petrol 300") },
                        trailingIcon = { TextButton(onClick = { applyQuick() }) { Text("Fill") } },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { applyQuick() })
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeButton("Expense", isExpense, ExpenseRed, Modifier.weight(1f)) { switchType(true) }
                TypeButton("Income", !isExpense, IncomeGreen, Modifier.weight(1f)) { switchType(false) }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { v -> amountText = v.filter { it.isDigit() || it == '.' }; error = null },
                label = { Text("Amount") },
                prefix = { Text(currency, fontSize = 22.sp) },
                textStyle = MaterialTheme.typography.headlineSmall,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cats.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { c ->
                            CategoryTile(c, c.name == category, Modifier.weight(1f)) { category = c.name }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                        date.year, date.monthValue - 1, date.dayOfMonth
                    ).show()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("${Dates.dayLabel(date)}  •  ${Dates.full(date)}")
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            error?.let { Text(it, color = ExpenseRed) }

            Button(
                onClick = { trySave() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExpense) ExpenseRed else IncomeGreen,
                    contentColor = Color.White
                )
            ) {
                Text(
                    if (existing == null) (if (isExpense) "Save expense" else "Save income") else "Save changes",
                    fontSize = 16.sp
                )
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("${existing.category} • ${Money.fmt(existing.amount, currency)} will be removed and your balance updated.") },
            confirmButton = {
                TextButton(onClick = { onDelete(existing) }) { Text("Delete", color = ExpenseRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TypeButton(label: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) color else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}
