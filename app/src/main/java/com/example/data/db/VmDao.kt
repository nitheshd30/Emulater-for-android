package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VirtualMachine
import com.example.data.model.VmSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface VmDao {
    @Query("SELECT * FROM virtual_machines ORDER BY lastRunAt DESC, createdAt DESC")
    fun getAllVms(): Flow<List<VirtualMachine>>

    @Query("SELECT * FROM virtual_machines WHERE id = :id")
    fun getVmByIdFlow(id: Long): Flow<VirtualMachine?>

    @Query("SELECT * FROM virtual_machines WHERE id = :id")
    suspend fun getVmById(id: Long): VirtualMachine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVm(vm: VirtualMachine): Long

    @Update
    suspend fun updateVm(vm: VirtualMachine)

    @Delete
    suspend fun deleteVm(vm: VirtualMachine)

    @Query("DELETE FROM virtual_machines WHERE id = :id")
    suspend fun deleteVmById(id: Long)

    @Query("DELETE FROM virtual_machines")
    suspend fun deleteAllVms()

    @Query("UPDATE virtual_machines SET status = :status WHERE id = :id")
    suspend fun updateVmStatus(id: Long, status: String)

    @Query("UPDATE virtual_machines SET lastRunAt = :timestamp WHERE id = :id")
    suspend fun updateLastRun(id: Long, timestamp: Long)

    // Snapshots
    @Query("SELECT * FROM vm_snapshots WHERE vmId = :vmId ORDER BY timestamp DESC")
    fun getSnapshotsForVm(vmId: Long): Flow<List<VmSnapshot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: VmSnapshot): Long

    @Delete
    suspend fun deleteSnapshot(snapshot: VmSnapshot)
}
