package dev.gkcoding.splitsnap.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitCalculatorTest {
    private val anna = Person(1, "Anna", 0)
    private val ben = Person(2, "Ben", 1)
    private val me = Person(3, "Me", 2)

    @Test
    fun distributeKeepsEveryCent() {
        assertEquals(listOf(334L, 333L, 333L), SplitCalculator.distribute(1000, listOf(1, 1, 1)))
        assertEquals(listOf(-334L, -333L, -333L), SplitCalculator.distribute(-1000, listOf(1, 1, 1)))
        assertEquals(listOf(0L, 0L), SplitCalculator.distribute(500, listOf(0, 0)))
    }

    @Test
    fun sharedItemsAndTipAddUpToTheBill() {
        val bill = Bill(
            people = listOf(anna, ben, me),
            items = listOf(
                BillItem(10, "Margherita", 1200, setOf(1)),
                BillItem(11, "Carbonara", 1500, setOf(3)),
                BillItem(12, "Wine", 3200, setOf(1, 2, 3)),
                BillItem(13, "Tiramisu", 800, setOf(2)),
            ),
            taxCents = 0,
            tipCents = 670,
        )

        val result = SplitCalculator.split(bill)

        assertEquals(0L, result.unassignedCents)
        assertEquals(bill.totalCents, result.shares.sumOf { it.totalCents })
        val annaShare = result.shares.first { it.person == anna }
        assertEquals(1200L + 1067L, annaShare.itemsCents)
        assertEquals(listOf(1067L, 1067L, 1066L), result.shares.map { share -> share.items.first { it.itemName == "Wine" }.cents })
    }

    @Test
    fun taxFollowsEachPersonsSubtotal() {
        val bill = Bill(
            people = listOf(anna, ben),
            items = listOf(BillItem(10, "Steak", 3000, setOf(1)), BillItem(11, "Salad", 1000, setOf(2))),
            taxCents = 400,
        )

        val result = SplitCalculator.split(bill)

        assertEquals(listOf(300L, 100L), result.shares.map { it.taxCents })
    }

    @Test
    fun unassignedItemsKeepTheirShareOfTaxAndTip() {
        val bill = Bill(
            people = listOf(anna, ben),
            items = listOf(BillItem(10, "Burger", 1000, setOf(1)), BillItem(11, "Fries", 1000)),
            tipCents = 200,
        )

        val result = SplitCalculator.split(bill)

        assertEquals(1100L, result.shares.first().totalCents)
        assertEquals(0L, result.shares.last().totalCents)
        assertEquals(1100L, result.unassignedCents)
    }

    @Test
    fun removingAPersonClearsTheirAssignments() {
        val bill = Bill(people = listOf(anna, ben), items = listOf(BillItem(10, "Beer", 500, setOf(1, 2))))

        val updated = bill.withoutPerson(2)

        assertEquals(setOf(1L), updated.items.single().assignees)
        assertEquals(500L, SplitCalculator.split(updated).shares.single().totalCents)
    }

    @Test
    fun splitUnassignedEvenlyOnlyTouchesUnassignedItems() {
        val bill = Bill(
            people = listOf(anna, ben),
            items = listOf(BillItem(10, "Pasta", 1000, setOf(1)), BillItem(11, "Bread", 400)),
        ).splitUnassignedEvenly()

        assertEquals(setOf(1L), bill.items[0].assignees)
        assertEquals(setOf(1L, 2L), bill.items[1].assignees)
    }
}
