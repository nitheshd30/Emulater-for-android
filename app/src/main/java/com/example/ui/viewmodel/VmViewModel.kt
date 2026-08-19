package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.VirtualMachine
import com.example.data.model.VmSnapshot
import com.example.data.repository.VmRepository
import com.example.engine.DeviceHardwareInfo
import com.example.engine.HardwareDetector
import com.example.engine.IsoInspectionResult
import com.example.engine.IsoInspector
import com.example.engine.VmRuntimeManager
import com.example.engine.VmRuntimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VmRepository
    val runtimeManager = VmRuntimeManager()

    val allVms: StateFlow<List<VirtualMachine>>
    val runtimeState: StateFlow<VmRuntimeState> = runtimeManager.state

    private val _hardwareInfo = MutableStateFlow(HardwareDetector.detect(application))
    val hardwareInfo: StateFlow<DeviceHardwareInfo> = _hardwareInfo.asStateFlow()

    private val _selectedVmForEdit = MutableStateFlow<VirtualMachine?>(null)
    val selectedVmForEdit: StateFlow<VirtualMachine?> = _selectedVmForEdit.asStateFlow()

    private val _currentIsoInspection = MutableStateFlow<IsoInspectionResult?>(null)
    val currentIsoInspection: StateFlow<IsoInspectionResult?> = _currentIsoInspection.asStateFlow()

    private val _snapshots = MutableStateFlow<List<VmSnapshot>>(emptyList())
    val snapshots: StateFlow<List<VmSnapshot>> = _snapshots.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = VmRepository(db.vmDao())

        allVms = repository.allVms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        runtimeManager.onInstallationCompleted = { updatedVm ->
            saveVm(updatedVm)
        }

        // Clean up any legacy dummy sample VM automatically
        viewModelScope.launch {
            val list = repository.allVms.firstOrNull() ?: emptyList()
            for (vm in list) {
                if (vm.isoPath == "content://com.android.providers.downloads/win11_arm64.iso" ||
                    vm.isoName == "Windows_11_ARM64_English_Pro.iso" ||
                    vm.name == "Windows 11 ARM64 (Sample)"
                ) {
                    repository.deleteVm(vm)
                }
            }
        }
    }

    fun clearAllVms() {
        viewModelScope.launch {
            if (runtimeState.value.vm != null) {
                runtimeManager.stopVm()
            }
            repository.deleteAllVms()
        }
    }

    fun selectVmForEdit(vm: VirtualMachine?) {
        _selectedVmForEdit.value = vm
        if (vm != null) {
            loadSnapshots(vm.id)
        }
    }

    fun loadSnapshots(vmId: Long) {
        viewModelScope.launch {
            repository.getSnapshots(vmId).collect {
                _snapshots.value = it
            }
        }
    }

    fun inspectIsoUri(uri: Uri) {
        val result = IsoInspector.inspectUri(getApplication(), uri)
        _currentIsoInspection.value = result
    }

    fun clearIsoInspection() {
        _currentIsoInspection.value = null
    }

    fun createVmFromIso(uri: Uri, customName: String? = null, onCreated: (VirtualMachine) -> Unit) {
        val inspection = IsoInspector.inspectUri(getApplication(), uri)
        val hw = _hardwareInfo.value
        val detectedArch = if (inspection.isArm64Iso) "aarch64" else "x86_64"

        val newVm = VirtualMachine(
            name = customName?.takeIf { it.isNotBlank() } ?: inspection.fileName.substringBeforeLast(".").ifEmpty { "Windows VM" },
            osType = if (inspection.isArm64Iso) "WINDOWS_11_ARM" else "WINDOWS_11_X64",
            arch = detectedArch,
            cpuCores = (hw.cpuCores / 2).coerceIn(2, 6),
            ramMb = hw.recommendedVmRamMb,
            diskSizeGb = 64,
            diskFormat = "qcow2",
            isoPath = uri.toString(),
            isoName = inspection.fileName,
            isoSizeBytes = inspection.fileSizeBytes,
            useKvm = hw.isKvmAvailable && inspection.isArm64Iso,
            bypassTpm = inspection.requiresTpmBypass,
            bypassSecureBoot = true,
            bypassRamCheck = true,
            bypassOobeNetwork = true,
            virtIoDriversEnabled = true,
            displayResolution = "1600x900",
            gpuMode = "VIRGL",
            audioDevice = "INTEL_HDA",
            networkMode = "USER_SLIRP",
            osVersionDisplay = inspection.detectedOs
        )

        viewModelScope.launch {
            val id = repository.insertVm(newVm)
            val created = newVm.copy(id = id)
            onCreated(created)
        }
    }

    fun saveVm(vm: VirtualMachine, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            if (vm.id == 0L) {
                val newId = repository.insertVm(vm)
                onSaved(newId)
            } else {
                repository.updateVm(vm)
                onSaved(vm.id)
            }
        }
    }

    fun deleteVm(vm: VirtualMachine) {
        viewModelScope.launch {
            if (runtimeState.value.vm?.id == vm.id) {
                runtimeManager.stopVm()
            }
            repository.deleteVm(vm)
        }
    }

    fun duplicateVm(vm: VirtualMachine) {
        viewModelScope.launch {
            val copy = vm.copy(
                id = 0L,
                name = "${vm.name} (Copy)",
                createdAt = System.currentTimeMillis(),
                status = "STOPPED"
            )
            repository.insertVm(copy)
        }
    }

    fun createQuickTemplate(osType: String) {
        val hw = _hardwareInfo.value
        val template = when (osType) {
            "WIN11_TINY" -> VirtualMachine(
                name = "Tiny11 ARM64",
                osType = "WINDOWS_11_ARM",
                arch = "aarch64",
                cpuCores = 2,
                ramMb = 2048,
                diskSizeGb = 32,
                isoName = "Tiny11_ARM64_Lite.iso",
                useKvm = hw.isKvmAvailable,
                bypassTpm = true,
                bypassSecureBoot = true,
                bypassRamCheck = true,
                gpuMode = "VIRTIO_GPU",
                osVersionDisplay = "Tiny11 ARM64 (Stripped Light Edition)"
            )
            "WIN10_ARM" -> VirtualMachine(
                name = "Windows 10 ARM64",
                osType = "WINDOWS_10_ARM",
                arch = "aarch64",
                cpuCores = 4,
                ramMb = 3072,
                diskSizeGb = 48,
                isoName = "Windows_10_ARM64_21H2.iso",
                useKvm = hw.isKvmAvailable,
                bypassTpm = false,
                bypassSecureBoot = false,
                osVersionDisplay = "Windows 10 Pro (21H2 ARM64)"
            )
            "UBUNTU_ARM" -> VirtualMachine(
                name = "Ubuntu 24.04 ARM64",
                osType = "UBUNTU_ARM",
                arch = "aarch64",
                cpuCores = 4,
                ramMb = 4096,
                diskSizeGb = 32,
                isoName = "ubuntu-24.04-desktop-arm64.iso",
                useKvm = hw.isKvmAvailable,
                osVersionDisplay = "Ubuntu Desktop 24.04 LTS (aarch64)"
            )
            else -> VirtualMachine(
                name = "Windows 11 ARM64 Pro",
                osType = "WINDOWS_11_ARM",
                arch = "aarch64",
                cpuCores = (hw.cpuCores / 2).coerceIn(2, 6),
                ramMb = hw.recommendedVmRamMb,
                diskSizeGb = 64,
                isoName = "Windows_11_ARM64_Pro_24H2.iso",
                useKvm = hw.isKvmAvailable,
                bypassTpm = true,
                bypassSecureBoot = true,
                bypassRamCheck = true,
                osVersionDisplay = "Windows 11 Pro 24H2 (ARM64)"
            )
        }
        viewModelScope.launch {
            repository.insertVm(template)
        }
    }

    fun launchVm(vm: VirtualMachine) {
        viewModelScope.launch {
            repository.markLastRun(vm.id)
            repository.setVmStatus(vm.id, "RUNNING")
            runtimeManager.startVm(vm)
        }
    }

    fun suspendVm(vm: VirtualMachine) {
        viewModelScope.launch {
            repository.setVmStatus(vm.id, "SUSPENDED")
            if (runtimeState.value.vm?.id == vm.id) {
                runtimeManager.pauseVm()
            }
        }
    }

    fun resumeVm(vm: VirtualMachine) {
        viewModelScope.launch {
            repository.markLastRun(vm.id)
            repository.setVmStatus(vm.id, "RUNNING")
            if (runtimeState.value.vm?.id == vm.id && runtimeState.value.bootState == com.example.engine.VmBootState.PAUSED) {
                runtimeManager.pauseVm() // unpause
            } else {
                runtimeManager.startVm(vm)
            }
        }
    }

    fun stopVm(vm: VirtualMachine) {
        viewModelScope.launch {
            repository.setVmStatus(vm.id, "STOPPED")
            if (runtimeState.value.vm?.id == vm.id) {
                runtimeManager.stopVm()
            }
        }
    }

    fun restartVm(vm: VirtualMachine) {
        viewModelScope.launch {
            repository.markLastRun(vm.id)
            repository.setVmStatus(vm.id, "RUNNING")
            runtimeManager.stopVm()
            kotlinx.coroutines.delay(200)
            runtimeManager.startVm(vm)
        }
    }

    fun stopActiveVm() {
        val active = runtimeState.value.vm
        if (active != null) {
            stopVm(active)
        }
    }

    fun takeSnapshot(vmId: Long, title: String, description: String = "") {
        viewModelScope.launch {
            val snap = VmSnapshot(
                vmId = vmId,
                title = title.ifEmpty { "Snapshot ${System.currentTimeMillis() % 10000}" },
                description = description,
                sizeBytes = 240L * 1024L * 1024L
            )
            repository.insertSnapshot(snap)
            loadSnapshots(vmId)
        }
    }

    fun deleteSnapshot(snapshot: VmSnapshot) {
        viewModelScope.launch {
            repository.deleteSnapshot(snapshot)
            loadSnapshots(snapshot.vmId)
        }
    }

    fun createVmFromIsoUri(uri: Uri, onCreated: (VirtualMachine) -> Unit) {
        viewModelScope.launch {
            val app = getApplication<android.app.Application>()
            try {
                app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            val inspection = IsoInspector.inspectUri(app, uri)
            val hw = _hardwareInfo.value
            val isArm = inspection.isArm64Iso
            val detectedArch = if (isArm) "aarch64" else "x86_64"
            val vmName = inspection.fileName.substringBeforeLast(".").replace("_", " ").ifEmpty {
                if (isArm) "Windows 11 ARM64" else "Windows 11 x64"
            }
            val newVm = VirtualMachine(
                name = vmName,
                osType = if (isArm) "WINDOWS_11_ARM" else "WINDOWS_11_X64",
                arch = detectedArch,
                cpuCores = (hw.cpuCores / 2).coerceIn(2, 6),
                ramMb = hw.recommendedVmRamMb,
                diskSizeGb = 64,
                isoPath = uri.toString(),
                isoName = inspection.fileName,
                isoSizeBytes = inspection.fileSizeBytes,
                useKvm = hw.isKvmAvailable && isArm,
                bypassTpm = true,
                bypassSecureBoot = true,
                bypassRamCheck = true,
                bypassOobeNetwork = true,
                virtIoDriversEnabled = true,
                osVersionDisplay = if (isArm) "Windows 11 Pro (ARM64)" else "Windows 11 Pro (x86_64 TCG)"
            )
            val id = repository.insertVm(newVm)
            val finalVm = newVm.copy(id = id)
            onCreated(finalVm)
        }
    }

    fun refreshHardware() {
        _hardwareInfo.value = HardwareDetector.detect(getApplication())
    }
}
