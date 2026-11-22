package com.simha.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val role: String,
    val approved: Boolean,
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val conditions: String = "",
    val medicines: String = "",
    val allergies: String = "",
    val doctorName: String = "",
    val doctorPhone: String = "",
)

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val tbl: String,
    val payload: String,
    val createdAt: Long,
)

@Dao
interface SimhaDao {
    @Query("SELECT * FROM contacts ORDER BY name")
    fun contacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY name")
    suspend fun contactsNow(): List<ContactEntity>

    @Upsert
    suspend fun upsertContact(c: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContact(id: String)

    @Query("UPDATE contacts SET approved = :a WHERE role IN (:roles)")
    suspend fun setApproved(roles: List<String>, a: Boolean)

    @Query("SELECT * FROM profile WHERE id = 1")
    fun profile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun profileNow(): ProfileEntity?

    @Upsert
    suspend fun upsertProfile(p: ProfileEntity)

    @Insert
    suspend fun addOp(o: OutboxEntity)

    @Query("SELECT * FROM outbox ORDER BY id LIMIT 50")
    suspend fun ops(): List<OutboxEntity>

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun delOp(id: Long)
}

@Database(entities = [ContactEntity::class, ProfileEntity::class, OutboxEntity::class], version = 1, exportSchema = false)
abstract class SimhaDb : RoomDatabase() {
    abstract fun dao(): SimhaDao
}
