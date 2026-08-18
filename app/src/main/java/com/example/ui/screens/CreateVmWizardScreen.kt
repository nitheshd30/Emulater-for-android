package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VirtualMachine
import com.example.ui.components.KvmChip
import com.example.ui.components.WindowsLogoMini
import com.example.ui.theme.KvmGreen
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.WindowsCyan
import com.example.ui.theme.WindowsDeepBlue
import com.example.ui.viewmodel.VmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVmWizardScreen(
    viewModel: VmViewModel,
    onNavigateBack: () -> Unit,
    onVmCreated: (VirtualMachine) -> Unit,
    modifier: Modifier = Modifier
) {
    val hardware by viewModel.hardwareInfo.collectAsStateWithLifecycle()
    val isoInspection by viewModel.currentIsoInspection.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("1. OS & Image", "2. Hardware", "3. Win11 Tweaks", "4. Display & Net")

    // Form state
    var vmName by remember { mutableStateOf("Windows 11 ARM64 Pro") }
    var selectedOsType by remember { mutableStateOf("WINDOWS_11_ARM") }
    var isoUriString by remember { mutableStateOf("") }
    var isoFileName by remember { mutableStateOf("Windows_11_ARM64_Pro_24H2.iso") }
    var isoSizeBytes by remember { mutableStateOf(5626896384L) }

    var cpuCores by remember { mutableFloatStateOf((hardware.cpuCores / 2).coerceIn(2, 6).toFloat()) }
    var ramMb by remember { mutableFloatStateOf(hardware.recommendedVmRamMb.toFloat()) }
    var diskSizeGb by remember { mutableFloatStateOf(64f) }
    var diskFormat by remember { mutableStateOf("qcow2") }

    var bypassTpm by remember { mutableStateOf(true) }
    var bypassSecureBoot by remember { mutableStateOf(true) }
    var bypassRamCheck by remember { mutableStateOf(true) }
    var bypassOobeNetwork by remember { mutableStateOf(true) }
    var virtIoDriversEnabled by remember { mutableStateOf(true) }

    var useKvm by remember { mutableStateOf(hardware.isKvmAvailable) }
    var gpuMode by remember { mutableStateOf("VIRGL") }
    var displayResolution by remember { mutableStateOf("1600x900") }
    var audioDevice by remember { mutableStateOf("INTEL_HDA") }
    var networkMode by remember { mutableStateOf("USER_SLIRP") }
    var portForwardRdp by remember { mutableIntStateOf(3389) }

    // SAF Document Picker for ISO
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isoUriString = uri.toString()
            viewModel.inspectIsoUri(uri)
        }
    }

    // React to ISO inspection updates
    if (isoInspection != null) {
        isoFileName = isoInspection!!.fileName
        isoSizeBytes = isoInspection!!.fileSizeBytes
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WindowsLogoMini(size = 20)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Virtual Machine", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate900,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = Slate300
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Slate900,
                contentColor = WindowsCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = WindowsCyan
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) WindowsCyan else Slate400
                            )
                        }
                    )
                }
            }

            // Scrollable Content Pane
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // OS & ISO Selection Tab
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Virtual Machine Name", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate300)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = vmName,
                                    onValueChange = { vmName = it },
                                    modifier = Modifier.fillMaxWidth().testTag("input_vm_name"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WindowsCyan,
                                        unfocusedBorderColor = Slate700,
                                        focusedContainerColor = Slate850,
                                        unfocusedContainerColor = Slate850
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Operating System Preset", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate300)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OsPresetCard(
                                        title = "Windows 11 ARM",
                                        subtitle = "ARM64 24H2 / 23H2",
                                        isSelected = selectedOsType == "WINDOWS_11_ARM",
                                        onClick = {
                                            selectedOsType = "WINDOWS_11_ARM"
                                            vmName = "Windows 11 ARM64 Pro"
                                            bypassTpm = true
                                            bypassSecureBoot = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OsPresetCard(
                                        title = "Windows 10 ARM",
                                        subtitle = "ARM64 21H2",
                                        isSelected = selectedOsType == "WINDOWS_10_ARM",
                                        onClick = {
                                            selectedOsType = "WINDOWS_10_ARM"
                                            vmName = "Windows 10 ARM64"
                                            bypassTpm = false
                                            bypassSecureBoot = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // ISO File Picker Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Boot ISO / Disk Image", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Select Windows 11 ARM ISO from Storage", fontSize = 12.sp, color = Slate400)
                                    }
                                    Button(
                                        onClick = {
                                            filePickerLauncher.launch(arrayOf("*/*"))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("btn_select_iso_file")
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Browse ISO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Attached ISO Info Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Slate850)
                                        .border(1.dp, if (isoInspection != null) KvmGreen.copy(alpha = 0.5f) else Slate700, RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Storage,
                                                contentDescription = null,
                                                tint = if (isoInspection != null) KvmGreen else WindowsCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = isoFileName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (isoInspection != null) {
                                                "Size: ${isoInspection!!.fileSizeFormatted} • ${isoInspection!!.detectedOs}\n${isoInspection!!.summaryNotes}"
                                            } else {
                                                "Default ARM64 disc image configured (~5.24 GB). You can replace this anytime by tapping 'Browse ISO'."
                                            },
                                            fontSize = 11.sp,
                                            color = Slate400,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Hardware Specs Tab (CPU, RAM, Disk)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // CPU Cores Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("CPU Cores", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${cpuCores.toInt()} Cores", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)
                                }
                                Text("Host has ${hardware.cpuCores} cores (${hardware.primaryAbi})", fontSize = 11.sp, color = Slate400)
                                Slider(
                                    value = cpuCores,
                                    onValueChange = { cpuCores = it },
                                    valueRange = 1f..hardware.cpuCores.toFloat().coerceAtLeast(4f),
                                    steps = (hardware.cpuCores.coerceAtLeast(4) - 2),
                                    colors = SliderDefaults.colors(
                                        thumbColor = WindowsCyan,
                                        activeTrackColor = WindowsCyan,
                                        inactiveTrackColor = Slate700
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // RAM Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("RAM Allocation", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(String.format("%.1f GB (%d MB)", ramMb / 1024f, ramMb.toInt()), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KvmGreen)
                                }
                                Text("Recommended: ${hardware.recommendedVmRamMb / 1024} GB (Host RAM: ${hardware.totalRamMb / 1024} GB)", fontSize = 11.sp, color = Slate400)
                                Slider(
                                    value = ramMb,
                                    onValueChange = { ramMb = it },
                                    valueRange = 1024f..(hardware.totalRamMb * 0.75f).coerceAtLeast(4096f),
                                    steps = 14,
                                    colors = SliderDefaults.colors(
                                        thumbColor = KvmGreen,
                                        activeTrackColor = KvmGreen,
                                        inactiveTrackColor = Slate700
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Virtual Disk Size
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Virtual Disk Size", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${diskSizeGb.toInt()} GB", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)
                                }
                                Text("Dynamic QCOW2 image expands as files are written. Available storage: ${hardware.freeStorageGb} GB", fontSize = 11.sp, color = Slate400)
                                Slider(
                                    value = diskSizeGb,
                                    onValueChange = { diskSizeGb = it },
                                    valueRange = 16f..256f,
                                    steps = 15,
                                    colors = SliderDefaults.colors(
                                        thumbColor = WindowsCyan,
                                        activeTrackColor = WindowsCyan,
                                        inactiveTrackColor = Slate700
                                    )
                                )
                            }
                        }
                    }

                    2 -> {
                        // Windows 11 Bypasses & VirtIO Tweaks
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Windows 11 Installer Bypasses", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)

                                SettingSwitchItem(
                                    title = "Bypass TPM 2.0 Requirement",
                                    subtitle = "Injects LabConfig BypassTPMCheck into Windows Setup registry",
                                    checked = bypassTpm,
                                    onCheckedChange = { bypassTpm = it }
                                )

                                SettingSwitchItem(
                                    title = "Bypass Secure Boot Check",
                                    subtitle = "Allows UEFI boot without signed Microsoft hardware certificate",
                                    checked = bypassSecureBoot,
                                    onCheckedChange = { bypassSecureBoot = it }
                                )

                                SettingSwitchItem(
                                    title = "Bypass 4GB RAM & CPU Check",
                                    subtitle = "Permits smooth installation on lower memory allocations",
                                    checked = bypassRamCheck,
                                    onCheckedChange = { bypassRamCheck = it }
                                )

                                SettingSwitchItem(
                                    title = "Bypass OOBE Microsoft Account",
                                    subtitle = "Executes oobe\\bypassnro for local offline account creation",
                                    checked = bypassOobeNetwork,
                                    onCheckedChange = { bypassOobeNetwork = it }
                                )

                                SettingSwitchItem(
                                    title = "Inject VirtIO Windows Drivers",
                                    subtitle = "Mounts virtio-win.iso with SCSI disk and VirtIO net adapters",
                                    checked = virtIoDriversEnabled,
                                    onCheckedChange = { virtIoDriversEnabled = it }
                                )
                            }
                        }
                    }

                    3 -> {
                        // Display, GPU & Networking
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Display & Virtual Graphics", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)

                                SettingSwitchItem(
                                    title = "Hardware KVM Virtualization",
                                    subtitle = if (hardware.isKvmAvailable) "Enabled via /dev/kvm" else "KVM node not present; JIT fallback",
                                    checked = useKvm,
                                    onCheckedChange = { useKvm = it }
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("GPU Acceleration Mode", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate300)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ChipOption(
                                        label = "Virgl 3D",
                                        isSelected = gpuMode == "VIRGL",
                                        onClick = { gpuMode = "VIRGL" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ChipOption(
                                        label = "VirtIO-GPU",
                                        isSelected = gpuMode == "VIRTIO_GPU",
                                        onClick = { gpuMode = "VIRTIO_GPU" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ChipOption(
                                        label = "Ramfb",
                                        isSelected = gpuMode == "RAMFB",
                                        onClick = { gpuMode = "RAMFB" },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Virtual Display Resolution", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate300)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ChipOption(
                                        label = "1600x900",
                                        isSelected = displayResolution == "1600x900",
                                        onClick = { displayResolution = "1600x900" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ChipOption(
                                        label = "1920x1080",
                                        isSelected = displayResolution == "1920x1080",
                                        onClick = { displayResolution = "1920x1080" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ChipOption(
                                        label = "1280x720",
                                        isSelected = displayResolution == "1280x720",
                                        onClick = { displayResolution = "1280x720" },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Virtual Networking", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate300)
                                Text("SLIRP User Network with Port Forwarding: RDP (3389 -> localhost:3389) and SSH (22 -> localhost:2222)", fontSize = 12.sp, color = Slate400)
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar (Next / Create)
            Surface(
                color = Slate900,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedTab > 0) {
                        Button(
                            onClick = { selectedTab-- },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Slate300),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (selectedTab < tabs.size - 1) {
                        Button(
                            onClick = { selectedTab++ },
                            colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_wizard_next")
                        ) {
                            Text("Next", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                val newVm = VirtualMachine(
                                    name = vmName.ifEmpty { "Windows 11 ARM64" },
                                    osType = selectedOsType,
                                    arch = "aarch64",
                                    cpuCores = cpuCores.toInt(),
                                    ramMb = ramMb.toInt(),
                                    diskSizeGb = diskSizeGb.toInt(),
                                    diskFormat = diskFormat,
                                    isoPath = isoUriString,
                                    isoName = isoFileName,
                                    isoSizeBytes = isoSizeBytes,
                                    useKvm = useKvm,
                                    bypassTpm = bypassTpm,
                                    bypassSecureBoot = bypassSecureBoot,
                                    bypassRamCheck = bypassRamCheck,
                                    bypassOobeNetwork = bypassOobeNetwork,
                                    virtIoDriversEnabled = virtIoDriversEnabled,
                                    displayResolution = displayResolution,
                                    gpuMode = gpuMode,
                                    audioDevice = audioDevice,
                                    networkMode = networkMode,
                                    portForwardRdp = portForwardRdp,
                                    osVersionDisplay = "Windows 11 Pro (ARM64 24H2)"
                                )
                                viewModel.saveVm(newVm) { id ->
                                    onVmCreated(newVm.copy(id = id))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KvmGreen, contentColor = Slate900),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_create_and_save_vm")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create VM", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OsPresetCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) WindowsCyan.copy(alpha = 0.15f) else Slate850)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) WindowsCyan else Slate700,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WindowsLogoMini(size = 18)
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WindowsCyan, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 11.sp, color = Slate400)
        }
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 11.sp, color = Slate400, lineHeight = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WindowsCyan,
                checkedTrackColor = Slate800,
                uncheckedThumbColor = Slate400,
                uncheckedTrackColor = Slate850
            )
        )
    }
}

@Composable
fun ChipOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) WindowsCyan.copy(alpha = 0.2f) else Slate850)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) WindowsCyan else Slate700,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) WindowsCyan else Slate300
        )
    }
}
