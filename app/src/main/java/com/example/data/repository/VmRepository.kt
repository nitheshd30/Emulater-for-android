package com.example.data.repository

import com.example.data.db.VmDao
import com.example.data.model.VirtualMachine
import com.example.data.model.VmSnapshot
import kotlinx.coroutines.flow.Flow

class VmRepository(private val vmDao: VmDao) {
    val allVms: Flow<List<VirtualMachine>> = vmDao.getAllVms()

    fun getVmById(id: Long): Flow<VirtualMachine?> = vmDao.getVmByIdFlow(id)

    suspend fun getVmDirect(id: Long): VirtualMachine? = vmDao.getVmById(id)

    suspend fun insertVm(vm: VirtualMachine): Long = vmDao.insertVm(vm)

    suspend fun updateVm(vm: VirtualMachine) = vmDao.updateVm(vm)

    suspend fun deleteVm(vm: VirtualMachine) = vmDao.deleteVm(vm)

    suspend fun deleteVmById(id: Long) = vmDao.deleteVmById(id)

    suspend fun setVmStatus(id: Long, status: String) = vmDao.updateVmStatus(id, status)

    suspend fun markLastRun(id: Long) = vmDao.updateLastRun(id, System.currentTimeMillis())

    fun getSnapshots(vmId: Long): Flow<List<VmSnapshot>> = vmDao.getSnapshotsForVm(vmId)

    suspend fun insertSnapshot(snapshot: VmSnapshot): Long = vmDao.insertSnapshot(snapshot)

    suspend fun deleteSnapshot(snapshot: VmSnapshot) = vmDao.deleteSnapshot(snapshot)
}
