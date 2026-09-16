package com.example.smsexpensetracker.data.db

import androidx.room.*
import com.example.smsexpensetracker.data.model.Setting
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: Setting)

    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): Setting?

    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    fun observe(key: String): Flow<Setting?>

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun delete(key: String)
}
