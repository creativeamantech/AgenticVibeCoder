package com.mahavtaar.vibecoder.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity)

    @Query("SELECT * FROM memory WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): MemoryEntity?

    @Query("DELETE FROM memory WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM memory ORDER BY timestamp DESC")
    suspend fun getAll(): List<MemoryEntity>
}
