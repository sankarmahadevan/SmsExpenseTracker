package com.example.smsexpensetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

// Explicitly import models instead of using wildcard *
import com.example.smsexpensetracker.data.model.Transaction
import com.example.smsexpensetracker.data.model.Setting
import com.example.smsexpensetracker.data.model.TransactionType
import com.example.smsexpensetracker.data.model.TransactionCategory
import com.example.smsexpensetracker.data.model.AccountType

// Add explicit imports for DAOs if they are in another package (e.g., com.example.smsexpensetracker.data.db.dao)
// import com.example.smsexpensetracker.data.db.dao.TransactionDao
// import com.example.smsexpensetracker.data.db.dao.SettingsDao

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
