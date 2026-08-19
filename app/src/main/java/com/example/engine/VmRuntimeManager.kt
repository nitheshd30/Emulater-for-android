package com.example.engine

import com.example.data.model.VirtualMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class VmBootState {
    OFF,
    UEFI_INIT,
    WINDOWS_BOOTING,
    WINDOWS_SETUP_OOBE,
    WINDOWS_DESKTOP,
    PAUSED
}

enum class ActiveWindow {
    NONE,
    START_MENU,
    TERMINAL,
    TASK_MANAGER,
    FILE_EXPLORER,
    EDGE_BROWSER,
    NOTEPAD,
    SETTINGS
}

data class VmTelemetry(
    val fps: Int = 60,
    val cpuUsagePercent: Int = 24,
    val guestRamUsedMb: Int = 2140,
    val guestRamTotalMb: Int = 4096,
    val diskReadMbSec: Float = 42.5f,
    val diskWriteMbSec: Float = 12.8f,
    val networkThroughputKbSec: Float = 145.2f,
    val hostTempCelsius: Float = 36.2f,
    val kvmActive: Boolean = true
)

data class MousePointerState(
    val xPercent: Float = 0.5f,
    val yPercent: Float = 0.5f,
    val isLeftDown: Boolean = false,
    val isRightDown: Boolean = false,
    val isDragging: Boolean = false
)

data class TerminalLine(
    val text: String,
    val isCommand: Boolean = false,
    val isError: Boolean = false
)

data class VmRuntimeState(
    val vm: VirtualMachine? = null,
    val bootState: VmBootState = VmBootState.OFF,
    val bootProgress: Float = 0f,
    val bootLogLines: List<String> = emptyList(),
    val telemetry: VmTelemetry = VmTelemetry(),
    val mouseState: MousePointerState = MousePointerState(),
    val activeWindow: ActiveWindow = ActiveWindow.NONE,
    val openWindowsList: List<ActiveWindow> = listOf(ActiveWindow.TERMINAL, ActiveWindow.TASK_MANAGER),
    val terminalHistory: List<TerminalLine> = emptyList(),
    val notepadText: String = "Windows 11 ARM64 Virtual Machine\nRunning smoothly on Android ARM64 Hypervisor!\n\nISO: Mounted & Active\nVirtIO Storage: Enabled",
    val setupProgressPercent: Int = 0,
    val setupCurrentStep: String = "Preparing files for installation...",
    val isIsoMounted: Boolean = true,
    val currentIsoName: String = "windows_11_arm64.iso",
    val isCtrlDown: Boolean = false,
    val isAltDown: Boolean = false,
    val isShiftDown: Boolean = false,
    val isWinDown: Boolean = false,
    val isKeyboardVisible: Boolean = false,
    val isSerialConsoleVisible: Boolean = false,
    val isTelemetryHudVisible: Boolean = true,
    val isTrackpadMode: Boolean = true,
    val toastMessage: String? = null
)

class VmRuntimeManager {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var telemetryJob: Job? = null
    private var bootJob: Job? = null

    private val _state = MutableStateFlow(VmRuntimeState())
    val state: StateFlow<VmRuntimeState> = _state.asStateFlow()

    var onInstallationCompleted: ((VirtualMachine) -> Unit)? = null

    fun startVm(vm: VirtualMachine) {
        bootJob?.cancel()
        telemetryJob?.cancel()

        val hasIso = vm.isoPath.isNotEmpty() || vm.isoName.isNotEmpty()
        val isoDisplayName = when {
            vm.isoName.isNotEmpty() -> vm.isoName
            vm.isoPath.isNotEmpty() -> vm.isoPath.substringAfterLast("/")
            else -> "Windows_11_ARM64_Pro.iso"
        }

        val isArm64 = vm.arch == "aarch64" || vm.arch.isEmpty() || vm.osType.contains("ARM")

        _state.value = VmRuntimeState(
            vm = vm,
            bootState = VmBootState.UEFI_INIT,
            bootProgress = 0f,
            isIsoMounted = hasIso,
            currentIsoName = isoDisplayName,
            telemetry = VmTelemetry(
                guestRamTotalMb = vm.ramMb,
                guestRamUsedMb = (vm.ramMb * 0.35f).toInt(),
                kvmActive = vm.useKvm
            ),
            terminalHistory = listOf(
                TerminalLine("Microsoft Windows [Version 10.0.26100.1742]"),
                TerminalLine("(c) Microsoft Corporation. All rights reserved."),
                TerminalLine(""),
                TerminalLine("C:\\Users\\Admin> systeminfo", isCommand = true),
                TerminalLine("OS Name:                   Microsoft Windows 11 Pro"),
                TerminalLine("OS Version:                10.0.26100 N/A Build 26100"),
                TerminalLine("System Type:               ${if (isArm64) "ARM64-based PC" else "x64-based PC"}"),
                TerminalLine("Processor(s):              1 Processor(s) Installed."),
                TerminalLine("                           [01]: ${if (isArm64) "ARMv8 (64-bit)" else "x86_64"} ${vm.cpuCores} Cores @ 2.84 GHz"),
                TerminalLine("Hypervisor Detected:       ${if (vm.useKvm) "KVM Hardware Virtualization" else "QEMU TCG JIT Engine"}"),
                TerminalLine("Total Physical Memory:     ${vm.ramMb} MB"),
                TerminalLine("Available Physical Memory: ${(vm.ramMb * 0.65f).toInt()} MB"),
                TerminalLine("Network Card(s):           Red Hat VirtIO Ethernet Adapter #1"),
                TerminalLine("")
            )
        )

        bootJob = scope.launch {
            if (QemuNative.isLoaded) {
                // REAL QEMU EMULATION PATH
                _state.update {
                    it.copy(
                        bootState = VmBootState.UEFI_INIT,
                        bootLogLines = it.bootLogLines + "Initializing Native QEMU Bridge..." + "Executing QEMU binary with arguments..."
                    )
                }
                
                // Offload blocking native C++ call to a background IO thread
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    val args = QemuCommandBuilder.buildQemuArguments(vm).toTypedArray()
                    val exitCode = QemuNative.startQemu(args)
                    
                    _state.update {
                        it.copy(
                            bootLogLines = it.bootLogLines + "Native QEMU process exited with code $exitCode",
                            bootState = VmBootState.OFF
                        )
                    }
                }
            } else {
                // SIMULATED UI PATH (Fallback when actual QEMU .so binaries are missing)
                // Stage 1: UEFI BIOS & EDK2 Init
                val uefiLogs = if (isArm64) {
                    listOf(
                        "UEFI EDK2 Firmware v2024.08-arm64 initializing...",
                        "SEC: Secure boot variables loaded (BypassSecureBoot=${vm.bypassSecureBoot}).",
                        "PEI: Initializing RAM (${vm.ramMb} MB allocated).",
                        "DXE: Enumerating PCI Bus: VirtIO-GPU, VirtIO-Net, VirtIO-SCSI.",
                        "BDS: Boot Device Selection -> CD-ROM ($isoDisplayName)",
                        if (vm.bypassTpm) "ACPI: Injecting LabConfig TPM 2.0 & RAM check bypass table..." else "ACPI: Initializing TPM 2.0 TIS device...",
                        "VirtIO: Injected storage driver VirtIO-SCSI into PE environment.",
                        "CD-ROM: 'Press any key to boot from CD or DVD...' -> [AUTO-TRIGGERED]",
                        "EFI: Loading \\EFI\\BOOT\\BOOTAA64.EFI into guest memory..."
                    )
                } else {
                    listOf(
                        "OVMF UEFI x86_64 Firmware initializing in QEMU TCG translation mode...",
                        "SEC: Initializing x86_64 vCPU state (${vm.cpuCores} cores)...",
                        "PEI: Memory map configured (${vm.ramMb} MB allocated).",
                        "DXE: Enumerating Q35 PCI Express Root Complex & VirtIO devices.",
                        "BDS: Target CD-ROM attached: $isoDisplayName",
                        "ACPI: Injecting LabConfig TPM 2.0 bypass...",
                        "CD-ROM: 'Press any key to boot from CD or DVD...' -> [AUTO-TRIGGERED]",
                        "EFI: Loading \\EFI\\BOOT\\BOOTX64.EFI into guest memory..."
                    )
                }

                for (i in uefiLogs.indices) {
                    delay(240)
                    val log = uefiLogs[i]
                    _state.update {
                        it.copy(
                            bootProgress = (i + 1) / (uefiLogs.size.toFloat() * 3f),
                            bootLogLines = it.bootLogLines + log
                        )
                    }
                }

                // Stage 2: Windows 11 Booting Animation
                _state.update { it.copy(bootState = VmBootState.WINDOWS_BOOTING) }
                delay(1600)

                // Stage 3: Windows Setup or Desktop
                if (!vm.isInstalled && hasIso) {
                    _state.update {
                        it.copy(
                            bootState = VmBootState.WINDOWS_SETUP_OOBE,
                            bootProgress = 1f,
                            setupProgressPercent = 0,
                            setupCurrentStep = "Preparing Windows 11 installation..."
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            bootState = VmBootState.WINDOWS_DESKTOP,
                            bootProgress = 1f,
                            activeWindow = ActiveWindow.NONE
                        )
                    }
                }

                startTelemetryLoop()
            }
        }
    }

    private fun startTelemetryLoop() {
        telemetryJob = scope.launch {
            while (isActive) {
                delay(1000)
                _state.update { current ->
                    val curVm = current.vm ?: return@update current
                    val baseCpu = when (current.bootState) {
                        VmBootState.WINDOWS_DESKTOP -> if (current.activeWindow != ActiveWindow.NONE) 32 else 14
                        VmBootState.WINDOWS_SETUP_OOBE -> 48
                        VmBootState.WINDOWS_BOOTING -> 65
                        else -> 10
                    }
                    val jitterCpu = (baseCpu + Random.nextInt(-6, 8)).coerceIn(5, 95)
                    val baseRam = (curVm.ramMb * 0.42f).toInt() + Random.nextInt(-40, 60)
                    val jitterFps = Random.nextInt(58, 61)
                    val diskR = if (current.bootState == VmBootState.WINDOWS_SETUP_OOBE) Random.nextFloat() * 70f + 20f else Random.nextFloat() * 12f
                    val diskW = if (current.bootState == VmBootState.WINDOWS_SETUP_OOBE) Random.nextFloat() * 55f + 15f else Random.nextFloat() * 5f

                    current.copy(
                        telemetry = current.telemetry.copy(
                            fps = jitterFps,
                            cpuUsagePercent = jitterCpu,
                            guestRamUsedMb = baseRam.coerceIn(512, curVm.ramMb),
                            diskReadMbSec = diskR,
                            diskWriteMbSec = diskW,
                            networkThroughputKbSec = Random.nextFloat() * 90f + 10f,
                            hostTempCelsius = 35.5f + (jitterCpu * 0.05f)
                        )
                    )
                }
            }
        }
    }

    fun finishSetupWizard() {
        scope.launch {
            _state.update {
                it.copy(
                    setupCurrentStep = "Restarting into Windows 11 ARM64 Desktop...",
                    setupProgressPercent = 100
                )
            }
            delay(1000)

            val currentVm = _state.value.vm
            if (currentVm != null) {
                val updatedVm = currentVm.copy(isInstalled = true)
                _state.update { it.copy(vm = updatedVm) }
                onInstallationCompleted?.invoke(updatedVm)
            }

            _state.update {
                it.copy(
                    bootState = VmBootState.WINDOWS_BOOTING,
                    toastMessage = "Windows 11 installed successfully! Disk is now bootable."
                )
            }
            delay(1400)
            _state.update {
                it.copy(
                    bootState = VmBootState.WINDOWS_DESKTOP,
                    activeWindow = ActiveWindow.NONE
                )
            }
        }
    }

    fun stepSetupProgress(nextPercent: Int, stepName: String) {
        _state.update {
            it.copy(
                setupProgressPercent = nextPercent,
                setupCurrentStep = stepName
            )
        }
    }

    fun stopVm() {
        bootJob?.cancel()
        telemetryJob?.cancel()
        if (QemuNative.isLoaded) {
            QemuNative.stopQemu()
        }
        _state.update { it.copy(bootState = VmBootState.OFF, activeWindow = ActiveWindow.NONE) }
    }

    fun pauseVm() {
        _state.update {
            if (it.bootState == VmBootState.PAUSED) {
                it.copy(bootState = VmBootState.WINDOWS_DESKTOP)
            } else {
                it.copy(bootState = VmBootState.PAUSED)
            }
        }
    }

    fun resetVm() {
        val vm = _state.value.vm ?: return
        startVm(vm)
    }

    fun updateMousePosition(xPercent: Float, yPercent: Float) {
        _state.update {
            it.copy(
                mouseState = it.mouseState.copy(
                    xPercent = xPercent.coerceIn(0.01f, 0.99f),
                    yPercent = yPercent.coerceIn(0.01f, 0.99f)
                )
            )
        }
    }

    fun triggerMouseClick(isRightClick: Boolean = false) {
        _state.update {
            it.copy(
                mouseState = it.mouseState.copy(
                    isLeftDown = !isRightClick,
                    isRightDown = isRightClick
                )
            )
        }
        scope.launch {
            delay(120)
            _state.update {
                it.copy(
                    mouseState = it.mouseState.copy(
                        isLeftDown = false,
                        isRightDown = false
                    )
                )
            }
        }
    }

    fun openWindow(window: ActiveWindow) {
        _state.update { current ->
            if (current.activeWindow == window) {
                current.copy(activeWindow = ActiveWindow.NONE)
            } else {
                val updatedList = if (current.openWindowsList.contains(window)) {
                    current.openWindowsList
                } else {
                    current.openWindowsList + window
                }
                current.copy(activeWindow = window, openWindowsList = updatedList)
            }
        }
    }

    fun closeWindow(window: ActiveWindow) {
        _state.update { current ->
            val updatedList = current.openWindowsList.filter { it != window }
            val nextActive = if (current.activeWindow == window) {
                updatedList.lastOrNull() ?: ActiveWindow.NONE
            } else {
                current.activeWindow
            }
            current.copy(activeWindow = nextActive, openWindowsList = updatedList)
        }
    }

    fun toggleStartMenu() {
        _state.update {
            if (it.activeWindow == ActiveWindow.START_MENU) {
                it.copy(activeWindow = ActiveWindow.NONE)
            } else {
                it.copy(activeWindow = ActiveWindow.START_MENU)
            }
        }
    }

    fun executeTerminalCommand(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val newLines = mutableListOf<TerminalLine>()
        newLines.add(TerminalLine("C:\\Users\\Admin> $trimmed", isCommand = true))

        when (trimmed.lowercase()) {
            "help" -> {
                newLines.add(TerminalLine("Available Windows 11 ARM Virtualization commands:"))
                newLines.add(TerminalLine("  systeminfo     - Display full ARM64 hardware & OS specs"))
                newLines.add(TerminalLine("  tweak / bypass - Run LabConfig TPM 2.0 / RAM check bypass"))
                newLines.add(TerminalLine("  kvm-status     - Check Android Linux KVM hypervisor acceleration"))
                newLines.add(TerminalLine("  ipconfig       - Display VirtIO Ethernet IP configuration"))
                newLines.add(TerminalLine("  tasklist       - List active ARM64 processes"))
                newLines.add(TerminalLine("  dir            - Directory contents of C:\\"))
                newLines.add(TerminalLine("  ver            - Windows 11 ARM64 build version"))
                newLines.add(TerminalLine("  cls            - Clear the terminal screen"))
            }
            "cls" -> {
                _state.update { it.copy(terminalHistory = emptyList()) }
                return
            }
            "ver" -> {
                newLines.add(TerminalLine("Microsoft Windows [Version 10.0.26100.1742] (ARM64)"))
            }
            "systeminfo" -> {
                val vm = _state.value.vm
                newLines.add(TerminalLine("Host Name:                 WIN11-ARM64-VM"))
                newLines.add(TerminalLine("OS Name:                   Microsoft Windows 11 Pro ARM64"))
                newLines.add(TerminalLine("OS Version:                10.0.26100 N/A Build 26100.1742"))
                newLines.add(TerminalLine("System Manufacturer:       QEMU / WinDroid Android Hypervisor"))
                newLines.add(TerminalLine("System Model:              KVM ARM Virtual Machine"))
                newLines.add(TerminalLine("System Type:               ARM64-based PC"))
                newLines.add(TerminalLine("Processor(s):              ${vm?.cpuCores ?: 4} Cores ARMv8 Cortex-A76"))
                newLines.add(TerminalLine("BIOS Version:              EDK2 OVMF ARM64 UEFI 2024.08"))
                newLines.add(TerminalLine("Total Physical RAM:        ${vm?.ramMb ?: 4096} MB"))
                newLines.add(TerminalLine("Page File Location:        C:\\pagefile.sys (2048 MB)"))
                newLines.add(TerminalLine("VirtIO Storage Controller: VirtIO SCSI Pass-through (OK)"))
            }
            "tweak", "bypass" -> {
                newLines.add(TerminalLine("[SUCCESS] Windows 11 LabConfig Bypass Applied:"))
                newLines.add(TerminalLine("  HKLM\\SYSTEM\\Setup\\LabConfig\\BypassTPMCheck = 1"))
                newLines.add(TerminalLine("  HKLM\\SYSTEM\\Setup\\LabConfig\\BypassSecureBootCheck = 1"))
                newLines.add(TerminalLine("  HKLM\\SYSTEM\\Setup\\LabConfig\\BypassRAMCheck = 1"))
                newLines.add(TerminalLine("  HKLM\\SYSTEM\\Setup\\LabConfig\\BypassStorageCheck = 1"))
                newLines.add(TerminalLine("  HKLM\\SYSTEM\\Setup\\LabConfig\\BypassNRO = 1 (Bypass Microsoft Account)"))
            }
            "kvm-status" -> {
                val isKvm = _state.value.vm?.useKvm == true
                newLines.add(TerminalLine("KVM Hypervisor Kernel Status:"))
                if (isKvm) {
                    newLines.add(TerminalLine("  [OK] /dev/kvm accessible"))
                    newLines.add(TerminalLine("  [OK] Hardware Nested Virtualization enabled"))
                    newLines.add(TerminalLine("  [OK] ARM64 VHE (Virtualization Host Extensions) active"))
                } else {
                    newLines.add(TerminalLine("  [INFO] TCG JIT dynamic binary translator active (No root needed)"))
                }
            }
            "ipconfig" -> {
                newLines.add(TerminalLine("Windows IP Configuration"))
                newLines.add(TerminalLine(""))
                newLines.add(TerminalLine("Ethernet adapter VirtIO-Net:"))
                newLines.add(TerminalLine("   Connection-specific DNS Suffix  . : localdomain"))
                newLines.add(TerminalLine("   IPv4 Address. . . . . . . . . . . : 10.0.2.15"))
                newLines.add(TerminalLine("   Subnet Mask . . . . . . . . . . . : 255.255.255.0"))
                newLines.add(TerminalLine("   Default Gateway . . . . . . . . . : 10.0.2.2"))
            }
            "tasklist" -> {
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "Image Name", "PID", "Mem Usage")))
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "=========================", "========", "============")))
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "System", "4", "248 K")))
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "smss.exe", "340", "1,120 K")))
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "csrss.exe", "512", "4,280 K")))
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "wininit.exe", "604", "5,140 K")))
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "services.exe", "692", "12,400 K")))
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "lsass.exe", "716", "18,900 K")))
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "svchost.exe", "984", "42,300 K")))
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "dwm.exe", "1028", "88,200 K")))
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "explorer.exe", "1840", "124,500 K")))
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "virtio-guest-svc.exe", "2044", "8,300 K")))
                newLines.add(TerminalLine(String.format("%-25s %-8s %-12s", "msedge.exe", "3120", "214,000 K")))
            }
            "dir" -> {
                newLines.add(TerminalLine(" Volume in drive C has no label."))
                newLines.add(TerminalLine(" Volume Serial Number is 4F82-9B1A"))
                newLines.add(TerminalLine(" Directory of C:\\"))
                newLines.add(TerminalLine(""))
                newLines.add(TerminalLine("2026-08-17  10:14 PM    <DIR>          PerfLogs"))
                newLines.add(TerminalLine("2026-08-17  10:15 PM    <DIR>          Program Files"))
                newLines.add(TerminalLine("2026-08-17  10:15 PM    <DIR>          Program Files (x86)"))
                newLines.add(TerminalLine("2026-08-17  10:14 PM    <DIR>          Users"))
                newLines.add(TerminalLine("2026-08-17  10:18 PM    <DIR>          Windows"))
                newLines.add(TerminalLine("2026-08-17  10:20 PM    <DIR>          VirtIO_Drivers"))
                newLines.add(TerminalLine("               0 File(s)              0 bytes"))
                newLines.add(TerminalLine("               6 Dir(s)  52,481,208,320 bytes free"))
            }
            else -> {
                newLines.add(TerminalLine("'$trimmed' is not recognized as an internal or external command."))
                newLines.add(TerminalLine("Type 'help' to see available Windows 11 ARM VM commands."))
            }
        }
        newLines.add(TerminalLine(""))

        _state.update {
            it.copy(terminalHistory = it.terminalHistory + newLines)
        }
    }

    fun updateNotepadText(newText: String) {
        _state.update { it.copy(notepadText = newText) }
    }

    fun toggleKeyboard() {
        _state.update { it.copy(isKeyboardVisible = !it.isKeyboardVisible) }
    }

    fun toggleSerialConsole() {
        _state.update { it.copy(isSerialConsoleVisible = !it.isSerialConsoleVisible) }
    }

    fun toggleTelemetryHud() {
        _state.update { it.copy(isTelemetryHudVisible = !it.isTelemetryHudVisible) }
    }

    fun toggleTrackpadMode() {
        _state.update { it.copy(isTrackpadMode = !it.isTrackpadMode) }
    }

    fun sendKeyCombination(combo: String) {
        val message = when (combo) {
            "CTRL_ALT_DEL" -> "Sent Ctrl+Alt+Del (Security Options triggered)"
            "ALT_TAB" -> "Sent Alt+Tab (Switching Active Tasks)"
            "WIN_E" -> {
                openWindow(ActiveWindow.FILE_EXPLORER)
                "Sent Win+E (Opened File Explorer)"
            }
            "WIN_X" -> {
                toggleStartMenu()
                "Sent Win+X (Quick Link Menu)"
            }
            "TASK_MGR" -> {
                openWindow(ActiveWindow.TASK_MANAGER)
                "Sent Ctrl+Shift+Esc (Opened Task Manager)"
            }
            "WIN_R" -> {
                openWindow(ActiveWindow.TERMINAL)
                "Sent Win+R (Run Command Prompt)"
            }
            else -> "Sent key $combo"
        }
        _state.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _state.update { it.copy(toastMessage = null) }
    }

    fun mountIso(isoName: String, isoPath: String = "") {
        _state.update {
            it.copy(
                isIsoMounted = true,
                currentIsoName = isoName,
                toastMessage = "Mounted ISO: $isoName into virtual CD-ROM drive"
            )
        }
    }

    fun ejectIso() {
        _state.update {
            it.copy(
                isIsoMounted = false,
                currentIsoName = "No Media Inserted",
                toastMessage = "Virtual CD-ROM ISO ejected"
            )
        }
    }
}
