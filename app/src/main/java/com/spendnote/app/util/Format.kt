package com.spendnote.app.util

import com.spendnote.app.data.Txn
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object Money {
    // Indian grouping: 1,00,000
    private val nf: NumberFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    fun fmt(value: Double, symbol: String): String {
        val sign = if (value < 0) "-" else ""
        return sign + symbol + nf.format(abs(value))
    }

    /** Plain number for text fields: 20.0 -> "20", 20.5 -> "20.5" */
    fun plain(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString()
        else "%.2f".format(Locale.US, value).trimEnd('0').trimEnd('.')
}

object Dates {
    private val dayFmt = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    private val shortFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val fullFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    fun dayLabel(d: LocalDate): String {
        val today = LocalDate.now()
        return when (d) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> d.format(dayFmt)
        }
    }

    fun short(d: LocalDate): String = d.format(shortFmt)
    fun full(d: LocalDate): String = d.format(fullFmt)
    fun month(m: YearMonth): String = m.format(monthFmt)
}

fun List<Txn>.inRange(start: LocalDate, end: LocalDate): List<Txn> {
    val s = start.toEpochDay()
    val e = end.toEpochDay()
    return filter { it.epochDay in s..e }
}

fun List<Txn>.inMonth(m: YearMonth): List<Txn> = inRange(m.atDay(1), m.atEndOfMonth())
fun List<Txn>.expenseTotal(): Double = filter { it.isExpense }.sumOf { it.amount }
fun List<Txn>.incomeTotal(): Double = filter { !it.isExpense }.sumOf { it.amount }
