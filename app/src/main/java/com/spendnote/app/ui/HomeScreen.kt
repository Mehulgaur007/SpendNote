package com.spendnote.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendnote.app.AppSettings
import com.spendnote.app.data.Budget
import com.spendnote.app.data.TOTAL_BUDGET
import com.spendnote.app.data.Txn
import com.spendnote.app.ui.theme.BrandEnd
import com.spendnote.app.ui.theme.BrandStart
import com.spendnote.app.ui.theme.ExpenseRed
import com.spendnote.app.util.Dates
import com.spendnote.app.util.Money
import com.spendnote.app.util.expenseTotal
import com.spendnote.app.util.inMonth
import com.spendnote.app.util.incomeTotal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Composable
fun HomeScreen(
    txns: List<Txn>,
    budgets: List<Budget>,
    settings: AppSettings,
    onOpen: (Txn) -> Unit
) {
    val cur = settings.currency
    var month by remember { mutableStateOf(YearMonth.now()) }

    val balance = remember(txns, settings.openingBalance) {
        settings.openingBalance + txns.incomeTotal() - txns.expenseTotal()
    }
    val monthTx = remember(txns, month) { txns.inMonth(month) }
    val todaySpent = remember(txns) {
        val d = LocalDate.now().toEpochDay()
        txns.filter { it.isExpense && it.epochDay == d }.sumOf { it.amount }
    }
    val totalBudget = budgets.firstOrNull { it.category == TOTAL_BUDGET }?.monthlyLimit
    val grouped = remember(monthTx) {
        monthTx.groupBy { it.epochDay }.toList().sortedByDescending { it.first }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("SpendNote", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    Dates.full(LocalDate.now()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            BalanceCard(balance, monthTx.incomeTotal(), monthTx.expenseTotal(), todaySpent, cur, month)
        }
        if (totalBudget != null && month == YearMonth.now()) {
            item { BudgetStrip(monthTx.expenseTotal(), totalBudget, cur) }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    Dates.month(month),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.now()) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                }
            }
        }
        if (grouped.isEmpty()) {
            item { EmptyState() }
        }
        grouped.forEach { (day, list) ->
            item(key = "day_$day") {
                DayHeader(LocalDate.ofEpochDay(day), list.expenseTotal(), cur)
            }
            items(list, key = { it.id }) { t ->
                TxnRow(t, cur) { onOpen(t) }
            }
        }
    }
}

@Composable
private fun BalanceCard(
    balance: Double,
    income: Double,
    expense: Double,
    today: Double,
    cur: String,
    month: YearMonth
) {
    val monthName = month.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(BrandStart, BrandEnd)))
            .padding(20.dp)
    ) {
        Text("Current balance", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
        Text(
            Money.fmt(balance, cur),
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Spent today: ${Money.fmt(today, cur)}",
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Income in $monthName", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
                Text("+" + Money.fmt(income, cur), color = Color(0xFFBBF7D0), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("Spent in $monthName", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
                Text("-" + Money.fmt(expense, cur), color = Color(0xFFFECACA), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun BudgetStrip(spent: Double, limit: Double, cur: String) {
    val left = limit - spent
    val frac = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
    val today = LocalDate.now()
    val daysLeft = today.lengthOfMonth() - today.dayOfMonth + 1
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Monthly budget", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(
                if (left >= 0) "${Money.fmt(left, cur)} left" else "Over by ${Money.fmt(-left, cur)}",
                color = if (left >= 0) MaterialTheme.colorScheme.primary else ExpenseRed,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = if (left >= 0) MaterialTheme.colorScheme.primary else ExpenseRed,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        if (left > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "You can spend ${Money.fmt(left / daysLeft, cur)} a day for the next $daysLeft days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, spent: Double, cur: String) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp)) {
        Text(Dates.dayLabel(date), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        if (spent > 0) {
            Text(
                "Spent ${Money.fmt(spent, cur)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📝", fontSize = 44.sp)
        Spacer(Modifier.height(8.dp))
        Text("No entries this month", fontWeight = FontWeight.SemiBold)
        Text(
            "Tap Add entry to note your first expense.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
