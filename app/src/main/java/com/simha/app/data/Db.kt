package com.simha.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dose: String,
    val times: String,
    val stock: Int?,
    val active: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "dose_log")
data class DoseLogEntity(
    @PrimaryKey val id: String,
    val medicineId: String?,
    val scheduledAt: Long,
    val status: String,
    val createdAt: Long,
)

@Entity(tableName = "diary")
data class DiaryEntity(
    @PrimaryKey val day: String,
    val data: String,
    val summary: String,
    val updatedAt: Long,
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val text: String,
    val createdAt: Long,
)

@Dao
interface GuardianDao {
    @Query("SELECT * FROM medicines ORDER BY name")
    fun medicines(): Flow<List<MedicineEntity>>

    @Upsert
    suspend fun upsertMedicine(m: MedicineEntity)

    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun deleteMedicine(id: String)

    @Insert
    suspend fun insertDose(d: DoseLogEntity)

    @Query("SELECT * FROM dose_log WHERE scheduledAt >= :since ORDER BY scheduledAt DESC")
    fun doses(since: Long): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM diary WHERE day = :day")
    suspend fun diary(day: String): DiaryEntity?

    @Upsert
    suspend fun upsertDiary(d: DiaryEntity)

    @Query("SELECT * FROM diary WHERE day >= :fromDay ORDER BY day DESC")
    suspend fun diarySince(fromDay: String): List<DiaryEntity>

    @Query("SELECT text FROM memories")
    suspend fun memoryTexts(): List<String>

    @Insert
    suspend fun insertMemory(m: MemoryEntity)

    @Query("SELECT * FROM memories ORDER BY createdAt DESC LIMIT :limit")
    suspend fun memories(limit: Int): List<MemoryEntity>
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `medicines` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `dose` TEXT NOT NULL, `times` TEXT NOT NULL, `stock` INTEGER, `active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `dose_log` (`id` TEXT NOT NULL, `medicineId` TEXT, `scheduledAt` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `diary` (`day` TEXT NOT NULL, `data` TEXT NOT NULL, `summary` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`day`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `memories` (`id` TEXT NOT NULL, `kind` TEXT NOT NULL, `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

@Database(
    entities = [
        ContactEntity::class, ProfileEntity::class, OutboxEntity::class,
        MedicineEntity::class, DoseLogEntity::class, DiaryEntity::class, MemoryEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class SimhaDb : RoomDatabase() {
    abstract fun dao(): SimhaDao
    abstract fun guardianDao(): GuardianDao
}
