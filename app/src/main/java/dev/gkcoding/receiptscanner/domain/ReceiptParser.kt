package dev.gkcoding.receiptscanner.domain

object ReceiptParser {
    private val datePattern = Regex(
        """\b(?:\d{1,2}[./-]\d{1,2}[./-]\d{2,4}|\d{4}[./-]\d{1,2}[./-]\d{1,2})\b""",
    )
    private val amountPattern = Regex("""(?:[$€£]\s*)?\d{1,3}(?:[.,]\d{3})*[.,]\d{2}|\d+[.,]\d{2}""")
    private val totalKeywords = listOf("total", "amount", "balance", "paid", "sum", "grand")
    private val ignoredMerchantWords = listOf("receipt", "invoice", "total", "amount", "tax", "vat", "date", "time")

    fun parse(text: String, originalFilePath: String, mimeType: String): ReceiptDraft {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return ReceiptDraft(
            merchant = findMerchant(lines),
            amount = findAmount(lines),
            receiptDate = lines.firstNotNullOfOrNull { datePattern.find(it)?.value }.orEmpty(),
            originalFilePath = originalFilePath,
            mimeType = mimeType,
            ocrText = text,
        )
    }

    private fun findMerchant(lines: List<String>): String =
        lines.firstOrNull { line ->
            val lower = line.lowercase()
            line.length in 3..48 &&
                line.any(Char::isLetter) &&
                datePattern.find(line) == null &&
                amountPattern.find(line) == null &&
                ignoredMerchantWords.none { lower.contains(it) }
        } ?: lines.firstOrNull().orEmpty()

    private fun findAmount(lines: List<String>): String {
        val keywordAmount = lines
            .filter { line -> totalKeywords.any { line.contains(it, ignoreCase = true) } }
            .flatMap(::amountCandidates)
            .maxByOrNull { it.value }

        return (keywordAmount ?: lines.flatMap(::amountCandidates).maxByOrNull { it.value })
            ?.raw
            .orEmpty()
    }

    private fun amountCandidates(line: String): List<AmountCandidate> =
        amountPattern.findAll(line)
            .mapNotNull { match ->
                val value = normalizeAmount(match.value) ?: return@mapNotNull null
                AmountCandidate(match.value.trim(), value)
            }
            .toList()

    private fun normalizeAmount(raw: String): Double? {
        val cleaned = raw.replace(Regex("""[^0-9,.]"""), "")
        if (cleaned.isBlank()) return null

        val decimalSeparator = when {
            cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.') -> ','
            else -> '.'
        }

        val normalized = buildString {
            cleaned.forEachIndexed { index, char ->
                when {
                    char.isDigit() -> append(char)
                    char == decimalSeparator && index == cleaned.lastIndexOf(decimalSeparator) -> append('.')
                }
            }
        }

        return normalized.toDoubleOrNull()
    }

    private data class AmountCandidate(val raw: String, val value: Double)
}
