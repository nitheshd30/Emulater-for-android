package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "virtual_machines")
data class VirtualMachine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "Windows 11 ARM64",
    val osType: String = "WINDOWS_11_ARM", // WINDOWS_11_ARM, WINDOWS_10_ARM, UBUNTU_ARM, CUSTOM
    val arch: String = "aarch64", // aarch64, x86_64
    val cpuModel: String = "cortex-a76", // host, cortex-a76, cortex-a72, max
    val cpuCores: Int = 4,
    val ramMb: Int = 4096,
    val diskSizeGb: Int = 64,
    val diskFormat: String = "qcow2", // qcow2, raw, vhdx
    val diskPath: String = "",
    val isoPath: String = "",
    val isoName: String = "",
    val isoSizeBytes: Long = 0L,
    val useKvm: Boolean = true,
    val bypassTpm: Boolean = true,
    val bypassSecureBoot: Boolean = true,
    val bypassRamCheck: Boolean = true,
    val bypassOobeNetwork: Boolean = true,
    val virtIoDriversEnabled: Boolean = true,
    val displayResolution: String = "1600x900", // 1280x720, 1600x900, 1920x1080
    val gpuMode: String = "VIRGL", // VIRGL (3D), VIRTIO_GPU, RAMFB, BOCHS
    val audioDevice: String = "INTEL_HDA", // INTEL_HDA, AC97, NONE
    val networkMode: String = "USER_SLIRP", // USER_SLIRP, BRIDGED, NONE
    val portForwardRdp: Int = 3389,
    val portForwardSsh: Int = 2222,
    val portForwardWeb: Int = 8080,
    val sharedFolderPath: String = "",
    val status: String = "STOPPED", // STOPPED, RUNNING, PAUSED
    val isInstalled: Boolean = false,
    val osVersionDisplay: String = "Windows 11 Pro 24H2 (ARM64)",
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long = 0L
)
