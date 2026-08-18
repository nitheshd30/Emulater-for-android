package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File

data class DeviceHardwareInfo(
    val modelName: String,
    val androidVersion: String,
    val apiLevel: Int,
    val primaryAbi: String,
    val isArm64: Boolean,
    val cpuCores: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val recommendedVmRamMb: Int,
    val freeStorageGb: Long,
    val isKvmAvailable: Boolean,
    val kvmStatusMessage: String
)

object HardwareDetector {

    fun detect(context: Context): DeviceHardwareInfo {
        val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val isArm64 = primaryAbi.contains("arm64") || primaryAbi.contains("aarch64")

        // Memory info
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availableRamMb = memInfo.availMem / (1024 * 1024)

        // Storage info
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeStorageGb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024)

        // Recommend safe VM RAM (approx 40-50% of total RAM, capped safely)
        val recommendedVmRamMb = when {
            totalRamMb >= 16000 -> 8192
            totalRamMb >= 12000 -> 6144
            totalRamMb >= 8000 -> 4096
            totalRamMb >= 6000 -> 3072
            else -> 2048
        }

        // KVM Detection check
        val kvmFile = File("/dev/kvm")
        val isKvmAvailable = try {
            kvmFile.exists() && kvmFile.canRead()
        } catch (_: Exception) {
            false
        }

        val kvmStatusMessage = if (isKvmAvailable) {
            "KVM hardware virtualization is active (/dev/kvm available). Near-native speed enabled!"
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isArm64) {
            "KVM supported on Android 13+ ARM64 kernels. Can be enabled via Wireless Debugging / Root / AVF hypervisor. Falling back to dynamic TCG JIT engine."
        } else {
            "Running via high-performance ARM64 JIT dynamic binary translator (TCG)."
        }

        return DeviceHardwareInfo(
            modelName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            apiLevel = Build.VERSION.SDK_INT,
            primaryAbi = primaryAbi,
            isArm64 = isArm64,
            cpuCores = cpuCores,
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            recommendedVmRamMb = recommendedVmRamMb,
            freeStorageGb = freeStorageGb,
            isKvmAvailable = isKvmAvailable,
            kvmStatusMessage = kvmStatusMessage
        )
    }
}
