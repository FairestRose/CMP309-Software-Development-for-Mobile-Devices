package com.example.medimap.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val timing: String,
    val reminderTime: String? = null,
    val reminderDays: String? = null,
    val lastTaken: Long? = null
)