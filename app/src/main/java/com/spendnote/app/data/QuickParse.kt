package com.spendnote.app.data

/**
 * Turns short text like "Tea 20", "petrol 300", "Salary 15000" into an entry.
 * Works fully offline using keyword matching (no AI / internet).
 */
data class ParsedEntry(
    val amount: Double?,
    val note: String,
    val category: String,
    val isExpense: Boolean
)

object QuickParse {
    private val numberRegex = Regex("""\d+(?:\.\d{1,2})?""")
    private val currencyWords = Regex("""(?i)(₹|\brs\.?|\binr\b|\brupees?\b)""")

    private val keywords: Map<String, List<String>> = linkedMapOf(
        "Food" to listOf("tea", "chai", "coffee", "lunch", "dinner", "breakfast", "snack", "snacks",
            "biryani", "hotel", "swiggy", "zomato", "food", "juice", "tiffin", "meals", "idli", "dosa"),
        "Groceries" to listOf("grocery", "groceries", "vegetable", "vegetables", "veggies", "milk",
            "rice", "kirana", "fruits", "dmart", "oil", "eggs"),
        "Transport" to listOf("bus", "auto", "uber", "ola", "rapido", "train", "metro", "taxi", "cab", "ticket"),
        "Fuel" to listOf("petrol", "diesel", "fuel", "cng"),
        "Shopping" to listOf("amazon", "flipkart", "meesho", "clothes", "shirt", "shoes", "dress", "shopping"),
        "Bills" to listOf("electricity", "current", "bill", "water", "wifi", "internet", "emi", "gas"),
        "Rent" to listOf("rent"),
        "Recharge" to listOf("recharge", "jio", "airtel", "vi", "bsnl", "dth"),
        "Health" to listOf("medicine", "medicines", "medical", "doctor", "hospital", "tablets", "pharmacy"),
        "Education" to listOf("book", "books", "fees", "fee", "course", "tuition", "school", "college"),
        "Entertainment" to listOf("movie", "cinema", "netflix", "hotstar", "prime", "game", "games"),
        "Travel" to listOf("trip", "travel", "flight", "tour"),
        "Gifts" to listOf("gift", "donation"),
        "Salary" to listOf("salary", "stipend", "wages"),
        "Business" to listOf("sale", "sales", "business", "profit"),
        "Interest" to listOf("interest"),
        "Refund" to listOf("refund", "cashback"),
    )

    private val incomeWords = setOf("received", "got", "salary", "income", "refund", "cashback",
        "credited", "earned", "stipend", "wages", "profit")

    fun parse(input: String): ParsedEntry {
        val cleaned = input.replace(",", "").trim()
        val match = numberRegex.find(cleaned)
        val amount = match?.value?.toDoubleOrNull()

        var note = if (match != null) cleaned.removeRange(match.range) else cleaned
        note = note.replace(currencyWords, " ").replace(Regex("\\s+"), " ").trim()

        val words = note.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()
        val category = keywords.entries.firstOrNull { (_, keys) -> keys.any { it in words } }?.key

        val incomeNames = Categories.income.map { it.name }
        val isIncome = incomeWords.any { it in words } || (category != null && category in incomeNames)

        val finalCategory = when {
            isIncome && category != null && category in incomeNames -> category
            isIncome && category == "Gifts" -> "Gift Received"
            isIncome -> "Other Income"
            category != null -> category
            else -> "Others"
        }
        return ParsedEntry(amount, note.replaceFirstChar { it.uppercase() }, finalCategory, !isIncome)
    }
}
