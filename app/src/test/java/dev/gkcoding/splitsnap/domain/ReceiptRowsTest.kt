package dev.gkcoding.splitsnap.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptRowsTest {
    @Test
    fun joinsNameAndPriceColumnsIntoRows() {
        // ML Kit returns the names column first, then the prices column.
        val lines = listOf(
            OcrLine("Margherita", 10, 100, 200, 130),
            OcrLine("Tiramisu", 10, 140, 180, 170),
            OcrLine("12.00", 400, 102, 470, 132),
            OcrLine("8.00", 410, 138, 470, 168),
        )

        assertEquals(listOf("Margherita  12.00", "Tiramisu  8.00"), ReceiptRows.build(lines))
    }
}
