package dev.gkcoding.splitsnap.domain

data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerY: Float get() = (top + bottom) / 2f
    val height: Int get() = bottom - top
}

/**
 * ML Kit groups text into column blocks, so an item name and its price usually end up in
 * different blocks. This rebuilds the visual rows of the receipt from line bounding boxes.
 */
object ReceiptRows {
    fun build(lines: List<OcrLine>): List<String> {
        if (lines.isEmpty()) return emptyList()

        val medianHeight = lines.map { it.height }.sorted()[lines.size / 2].coerceAtLeast(1)
        val tolerance = medianHeight * 0.6f
        val rows = mutableListOf<MutableList<OcrLine>>()

        lines.sortedBy { it.centerY }.forEach { line ->
            val row = rows.lastOrNull()
            if (row != null && kotlin.math.abs(row.map { it.centerY }.average() - line.centerY) <= tolerance) {
                row += line
            } else {
                rows += mutableListOf(line)
            }
        }

        return rows.map { row -> row.sortedBy { it.left }.joinToString("  ") { it.text.trim() } }
    }
}
