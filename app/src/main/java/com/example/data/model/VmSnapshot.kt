package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vm_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = VirtualMachine::class,
            parentColumns = ["id"],
            childColumns = ["vmId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vmId")]
)
data class VmSnapshot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vmId: Long,
    val title: String,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L,
    val screenshotTag: String = ""
)
