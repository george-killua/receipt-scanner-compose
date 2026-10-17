package dev.gkcoding.splitsnap.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val date: String,
    val currencySymbol: String,
    val taxCents: Long,
    val tipCents: Long,
    val receiptTotalCents: Long?,
    val originalFilePath: String,
    val mimeType: String,
    val ocrText: String,
    val createdAt: Long,
)

@Entity(
    tableName = "people",
    foreignKeys = [ForeignKey(BillEntity::class, ["id"], ["billId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("billId")],
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val name: String,
    val colorIndex: Int,
    val paid: Boolean,
    val position: Int,
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(BillEntity::class, ["id"], ["billId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("billId")],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val name: String,
    val priceCents: Long,
    val position: Int,
)

@Entity(
    tableName = "assignments",
    primaryKeys = ["itemId", "personId"],
    foreignKeys = [
        ForeignKey(ItemEntity::class, ["id"], ["itemId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PersonEntity::class, ["id"], ["personId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("personId")],
)
data class AssignmentEntity(
    val itemId: Long,
    val personId: Long,
)

data class ItemWithAssignments(
    @Embedded val item: ItemEntity,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val assignments: List<AssignmentEntity>,
)

data class BillWithDetails(
    @Embedded val bill: BillEntity,
    @Relation(parentColumn = "id", entityColumn = "billId")
    val people: List<PersonEntity>,
    @Relation(entity = ItemEntity::class, parentColumn = "id", entityColumn = "billId")
    val items: List<ItemWithAssignments>,
)
