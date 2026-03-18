package com.mahavtaar.vibecoder.agent.memory

import com.mahavtaar.vibecoder.data.db.MemoryDao
import com.mahavtaar.vibecoder.data.db.MemoryEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentMemory @Inject constructor(
    private val memoryDao: MemoryDao
) {
    suspend fun set(key: String, value: String) {
        memoryDao.insert(MemoryEntity(key = key, value = value))
    }

    suspend fun get(key: String): String? {
        return memoryDao.get(key)?.value
    }

    suspend fun delete(key: String) {
        memoryDao.delete(key)
    }

    suspend fun listAll(): List<MemoryEntity> {
        return memoryDao.getAll()
    }
}
