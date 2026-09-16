package com.example.smsexpensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType { DEBIT, CREDIT }

enum class TransactionCategory {
    UPI,
    ATM,
    CREDIT_CARD,
    CREDIT_CARD_PAYMENT, // bill payment from savings → credit card
    OTHER
}

enum class AccountType { SAVINGS, CREDIT_CARD }

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Stored in paise (Long) to avoid floating-point errors. ₹1 = 100 paise. */
    val amountPaise: Long,

    val type: TransactionType,
    val category: TransactionCategory,
    val accountType: AccountType,

    val bankName: String = "",
    val description: String = "",

    /** Unix epoch milliseconds */
    val timestamp: Long = System.currentTimeMillis(),

    /** false = parsed from SMS, true = user entered manually */
    val isManual: Boolean = false,

    /** Raw SMS body for debugging/re-parsing */
    val rawSms: String = ""
)
