package dev.gkcoding.splitsnap.domain

data class ItemShare(val itemName: String, val cents: Long, val sharedWith: Int)

data class PersonShare(
    val person: Person,
    val items: List<ItemShare>,
    val itemsCents: Long,
    val taxCents: Long,
    val tipCents: Long,
) {
    val totalCents: Long get() = itemsCents + taxCents + tipCents
}

data class SplitResult(
    val shares: List<PersonShare>,
    val unassignedCents: Long,
)

/**
 * Splits a bill down to the cent. Shared items are divided evenly, tax and tip follow each
 * person's share of the assigned subtotal, and leftover cents go to the largest remainders so
 * the shares always add up exactly to what was assigned.
 */
object SplitCalculator {
    fun split(bill: Bill): SplitResult {
        val people = bill.people
        val itemShares = people.associate { it.key to mutableListOf<ItemShare>() }

        bill.items.forEach { item ->
            val assignees = people.filter { it.key in item.assignees }
            if (assignees.isEmpty()) return@forEach

            val parts = distribute(item.priceCents, List(assignees.size) { 1L })
            assignees.forEachIndexed { index, person ->
                itemShares.getValue(person.key) += ItemShare(item.name, parts[index], assignees.size)
            }
        }

        val subtotals = people.map { person -> itemShares.getValue(person.key).sumOf { it.cents } }
        val assignedSubtotal = subtotals.sum()
        val assignedRatio = if (bill.subtotalCents == 0L) 0.0 else assignedSubtotal.toDouble() / bill.subtotalCents

        // Only the assigned part of tax and tip is handed out; the rest stays with unassigned items.
        val taxToSplit = Math.round(bill.taxCents * assignedRatio)
        val tipToSplit = Math.round(bill.tipCents * assignedRatio)
        val weights = subtotals.map { it.coerceAtLeast(0) }
        val taxShares = distribute(taxToSplit, weights)
        val tipShares = distribute(tipToSplit, weights)

        val shares = people.mapIndexed { index, person ->
            PersonShare(
                person = person,
                items = itemShares.getValue(person.key),
                itemsCents = subtotals[index],
                taxCents = taxShares[index],
                tipCents = tipShares[index],
            )
        }

        return SplitResult(
            shares = shares,
            unassignedCents = bill.totalCents - shares.sumOf { it.totalCents },
        )
    }

    /** Divides [amount] proportionally to [weights] using the largest-remainder method. */
    fun distribute(amount: Long, weights: List<Long>): List<Long> {
        val totalWeight = weights.sum()
        if (weights.isEmpty()) return emptyList()
        if (totalWeight == 0L) return List(weights.size) { 0L }

        val sign = if (amount < 0) -1 else 1
        val absolute = amount * sign
        val base = weights.map { absolute * it / totalWeight }
        val remainders = weights.mapIndexed { index, weight -> index to (absolute * weight % totalWeight) }
        val leftover = (absolute - base.sum()).toInt()
        val bonus = remainders.sortedWith(compareByDescending<Pair<Int, Long>> { it.second }.thenBy { it.first })
            .take(leftover)
            .map { it.first }
            .toSet()

        return base.mapIndexed { index, value -> (value + if (index in bonus) 1 else 0) * sign }
    }
}
