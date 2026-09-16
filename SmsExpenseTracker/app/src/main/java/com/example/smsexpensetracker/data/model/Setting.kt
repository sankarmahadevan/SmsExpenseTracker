package com.example.smsexpensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey val key: String,
    val value: String
) {
    companion object {
        const val KEY_INITIAL_BALANCE = "initial_balance_paise"
        const val KEY_CC_RESET_DAY   = "credit_card_reset_day" // day of month 1–28
        const val KEY_SETUP_DONE     = "setup_done"
    }
}
