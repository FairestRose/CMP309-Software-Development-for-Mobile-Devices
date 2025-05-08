package com.example.medimap.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.medimap.database.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications")
    fun getAll(): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(medication: Medication): Long

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Int): Medication?


    @Query("UPDATE medications SET lastTaken = :timestamp WHERE id = :medicationId")
    suspend fun updateLastTaken(medicationId: Int, timestamp: Long)
}