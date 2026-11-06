package com.spendnote.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TxnDao {
    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<Txn>>

    @Upsert
    suspend fun upsert(txn: Txn)

    @Insert
    suspend fun insertAll(list: List<Txn>)

    @Delete
    suspend fun delete(txn: Txn)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<Budget>>

    @Upsert
    suspend fun upsert(budget: Budget)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun remove(category: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Database(entities = [Txn::class, Budget::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun txnDao(): TxnDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spendnote.db"
                ).build().also { INSTANCE = it }
            }
    }
}
