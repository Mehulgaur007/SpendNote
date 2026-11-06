package com.spendnote.app.data

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One expense or income entry. `epochDay` = LocalDate.toEpochDay(). */
@Entity(tableName = "transactions")
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val isExpense: Boolean,
    val category: String,
    val note: String = "",
    val epochDay: Long,
    val createdAt: Long = System.currentTimeMillis()
)

/** Monthly spending limit. Category = TOTAL_BUDGET means the overall monthly budget. */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: String,
    val monthlyLimit: Double
)

const val TOTAL_BUDGET = "__TOTAL__"

data class Category(val name: String, val emoji: String, val color: Color)

object Categories {
    val expense = listOf(
        Category("Food", "🍔", Color(0xFFEF6C00)),
        Category("Groceries", "🛒", Color(0xFF43A047)),
        Category("Transport", "🚌", Color(0xFF1E88E5)),
        Category("Fuel", "⛽", Color(0xFF6D4C41)),
        Category("Shopping", "🛍️", Color(0xFFD81B60)),
        Category("Bills", "💡", Color(0xFFF9A825)),
        Category("Rent", "🏠", Color(0xFF5E35B1)),
        Category("Recharge", "📱", Color(0xFF00ACC1)),
        Category("Health", "💊", Color(0xFFE53935)),
        Category("Education", "📚", Color(0xFF3949AB)),
        Category("Entertainment", "🎬", Color(0xFF8E24AA)),
        Category("Travel", "✈️", Color(0xFF00897B)),
        Category("Gifts", "🎁", Color(0xFFF4511E)),
        Category("Others", "📦", Color(0xFF757575)),
    )
    val income = listOf(
        Category("Salary", "💼", Color(0xFF2E7D32)),
        Category("Business", "🏪", Color(0xFF1565C0)),
        Category("Gift Received", "🎉", Color(0xFFAD1457)),
        Category("Interest", "🏦", Color(0xFF00838F)),
        Category("Refund", "↩️", Color(0xFF6A1B9A)),
        Category("Other Income", "💰", Color(0xFF558B2F)),
    )

    fun find(name: String): Category =
        (expense + income).firstOrNull { it.name == name } ?: Category(name, "📦", Color(0xFF757575))
}
