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

@Entity(tableName = "daily_signal")
data class DailySignalEntity(
    @PrimaryKey val day: String,
    val t: String,
    val doses: String,
    val synced: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "weekly_signal")
data class WeeklySignalEntity(
    @PrimaryKey val week: String,
    val t: String,
)

@Entity(tableName = "facts")
data class FactEntity(
    @PrimaryKey val k: String,
    val v: String,
    val weight: Int,
    val updatedAt: Long,
)

@Dao
interface GuardianDao {
    @Query("SELECT * FROM medicines ORDER BY name")
    fun medicines(): Flow<List<MedicineEntity>>

    @Upsert
    suspend fun upsertMedicine(m: MedicineEntity)

    @Query("DELETE FROM medicines WHERE id = :id")
    suspend fun deleteMedicine(id: String)

    @Query("SELECT * FROM medicines")
    suspend fun medicinesNow(): List<MedicineEntity>

    @Query("SELECT * FROM daily_signal WHERE day = :day")
    suspend fun signal(day: String): DailySignalEntity?

    @Upsert
    suspend fun upsertSignal(e: DailySignalEntity)

    @Query("SELECT * FROM daily_signal WHERE day >= :from ORDER BY day DESC")
    suspend fun signalsSince(from: String): List<DailySignalEntity>

    @Query("SELECT * FROM daily_signal WHERE day >= :from ORDER BY day DESC")
    fun signalsFlow(from: String): Flow<List<DailySignalEntity>>

    @Query("SELECT * FROM daily_signal WHERE day >= :from AND day <= :to ORDER BY day")
    suspend fun signalsBetween(from: String, to: String): List<DailySignalEntity>

    @Query("SELECT * FROM daily_signal WHERE synced = 0")
    suspend fun unsynced(): List<DailySignalEntity>

    @Query("UPDATE daily_signal SET synced = 1 WHERE day = :day AND updatedAt = :at")
    suspend fun markSynced(day: String, at: Long)

    @Query("UPDATE daily_signal SET doses = '' WHERE day < :day AND doses != ''")
    suspend fun clearDosesBefore(day: String)

    @Query("DELETE FROM daily_signal WHERE day < :day")
    suspend fun deleteSignalsBefore(day: String): Int

    @Query("SELECT * FROM weekly_signal WHERE week = :week")
    suspend fun weekly(week: String): WeeklySignalEntity?

    @Upsert
    suspend fun upsertWeekly(e: WeeklySignalEntity)

    @Query("DELETE FROM weekly_signal WHERE week < :week")
    suspend fun deleteWeeklyBefore(week: String): Int

    @Query("SELECT * FROM facts ORDER BY weight DESC, updatedAt DESC")
    suspend fun facts(): List<FactEntity>

    @Upsert
    suspend fun upsertFact(f: FactEntity)

    @Query("DELETE FROM facts WHERE k = :k")
    suspend fun deleteFact(k: String)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `medicines` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `dose` TEXT NOT NULL, `times` TEXT NOT NULL, `stock` INTEGER, `active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `dose_log` (`id` TEXT NOT NULL, `medicineId` TEXT, `scheduledAt` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `diary` (`day` TEXT NOT NULL, `data` TEXT NOT NULL, `summary` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`day`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `memories` (`id` TEXT NOT NULL, `kind` TEXT NOT NULL, `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_signal` (`day` TEXT NOT NULL, `t` TEXT NOT NULL, `doses` TEXT NOT NULL, `synced` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`day`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `weekly_signal` (`week` TEXT NOT NULL, `t` TEXT NOT NULL, PRIMARY KEY(`week`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `facts` (`k` TEXT NOT NULL, `v` TEXT NOT NULL, `weight` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`k`))")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `g_node` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` INTEGER NOT NULL, `k` TEXT NOT NULL, `label` TEXT NOT NULL, `weight` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `ts` INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_g_node_type_k` ON `g_node` (`type`, `k`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `g_edge` (`src` INTEGER NOT NULL, `dst` INTEGER NOT NULL, `rel` TEXT NOT NULL, `strength` REAL NOT NULL, `ts` INTEGER NOT NULL, PRIMARY KEY(`src`, `dst`, `rel`))")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `agenda` (`id` TEXT NOT NULL, `kind` TEXT NOT NULL, `what` TEXT NOT NULL, `time` TEXT NOT NULL, `rule` TEXT NOT NULL, `date` TEXT NOT NULL, `nextAt` INTEGER NOT NULL, `active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

@Database(
    entities = [
        ContactEntity::class, ProfileEntity::class, OutboxEntity::class,
        MedicineEntity::class, DailySignalEntity::class, WeeklySignalEntity::class, FactEntity::class,
        com.simha.app.care.GNode::class, com.simha.app.care.GEdge::class,
        com.simha.app.agenda.AgendaItem::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class SimhaDb : RoomDatabase() {
    abstract fun dao(): SimhaDao
    abstract fun guardianDao(): GuardianDao
    abstract fun graphDao(): com.simha.app.care.GraphDao
    abstract fun agendaDao(): com.simha.app.agenda.AgendaDao
}
