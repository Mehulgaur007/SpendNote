package com.spendnote.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendnote.app.data.Categories
import com.spendnote.app.data.Category
import com.spendnote.app.data.Txn
import com.spendnote.app.ui.theme.ExpenseRed
import com.spendnote.app.ui.theme.IncomeGreen
import com.spendnote.app.util.Money

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun CategoryIcon(category: Category, size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(category.color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(category.emoji, fontSize = (size.value * 0.45f).sp)
    }
}

@Composable
fun TxnRow(t: Txn, currency: String, onClick: () -> Unit) {
    val cat = Categories.find(t.category)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(cat)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (t.note.isNotBlank()) {
                Text(
                    t.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            (if (t.isExpense) "-" else "+") + Money.fmt(t.amount, currency),
            color = if (t.isExpense) ExpenseRed else IncomeGreen,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CategoryTile(category: Category, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(
                if (selected) category.color.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surface
            )
            .then(if (selected) Modifier.border(2.dp, category.color, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(category.emoji, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            category.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value != null) {
            Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Donut chart. Each slice = (color, value). */
@Composable
fun DonutChart(
    slices: List<Pair<Color, Double>>,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit
) {
    val total = slices.sumOf { it.second }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 26.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            if (total <= 0.0) {
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = strokeWidth))
            } else {
                var start = -90f
                slices.forEach { (color, value) ->
                    val sweep = (value / total * 360.0).toFloat()
                    val gap = if (slices.size > 1) 1.5f else 0f
                    drawArc(color, start, (sweep - gap).coerceAtLeast(0.5f), false, topLeft, arcSize, style = Stroke(width = strokeWidth))
                    start += sweep
                }
            }
        }
        center()
    }
}

/** Simple vertical bar chart. Empty labels are allowed (for crowded months). */
@Composable
fun BarChart(
    values: List<Double>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    highlightIndex: Int = -1
) {
    val max = (values.maxOrNull() ?: 0.0).takeIf { it > 0 } ?: 1.0
    Row(
        modifier.fillMaxWidth().height(150.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        values.forEachIndexed { i, v ->
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val frac = (v / max).toFloat().coerceAtLeast(0.02f)
                val barColor = when {
                    i == highlightIndex -> color
                    v > 0 -> color.copy(alpha = 0.55f)
                    else -> color.copy(alpha = 0.15f)
                }
                Box(
                    Modifier
                        .fillMaxWidth(0.75f)
                        .fillMaxHeight(0.82f * frac)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(barColor)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    labels.getOrElse(i) { "" },
                    fontSize = 9.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AmountDialog(
    title: String,
    initial: Double?,
    currency: String,
    allowNegative: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember { mutableStateOf(initial?.let { Money.plain(it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' || (allowNegative && it == '-') } },
                prefix = { Text(currency) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { text.toDoubleOrNull()?.let(onConfirm) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
