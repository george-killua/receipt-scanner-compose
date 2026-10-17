package dev.gkcoding.splitsnap.data

import androidx.room.withTransaction
import dev.gkcoding.splitsnap.domain.Bill
import dev.gkcoding.splitsnap.domain.BillItem
import dev.gkcoding.splitsnap.domain.Person
import kotlinx.coroutines.flow.map

class BillRepository(private val database: SplitSnapDatabase) {
    private val dao = database.billDao()

    val bills = dao.observeBills().map { rows -> rows.map { it.toBill() } }

    fun recentNames(exclude: String) = dao.observeRecentNames(exclude)

    suspend fun get(id: Long): Bill? = dao.getBill(id)?.toBill()

    /** Saves the whole bill, replacing its people, items and assignments. Returns the saved bill. */
    suspend fun save(bill: Bill): Bill = database.withTransaction {
        val billId = dao.upsertBill(
            BillEntity(
                id = bill.id,
                merchant = bill.merchant,
                date = bill.date,
                currencySymbol = bill.currencySymbol,
                taxCents = bill.taxCents,
                tipCents = bill.tipCents,
                receiptTotalCents = bill.receiptTotalCents,
                originalFilePath = bill.originalFilePath,
                mimeType = bill.mimeType,
                ocrText = bill.ocrText,
                createdAt = bill.createdAt,
            ),
        )
        dao.deletePeople(billId)
        dao.deleteItems(billId)

        val personIds = bill.people.mapIndexed { index, person ->
            person.key to dao.insertPerson(PersonEntity(0, billId, person.name, person.colorIndex, person.paid, index))
        }.toMap()

        val assignments = bill.items.flatMapIndexed { index, item ->
            val itemId = dao.insertItem(ItemEntity(0, billId, item.name, item.priceCents, index))
            item.assignees.mapNotNull { key -> personIds[key]?.let { AssignmentEntity(itemId, it) } }
        }
        dao.insertAssignments(assignments)

        requireNotNull(dao.getBill(billId)).toBill()
    }

    suspend fun setPaid(personKey: Long, paid: Boolean) {
        dao.setPaid(personKey, paid)
    }

    suspend fun delete(id: Long) {
        dao.deleteBill(id)
    }

    private fun BillWithDetails.toBill() = Bill(
        id = bill.id,
        merchant = bill.merchant,
        date = bill.date,
        currencySymbol = bill.currencySymbol,
        people = people.sortedBy { it.position }.map { Person(it.id, it.name, it.colorIndex, it.paid) },
        items = items.sortedBy { it.item.position }.map { row ->
            BillItem(row.item.id, row.item.name, row.item.priceCents, row.assignments.map { it.personId }.toSet())
        },
        taxCents = bill.taxCents,
        tipCents = bill.tipCents,
        receiptTotalCents = bill.receiptTotalCents,
        originalFilePath = bill.originalFilePath,
        mimeType = bill.mimeType,
        ocrText = bill.ocrText,
        createdAt = bill.createdAt,
    )
}
