package dev.gkcoding.splitsnap.domain

import kotlin.math.abs

object Money {
    private val amountPattern = Regex("""-?[$€£]?\s?-?(?:\d{1,3}(?:[.,]\d{3})+|\d+)[.,]\d{2}(?![\d.,])-?""")

    /** All money-looking values in [text], in order of appearance. */
    fun findAll(text: String): List<Long> =
        amountPattern.findAll(text).mapNotNull { parseCents(it.value) }.toList()

    /** Parses user or OCR input such as "12,50", "€1.299,00" or "-3.20" into cents. */
    fun parseCents(raw: String): Long? {
        val trimmed = raw.trim()
        val firstDigit = trimmed.indexOfFirst(Char::isDigit)
        val negative = trimmed.endsWith("-") || (firstDigit > 0 && '-' in trimmed.substring(0, firstDigit))
        val cleaned = raw.replace(Regex("""[^0-9,.]"""), "")
        if (cleaned.none(Char::isDigit)) return null

        val separatorIndex = maxOf(cleaned.lastIndexOf(','), cleaned.lastIndexOf('.'))
        val hasDecimals = separatorIndex >= 0 && cleaned.length - separatorIndex - 1 in 1..2
        val whole = if (hasDecimals) cleaned.substring(0, separatorIndex) else cleaned
        val fraction = if (hasDecimals) cleaned.substring(separatorIndex + 1).padEnd(2, '0') else "00"

        val units = whole.filter(Char::isDigit).ifEmpty { "0" }.toLongOrNull() ?: return null
        val cents = units * 100 + fraction.toLong()
        return if (negative) -cents else cents
    }

    fun format(cents: Long, symbol: String): String {
        val sign = if (cents < 0) "-" else ""
        val value = abs(cents)
        return "$sign$symbol${value / 100}.${(value % 100).toString().padStart(2, '0')}"
    }

    /** Plain editable form, e.g. 1250 -> "12.50". */
    fun toInput(cents: Long): String = format(cents, "")

    fun detectSymbol(text: String): String? =
        listOf("€", "$", "£").maxByOrNull { symbol -> text.count { it.toString() == symbol } }
            ?.takeIf { symbol -> symbol in text }
            ?: when {
                Regex("""\bEUR\b""").containsMatchIn(text) -> "€"
                Regex("""\b(?:USD|CAD|AUD)\b""").containsMatchIn(text) -> "$"
                Regex("""\bGBP\b""").containsMatchIn(text) -> "£"
                else -> null
            }
}
