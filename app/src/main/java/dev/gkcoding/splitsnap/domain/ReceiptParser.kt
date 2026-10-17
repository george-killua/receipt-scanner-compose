package dev.gkcoding.splitsnap.domain

import kotlin.math.abs

data class ParsedItem(val name: String, val priceCents: Long)

data class ParsedReceipt(
    val merchant: String,
    val date: String,
    val currencySymbol: String?,
    val items: List<ParsedItem>,
    /** Tax that is added on top of the item prices; 0 when prices already include it. */
    val taxCents: Long,
    val tipCents: Long,
    val totalCents: Long?,
)

/** Reads line items, tax, tip and the total from receipt text with one visual row per line. */
object ReceiptParser {
    private val datePattern = Regex(
        """\b(?:\d{1,2}[./-]\d{1,2}[./-]\d{2,4}|\d{4}[./-]\d{1,2}[./-]\d{1,2})\b""",
    )
    private val timePattern = Regex("""\b\d{1,2}:\d{2}\b""")
    private val quantityPrefix = Regex("""^(\d{1,2})\s*[xX×*]\s*|^(\d{1,2})\s+(?=\p{L})""")
    private val trailingNoise = Regex("""[\s\-.:*]+$|\s+\d+(?:[.,]\d+)?\s*[xX×@]\s*$""")

    private val subtotalWords = listOf("subtotal", "sub total", "sub-total", "zwischensumme", "net total", "netto")
    private val taxWords = listOf("tax", "vat", "mwst", "gst", "hst", "ust", "iva", "tva")
    private val tipWords = listOf("tip", "gratuity", "service charge", "service", "trinkgeld")
    private val totalWords = listOf("total", "summe", "amount due", "balance due", "to pay", "gesamt", "zu zahlen", "grand")
    private val ignoredWords = listOf(
        "cash", "change", "card", "visa", "mastercard", "amex", "debit", "credit", "tendered", "paid",
        "rounding", "balance", "auth", "terminal", "receipt", "invoice",
        "tel", "phone", "www", "thank", "danke", "kasse", "netto", "brutto", "rückgeld", "gegeben",
    )
    private val merchantIgnoredWords = ignoredWords + taxWords + totalWords + listOf("date", "time", "order", "check")

    fun parse(text: String): ParsedReceipt {
        val rows = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        val items = mutableListOf<ParsedItem>()
        var subtotal: Long? = null
        var tax: Long? = null
        var tip: Long? = null
        var total: Long? = null
        var reachedTotals = false

        rows.forEach { row ->
            val lower = row.lowercase()
            val amount = Money.findAll(row).lastOrNull()

            when {
                amount == null -> Unit
                subtotalWords.any { lower.contains(it) } -> { subtotal = amount; reachedTotals = true }
                taxWords.any { lower.containsWord(it) } -> { tax = (tax ?: 0) + amount; reachedTotals = true }
                tipWords.any { lower.containsWord(it) } -> { tip = (tip ?: 0) + amount; reachedTotals = true }
                totalWords.any { lower.contains(it) } -> { if (total == null) total = amount; reachedTotals = true }
                reachedTotals -> Unit
                else -> itemFrom(row)?.let(items::add)
            }
        }

        val itemsSum = items.sumOf { it.priceCents }
        val addedTax = when {
            tax == null -> 0L
            total != null && abs(itemsSum + (tip ?: 0) - total!!) <= 1 -> 0L // Tax already included in prices.
            subtotal != null && total != null && abs(subtotal!! - total!!) <= 1 -> 0L
            else -> tax!!
        }

        return ParsedReceipt(
            merchant = findMerchant(rows),
            date = rows.firstNotNullOfOrNull { datePattern.find(it)?.value }.orEmpty(),
            currencySymbol = Money.detectSymbol(text),
            items = items,
            taxCents = addedTax,
            tipCents = tip ?: 0,
            totalCents = total,
        )
    }

    private fun itemFrom(row: String): ParsedItem? {
        val lower = row.lowercase()
        if (ignoredWords.any { lower.containsWord(it) }) return null
        if (datePattern.containsMatchIn(row) || timePattern.containsMatchIn(row)) return null

        val amounts = Money.findAll(row)
        val price = amounts.lastOrNull() ?: return null

        // The name is whatever sits left of the first money value.
        val firstMoney = Regex("""-?[$€£]?\s?-?\d+(?:[.,]\d{3})*[.,]\d{2}""").find(row) ?: return null
        var name = row.substring(0, firstMoney.range.first).replace(trailingNoise, "").trim()

        val quantity = quantityPrefix.find(name)
        if (quantity != null) {
            val count = (quantity.groupValues[1].ifEmpty { quantity.groupValues[2] }).toIntOrNull() ?: 1
            name = name.substring(quantity.range.last + 1).trim()
            if (count > 1) name = "$count× $name"
        }

        name = name.trim(' ', '.', '-', '*', ':')
        if (name.count(Char::isLetter) < 2) return null

        return ParsedItem(name, price)
    }

    private fun findMerchant(rows: List<String>): String =
        rows.take(8).firstOrNull { row ->
            val lower = row.lowercase()
            row.length in 3..40 &&
                row.count(Char::isLetter) >= 3 &&
                !datePattern.containsMatchIn(row) &&
                Money.findAll(row).isEmpty() &&
                merchantIgnoredWords.none { lower.containsWord(it) }
        }.orEmpty()

    private fun String.containsWord(word: String): Boolean =
        Regex("""(?<!\p{L})${Regex.escape(word)}(?!\p{L})""").containsMatchIn(this)
}
