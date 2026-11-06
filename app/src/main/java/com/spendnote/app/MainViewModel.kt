package com.spendnote.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spendnote.app.data.AppDatabase
import com.spendnote.app.data.Budget
import com.spendnote.app.data.Txn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class AppSettings(
    val openingBalance: Double,
    val currency: String,
    val setupDone: Boolean
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val prefs = app.getSharedPreferences("spendnote_settings", Context.MODE_PRIVATE)

    val transactions: StateFlow<List<Txn>> =
        db.txnDao().observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val budgets: StateFlow<List<Budget>> =
        db.budgetDao().observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun readSettings() = AppSettings(
        openingBalance = prefs.getString("opening", "0")?.toDoubleOrNull() ?: 0.0,
        currency = prefs.getString("currency", "₹") ?: "₹",
        setupDone = prefs.getBoolean("setup_done", false)
    )

    fun setOpeningBalance(value: Double) {
        prefs.edit().putString("opening", value.toString()).putBoolean("setup_done", true).apply()
        _settings.value = readSettings()
    }

    fun setCurrency(symbol: String) {
        prefs.edit().putString("currency", symbol).apply()
        _settings.value = readSettings()
    }

    fun save(txn: Txn) {
        viewModelScope.launch { db.txnDao().upsert(txn) }
    }

    fun delete(txn: Txn) {
        viewModelScope.launch { db.txnDao().delete(txn) }
    }

    fun setBudget(category: String, limit: Double) {
        viewModelScope.launch { db.budgetDao().upsert(Budget(category, limit)) }
    }

    fun removeBudget(category: String) {
        viewModelScope.launch { db.budgetDao().remove(category) }
    }

    fun clearAll() {
        viewModelScope.launch {
            db.txnDao().deleteAll()
            db.budgetDao().deleteAll()
        }
        prefs.edit().clear().apply()
        _settings.value = readSettings()
    }

    // ---------- Backup (CSV) ----------

    fun exportCsv(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val out = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: error("Cannot open file")
                    out.bufferedWriter().use { w ->
                        w.write("Date,Type,Category,Amount,Note\n")
                        w.write("${LocalDate.now()},Opening,,${settings.value.openingBalance},Opening balance\n")
                        transactions.value.sortedBy { it.epochDay }.forEach { t ->
                            val type = if (t.isExpense) "Expense" else "Income"
                            w.write("${LocalDate.ofEpochDay(t.epochDay)},$type,${csv(t.category)},${t.amount},${csv(t.note)}\n")
                        }
                    }
                }.isSuccess
            }
            onDone(ok)
        }
    }

    /** Returns number of imported entries, or -1 on failure. Import adds to existing data. */
    fun importCsv(uri: Uri, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                runCatching {
                    val lines = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readLines() } ?: emptyList()
                    var opening: Double? = null
                    val list = mutableListOf<Txn>()
                    for (line in lines.drop(1)) {
                        if (line.isBlank()) continue
                        val c = parseCsvLine(line)
                        if (c.size < 4) continue
                        val type = c[1].trim()
                        val amount = c[3].trim().toDoubleOrNull() ?: continue
                        if (type.equals("Opening", ignoreCase = true)) {
                            opening = amount
                            continue
                        }
                        val day = runCatching { LocalDate.parse(c[0].trim()).toEpochDay() }.getOrNull() ?: continue
                        list += Txn(
                            amount = amount,
                            isExpense = !type.equals("Income", ignoreCase = true),
                            category = c[2].ifBlank { "Others" },
                            note = c.getOrElse(4) { "" },
                            epochDay = day
                        )
                    }
                    db.txnDao().insertAll(list)
                    opening?.let { setOpeningBalance(it) }
                    list.size
                }.getOrDefault(-1)
            }
            onDone(count)
        }
    }

    private fun csv(s: String) = "\"" + s.replace("\n", " ").replace("\"", "\"\"") + "\""

    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"'); i++
                    } else inQuotes = false
                } else sb.append(ch)
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> { out.add(sb.toString()); sb.clear() }
                    else -> sb.append(ch)
                }
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}
