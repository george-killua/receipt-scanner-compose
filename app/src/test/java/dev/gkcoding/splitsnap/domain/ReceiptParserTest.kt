package dev.gkcoding.splitsnap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptParserTest {
    @Test
    fun readsRestaurantReceiptWithAddedTaxAndTip() {
        val text = """
            TRATTORIA ROMA
            Via Roma 12, Berlin
            17/10/2026  20:14
            Table 4   Server: Luca
            Margherita          12.00
            2 x Carbonara       30.00
            Wine (btl)          32.00
            Tiramisu             8.00
            Subtotal            82.00
            Tax 5%               4.10
            Tip                  8.20
            TOTAL              $94.30
            Visa ****1234      $94.30
            Thank you!
        """.trimIndent()

        val receipt = ReceiptParser.parse(text)

        assertEquals("TRATTORIA ROMA", receipt.merchant)
        assertEquals("17/10/2026", receipt.date)
        assertEquals("$", receipt.currencySymbol)
        assertEquals(
            listOf(
                ParsedItem("Margherita", 1200),
                ParsedItem("2× Carbonara", 3000),
                ParsedItem("Wine (btl)", 3200),
                ParsedItem("Tiramisu", 800),
            ),
            receipt.items,
        )
        assertEquals(410L, receipt.taxCents)
        assertEquals(820L, receipt.tipCents)
        assertEquals(9430L, receipt.totalCents)
    }

    @Test
    fun ignoresIncludedVatOnEuropeanReceipts() {
        val text = """
            Café Kranzler
            Cappuccino      3,90 A
            Apfelstrudel    5,50 A
            Mineralwasser   2,80 A
            SUMME EUR      12,20
            MwSt 19%        1,95
            Bar            20,00
            Rückgeld        7,80
        """.trimIndent()

        val receipt = ReceiptParser.parse(text)

        assertEquals("Café Kranzler", receipt.merchant)
        assertEquals(listOf(390L, 550L, 280L), receipt.items.map { it.priceCents })
        assertEquals(0L, receipt.taxCents)
        assertEquals(1220L, receipt.totalCents)
        assertEquals("€", receipt.currencySymbol)
    }

    @Test
    fun keepsDiscountsAsNegativeItems() {
        val receipt = ReceiptParser.parse(
            """
            Burger Barn
            Cheeseburger   9.50
            Happy hour    -2.00
            Total          7.50
            """.trimIndent(),
        )

        assertEquals(listOf(ParsedItem("Cheeseburger", 950), ParsedItem("Happy hour", -200)), receipt.items)
    }

    @Test
    fun handlesTextWithoutItems() {
        val receipt = ReceiptParser.parse("blurry photo")

        assertEquals(emptyList<ParsedItem>(), receipt.items)
        assertNull(receipt.totalCents)
        assertNull(receipt.currencySymbol)
    }

    @Test
    fun parsesMoneyInput() {
        assertEquals(1250L, Money.parseCents("12,50"))
        assertEquals(129900L, Money.parseCents("€1.299,00"))
        assertEquals(1200L, Money.parseCents("12"))
        assertEquals(-320L, Money.parseCents("-3.20"))
        assertEquals(-320L, Money.parseCents("3.20-"))
        assertNull(Money.parseCents("abc"))
        assertEquals("€12.05", Money.format(1205, "€"))
        assertEquals("-$0.50", Money.format(-50, "$"))
    }
}
