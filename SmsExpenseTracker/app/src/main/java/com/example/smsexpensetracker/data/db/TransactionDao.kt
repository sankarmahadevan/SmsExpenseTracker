package com.example.smsexpensetracker.data.db

import androidx.room.*
import com.example.smsexpensetracker.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions
        WHERE timestamp >= :epochStart AND timestamp <= :epochEnd
        ORDER BY timestamp DESC
    """)
    fun getTransactionsByMonth(epochStart: Long, epochEnd: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM transactions WHERE accountType = 'SAVINGS' AND type = 'CREDIT'")
    suspend fun getTotalSavingsCredits(): Long

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM transactions WHERE accountType = 'SAVINGS' AND type = 'DEBIT'")
    suspend fun getTotalSavingsDebits(): Long

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions
        WHERE category = 'CREDIT_CARD' AND accountType = 'CREDIT_CARD' AND timestamp >= :cycleStart
    """)
    suspend fun getCreditCardSpentSince(cycleStart: Long): Long

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions
        WHERE category = 'CREDIT_CARD_PAYMENT' AND timestamp >= :cycleStart
    """)
    suspend fun getCreditCardPaymentsSince(cycleStart: Long): Long

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions
        WHERE type = 'DEBIT' AND accountType = 'SAVINGS'
          AND timestamp >= :epochStart AND timestamp <= :epochEnd
    """)
    suspend fun getMonthlyTotalDebit(epochStart: Long, epochEnd: Long): Long

    @Query("""
        SELECT COALESCE(SUM(amountPaise), 0) FROM transactions
        WHERE type = 'CREDIT' AND accountType = 'SAVINGS'
          AND timestamp >= :epochStart AND timestamp <= :epochEnd
    """)
    suspend fun getMonthlyTotalCredit(epochStart: Long, epochEnd: Long): Long

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE amountPaise = :amountPaise AND category = :category
          AND timestamp >= :windowStart AND isManual = 0
    """)
    suspend fun countRecentDuplicates(amountPaise: Long, category: String, windowStart: Long): Int
}
