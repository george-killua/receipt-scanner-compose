package dev.gkcoding.splitsnap.domain

data class Person(
    val key: Long,
    val name: String,
    val colorIndex: Int,
    val paid: Boolean = false,
)

data class BillItem(
    val key: Long,
    val name: String,
    val priceCents: Long,
    val assignees: Set<Long> = emptySet(),
)

data class Bill(
    val id: Long = 0,
    val merchant: String = "",
    val date: String = "",
    val currencySymbol: String = "€",
    val items: List<BillItem> = emptyList(),
    val people: List<Person> = emptyList(),
    val taxCents: Long = 0,
    val tipCents: Long = 0,
    val receiptTotalCents: Long? = null,
    val originalFilePath: String = "",
    val mimeType: String = "",
    val ocrText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val subtotalCents: Long get() = items.sumOf { it.priceCents }
    val totalCents: Long get() = subtotalCents + taxCents + tipCents
    val unassignedItems: List<BillItem> get() = items.filter { item -> item.assignees.none { key -> people.any { it.key == key } } }
    val displayName: String get() = merchant.ifBlank { "Untitled bill" }

    fun nextKey(): Long = ((items.map { it.key } + people.map { it.key }).maxOrNull() ?: 0) + 1

    fun withPerson(name: String): Bill {
        val colorIndex = (0 until PERSON_COLOR_COUNT).firstOrNull { index -> people.none { it.colorIndex == index } }
            ?: (people.size % PERSON_COLOR_COUNT)
        return copy(people = people + Person(nextKey(), name.trim(), colorIndex))
    }

    fun withoutPerson(key: Long): Bill = copy(
        people = people.filterNot { it.key == key },
        items = items.map { it.copy(assignees = it.assignees - key) },
    )

    fun toggleAssignment(itemKey: Long, personKey: Long): Bill = copy(
        items = items.map { item ->
            if (item.key != itemKey) item
            else item.copy(assignees = if (personKey in item.assignees) item.assignees - personKey else item.assignees + personKey)
        },
    )

    /** Shares every unassigned item between everyone on the bill. */
    fun splitUnassignedEvenly(): Bill {
        val everyone = people.map { it.key }.toSet()
        val unassigned = unassignedItems.map { it.key }.toSet()
        return copy(items = items.map { if (it.key in unassigned) it.copy(assignees = everyone) else it })
    }

    companion object {
        const val PERSON_COLOR_COUNT = 8
    }
}
