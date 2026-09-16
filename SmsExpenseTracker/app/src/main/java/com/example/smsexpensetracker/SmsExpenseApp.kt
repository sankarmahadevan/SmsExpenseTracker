package com.example.smsexpensetracker

import android.app.Application
import com.example.smsexpensetracker.data.db.AppDatabase
import com.example.smsexpensetracker.data.repository.ExpenseRepository

class SmsExpenseApp : Application() {
    val database   by lazy { AppDatabase.getInstance(this) }
    val repository by lazy {
        ExpenseRepository(
            transactionDao = database.transactionDao(),
            settingsDao    = database.settingsDao()
        )
    }
}
