@file:OptIn(ExperimentalMaterial3Api::class)

package com.spendnote.app.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spendnote.app.data.Categories
import com.spendnote.app.data.Txn
import com.spendnote.app.ui.theme.ExpenseRed
import com.spendnote.app.ui.theme.IncomeGreen
import com.spendnote.app.util.Dates
import com.spendnote.app.util.Money
import com.spendnote.app.util.expenseTotal
import com.spendnote.app.util.inRange
import com.spendnote.app.util.incomeTotal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

enum class Period(val label: String) { Week("Week"), Month("Month"), Year("Year") }

@Composable
fun InsightsScreen(txns: List<Txn>, currency: String) {
    var period by remember { mutableStateOf(Period.Month) }
    var anchor by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()

    val start: LocalDate = when (period) {
        Period.Week -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        Period.Month -> anchor.withDayOfMonth(1)
        Period.Year -> anchor.withDayOfYear(1)
    }
    val end: LocalDate = when (period) {
        Period.Week -> start.plusDays(6)
        Period.Month -> start.plusMonths(1).minusDays(1)
        Period.Year -> start.plusYears(1).minusDays(1)
    }
    val title = when (period) {
        Period.Week -> "${Dates.short(start)} – ${Dates.short(end)}"
        Period.Month -> Dates.month(YearMonth.from(start))
        Period.Year -> start.year.toString()
    }

    val inRange = remember(txns, start, end) { txns.inRange(start, end) }
    val spent = inRange.expenseTotal()
    val earned = inRange.incomeTotal()
    val byCat = remember(inRange) {
        inRange.filter { it.isExpense }
            .groupBy { it.category }
            .map { (k, v) -> k to v.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }
    val bars = remember(inRange, period, start) { buildBars(period, start, inRange) }

    val countedDays = if (start > today) 0L else ChronoUnit.DAYS.between(start, if (end < today) end else today) + 1
    val avgPerDay = if (countedDays > 0) spent / countedDays else 0.0
    val highlight = when (period) {
        Period.Week -> ChronoUnit.DAYS.between(start, today).toInt().takeIf { it in 0..6 } ?: -1
        Period.Month -> if (YearMonth.from(start) == YearMonth.from(today)) today.dayOfMonth - 1 else -1
        Period.Year -> if (start.year == today.year) today.monthValue - 1 else -1
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Period.entries.forEach { p ->
                    FilterChip(
                        selected = period == p,
                        onClick = { period = p; anchor = LocalDate.now() },
                        label = { Text(p.label) }
                    )
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { anchor = shift(period, anchor, -1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous")
                }
                Text(
                    title,
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { anchor = shift(period, anchor, 1) }, enabled = end < today) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Spent", Money.fmt(spent, currency), ExpenseRed, Modifier.weight(1f))
                StatTile("Earned", Money.fmt(earned, currency), IncomeGreen, Modifier.weight(1f))
                StatTile(
                    "Net",
                    Money.fmt(earned - spent, currency),
                    if (earned - spent >= 0) IncomeGreen else ExpenseRed,
                    Modifier.weight(1f)
                )
            }
        }
        item {
            Panel {
                Row {
                    Text("Spending trend", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(
                        "Avg ${Money.fmt(avgPerDay, currency)}/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                BarChart(bars.first, bars.second, MaterialTheme.colorScheme.primary, highlightIndex = highlight)
            }
        }
        item {
            Panel {
                Text("Where your money went", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                if (byCat.isEmpty()) {
                    Text(
                        "No expenses in this period.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    DonutChart(
                        slices = byCat.map { Categories.find(it.first).color to it.second },
                        modifier = Modifier.size(190.dp).align(Alignment.CenterHorizontally)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.fmt(spent, currency), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    byCat.forEach { (name, value) ->
                        val cat = Categories.find(name)
                        val frac = if (spent > 0) (value / spent).toFloat() else 0f
                        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            CategoryIcon(cat, 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Text(name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                    Text(Money.fmt(value, currency), fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = { frac },
                                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = cat.color,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${(frac * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shift(p: Period, d: LocalDate, n: Long): LocalDate = when (p) {
    Period.Week -> d.plusWeeks(n)
    Period.Month -> d.plusMonths(n)
    Period.Year -> d.plusYears(n)
}

private fun buildBars(period: Period, start: LocalDate, txns: List<Txn>): Pair<List<Double>, List<String>> {
    val exp = txns.filter { it.isExpense }
    return when (period) {
        Period.Week -> {
            val days = (0..6).map { start.plusDays(it.toLong()) }
            val values = days.map { d -> exp.filter { it.epochDay == d.toEpochDay() }.sumOf { it.amount } }
            val labels = days.map { it.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()).take(3) }
            values to labels
        }
        Period.Month -> {
            val n = start.lengthOfMonth()
            val byDay = exp.groupBy { LocalDate.ofEpochDay(it.epochDay).dayOfMonth }
            val values = (1..n).map { d -> byDay[d]?.sumOf { it.amount } ?: 0.0 }
            val labels = (1..n).map { d -> if (d == 1 || d % 5 == 0) d.toString() else "" }
            values to labels
        }
        Period.Year -> {
            val byMonth = exp.groupBy { LocalDate.ofEpochDay(it.epochDay).monthValue }
            val values = (1..12).map { m -> byMonth[m]?.sumOf { it.amount } ?: 0.0 }
            val labels = (1..12).map { m ->
                java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault())
            }
            values to labels
        }
    }
}
