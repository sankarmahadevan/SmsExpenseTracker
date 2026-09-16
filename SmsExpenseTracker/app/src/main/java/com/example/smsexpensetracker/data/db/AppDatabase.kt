package com.example.smsexpensetracker.data.db

import android.content.Context
import androidx.room.*
import com.example.smsexpensetracker.data.model.*

class Converters {
    @TypeConverter fun fromTransactionType(v: TransactionType) = v.name
    @TypeConverter fun toTransactionType(v: String) = TransactionType.valueOf(v)

    @TypeConverter fun fromTransactionCategory(v: TransactionCategory) = v.name
    @TypeConverter fun toTransactionCategory(v: String) = TransactionCategory.valueOf(v)

    @TypeConverter fun fromAccountType(v: AccountType) = v.name
    @TypeConverter fun toAccountType(v: String) = AccountType.valueOf(v)
}

@Database(
    entities  = [Transaction::class, Setting::class],
    version   = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
