package dev.gkcoding.splitsnap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Transaction
    @Query("SELECT * FROM bills ORDER BY createdAt DESC")
    fun observeBills(): Flow<List<BillWithDetails>>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBill(id: Long): BillWithDetails?

    @Query("SELECT name FROM people WHERE name != :exclude GROUP BY name COLLATE NOCASE ORDER BY MAX(id) DESC LIMIT 12")
    fun observeRecentNames(exclude: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBill(bill: BillEntity): Long

    @Insert
    suspend fun insertPerson(person: PersonEntity): Long

    @Insert
    suspend fun insertItem(item: ItemEntity): Long

    @Insert
    suspend fun insertAssignments(assignments: List<AssignmentEntity>)

    @Query("DELETE FROM people WHERE billId = :billId")
    suspend fun deletePeople(billId: Long)

    @Query("DELETE FROM items WHERE billId = :billId")
    suspend fun deleteItems(billId: Long)

    @Query("UPDATE people SET paid = :paid WHERE id = :personId")
    suspend fun setPaid(personId: Long, paid: Boolean)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBill(id: Long)
}
