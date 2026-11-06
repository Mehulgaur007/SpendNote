@file:OptIn(ExperimentalMaterial3Api::class)

package com.spendnote.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendnote.app.data.Budget
import com.spendnote.app.data.Categories
import com.spendnote.app.data.Category
import com.spendnote.app.data.TOTAL_BUDGET
import com.spendnote.app.data.Txn
import com.spendnote.app.ui.theme.ExpenseRed
import com.spendnote.app.ui.theme.IncomeGreen
import com.spendnote.app.util.Dates
import com.spendnote.app.util.Money
import com.spendnote.app.util.expenseTotal
import com.spendnote.app.util.inMonth
import java.time.LocalDate
import java.time.YearMonth

private data class BudgetDialogState(
    val title: String,
    val category: String?,
    val initial: Double?,
    val pickCategory: Boolean,
    val removable: Boolean
)

@Composable
fun BudgetScreen(
    txns: List<Txn>,
    budgets: List<Budget>,
    currency: String,
    onSet: (String, Double) -> Unit,
    onRemove: (String) -> Unit
) {
    val month = YearMonth.now()
    val monthExp = remember(txns) { txns.inMonth(month).filter { it.isExpense } }
    val spent = monthExp.sumOf { it.amount }
    val total = budgets.firstOrNull { it.category == TOTAL_BUDGET }
    val catBudgets = budgets.filter { it.category != TOTAL_BUDGET }.sortedBy { it.category }
    val available = Categories.expense.filter { c -> catBudgets.none { it.category == c.name } }
    val today = LocalDate.now()
    val daysLeft = today.lengthOfMonth() - today.dayOfMonth + 1
    val history = remember(txns) { (0..5).map { i -> month.minusMonths(i.toLong()) to txns.inMonth(month.minusMonths(i.toLong())).expenseTotal() } }

    var dialog by remember { mutableStateOf<BudgetDialogState?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Budget", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(Dates.month(month), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Panel {
                Text("Monthly budget", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                if (total == null) {
                    Text(
                        "Set how much you want to spend this month. You'll see what's left and a daily allowance.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        dialog = BudgetDialogState("Monthly budget", TOTAL_BUDGET, null, pickCategory = false, removable = false)
                    }) { Text("Set monthly budget") }
                } else {
                    val left = total.monthlyLimit - spent
                    val frac = (spent / total.monthlyLimit).toFloat()
                    Text(Money.fmt(left.coerceAtLeast(0.0), currency), fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        color = if (left >= 0) MaterialTheme.colorScheme.primary else ExpenseRed)
                    Text(
                        if (left >= 0) "left of ${Money.fmt(total.monthlyLimit, currency)}"
                        else "Over budget by ${Money.fmt(-left, currency)}",
                        color = if (left >= 0) MaterialTheme.colorScheme.onSurfaceVariant else ExpenseRed
                    )
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { frac.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = if (frac > 1f) ExpenseRed else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("Spent ${Money.fmt(spent, currency)}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        if (left > 0) {
                            Text(
                                "${Money.fmt(left / daysLeft, currency)}/day for $daysLeft days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        dialog = BudgetDialogState("Monthly budget", TOTAL_BUDGET, total.monthlyLimit, pickCategory = false, removable = true)
                    }) { Text("Change budget") }
                }
            }
        }

        item {
            Panel {
                Text("Category limits", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                if (catBudgets.isEmpty()) {
                    Text(
                        "Limit specific categories, like Food or Shopping.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                catBudgets.forEach { b ->
                    val cat = Categories.find(b.category)
                    val s = monthExp.filter { it.category == b.category }.sumOf { it.amount }
                    val frac = (s / b.monthlyLimit).toFloat()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                dialog = BudgetDialogState("${b.category} limit", b.category, b.monthlyLimit, pickCategory = false, removable = true)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIcon(cat, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(b.category, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                Text(
                                    "${Money.fmt(s, currency)} / ${Money.fmt(b.monthlyLimit, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (frac > 1f) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { frac.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = if (frac > 1f) ExpenseRed else cat.color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        dialog = BudgetDialogState("Add category limit", null, null, pickCategory = true, removable = false)
                    },
                    enabled = available.isNotEmpty()
                ) { Text("Add category limit") }
            }
        }

        item {
            Panel {
                Text("Past months", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                history.forEach { (m, s) ->
                    val over = total != null && s > total.monthlyLimit
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(Dates.month(m), Modifier.weight(1f))
                        Text(
                            Money.fmt(s, currency),
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                total == null -> MaterialTheme.colorScheme.onSurface
                                over -> ExpenseRed
                                else -> IncomeGreen
                            }
                        )
                    }
                }
            }
        }
    }

    dialog?.let { d ->
        val removeAction: ((String) -> Unit)? =
            if (d.removable) { { c -> onRemove(c); dialog = null } } else null
        BudgetEditDialog(
            state = d,
            options = if (d.pickCategory) available else emptyList(),
            currency = currency,
            onDismiss = { dialog = null },
            onSave = { c, a -> onSet(c, a); dialog = null },
            onRemove = removeAction
        )
    }
}

@Composable
private fun BudgetEditDialog(
    state: BudgetDialogState,
    options: List<Category>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit,
    onRemove: ((String) -> Unit)?
) {
    var picked by remember { mutableStateOf(state.category ?: options.firstOrNull()?.name ?: "Others") }
    var text by remember { mutableStateOf(state.initial?.let { Money.plain(it) } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.pickCategory) {
                    options.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { c ->
                                FilterChip(
                                    selected = picked == c.name,
                                    onClick = { picked = c.name },
                                    label = { Text("${c.emoji} ${c.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' } },
                    label = { Text("Monthly limit") },
                    prefix = { Text(currency) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                text.toDoubleOrNull()?.takeIf { it > 0 }?.let { onSave(picked, it) }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onRemove != null) {
                    TextButton(onClick = { onRemove(picked) }) { Text("Remove", color = ExpenseRed) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
