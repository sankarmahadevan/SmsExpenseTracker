package com.example.smsexpensetracker.data.repository

import com.example.smsexpensetracker.data.db.SettingsDao
import com.example.smsexpensetracker.data.db.TransactionDao
import com.example.smsexpensetracker.data.model.Setting
import com.example.smsexpensetracker.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val settingsDao: SettingsDao
) {

    // ── Transactions ──────────────────────────────────────────────────────────

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    fun getTransactionsForMonth(yearMonth: YearMonth): Flow<List<Transaction>> {
        val (start, end) = yearMonth.toEpochRange()
        return transactionDao.getTransactionsByMonth(start, end)
    }

    suspend fun getTransactionById(id: Long): Transaction? =
        transactionDao.getById(id)

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insert(transaction)

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.update(transaction)

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.delete(transaction)

    suspend fun isDuplicate(transaction: Transaction): Boolean {
        val windowStart = transaction.timestamp - 30_000L
        return transactionDao.countRecentDuplicates(
            amountPaise = transaction.amountPaise,
            category    = transaction.category.name,
            windowStart = windowStart
        ) > 0
    }

    // ── Balance Calculations ──────────────────────────────────────────────────

    suspend fun getSavingsBalance(): Long {
        val initial = getInitialBalancePaise()
        val credits = transactionDao.getTotalSavingsCredits()
        val debits  = transactionDao.getTotalSavingsDebits()
        return initial + credits - debits
    }

    suspend fun getCreditCardNetSpent(): Long {
        val cycleStart = getCycleStartEpoch()
        val spent    = transactionDao.getCreditCardSpentSince(cycleStart)
        val payments = transactionDao.getCreditCardPaymentsSince(cycleStart)
        return (spent - payments).coerceAtLeast(0L)
    }

    // ── Month Summary ─────────────────────────────────────────────────────────

    data class MonthSummary(
        val yearMonth: YearMonth,
        val totalDebitPaise: Long,
        val totalCreditPaise: Long
    )

    suspend fun getMonthSummary(yearMonth: YearMonth): MonthSummary {
        val (start, end) = yearMonth.toEpochRange()
        return MonthSummary(
            yearMonth        = yearMonth,
            totalDebitPaise  = transactionDao.getMonthlyTotalDebit(start, end),
            totalCreditPaise = transactionDao.getMonthlyTotalCredit(start, end)
        )
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    suspend fun getInitialBalancePaise(): Long =
        settingsDao.get(Setting.KEY_INITIAL_BALANCE)?.value?.toLongOrNull() ?: 0L

    suspend fun setInitialBalancePaise(paise: Long) =
        settingsDao.upsert(Setting(Setting.KEY_INITIAL_BALANCE, paise.toString()))

    fun observeInitialBalance(): Flow<Long> =
        settingsDao.observe(Setting.KEY_INITIAL_BALANCE)
            .map { it?.value?.toLongOrNull() ?: 0L }

    suspend fun getCreditCardResetDay(): Int =
        settingsDao.get(Setting.KEY_CC_RESET_DAY)?.value?.toIntOrNull() ?: 1

    suspend fun setCreditCardResetDay(day: Int) =
        settingsDao.upsert(Setting(Setting.KEY_CC_RESET_DAY, day.toString()))

    suspend fun isSetupDone(): Boolean =
        settingsDao.get(Setting.KEY_SETUP_DONE)?.value == "true"

    suspend fun markSetupDone() =
        settingsDao.upsert(Setting(Setting.KEY_SETUP_DONE, "true"))

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun getCycleStartEpoch(): Long {
        val resetDay  = getCreditCardResetDay().coerceIn(1, 28)
        val zone      = ZoneId.systemDefault()
        val today     = LocalDate.now(zone)
        val cycleStart = if (today.dayOfMonth >= resetDay)
            today.withDayOfMonth(resetDay)
        else
            today.minusMonths(1).withDayOfMonth(resetDay)
        return cycleStart.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}

fun YearMonth.toEpochRange(): Pair<Long, Long> {
    val zone  = ZoneId.systemDefault()
    val start = atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end   = atEndOfMonth().atTime(23, 59, 59, 999_000_000)
        .atZone(zone).toInstant().toEpochMilli()
    return start to end
}
