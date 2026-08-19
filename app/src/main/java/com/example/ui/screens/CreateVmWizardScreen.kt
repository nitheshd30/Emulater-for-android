package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VirtualMachine
import com.example.engine.IsoInspector
import com.example.ui.theme.KvmGreen
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WindowsBlue
import com.example.ui.theme.WindowsCyan
import com.example.ui.viewmodel.VmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVmWizardScreen(
    viewModel: VmViewModel,
    onNavigateBack: () -> Unit,
    onVmCreated: (VirtualMachine) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hardware by viewModel.hardwareInfo.collectAsStateWithLifecycle()
    val isoInspection by viewModel.currentIsoInspection.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("1. OS & Image", "2. Hardware", "3. Win11 Tweaks", "4. Display & Net")

    // Form state
    var vmName by remember { mutableStateOf("Windows 11 VM") }
    var selectedOsType by remember { mutableStateOf("WINDOWS_11_ARM") }
    var isoUriString by remember { mutableStateOf("") }
    var isoFileName by remember { mutableStateOf("") }
    var isoSizeBytes by remember { mutableStateOf(0L) }

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

    var showIsoDownloadDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    // SAF Document Picker for ISO
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {
                // Non-persistable URI fallback
            }
            isoUriString = uri.toString()
            viewModel.inspectIsoUri(uri)
        }
    }

    // React to ISO inspection updates
    androidx.compose.runtime.LaunchedEffect(isoInspection) {
        isoInspection?.let { insp ->
            isoFileName = insp.fileName
            isoSizeBytes = insp.fileSizeBytes
            if (insp.isArm64Iso) {
                selectedOsType = "WINDOWS_11_ARM"
                vmName = insp.fileName.substringBeforeLast(".").replace("_", " ").ifEmpty { "Windows 11 ARM64" }
            } else {
                selectedOsType = "WINDOWS_11_X64"
                vmName = insp.fileName.substringBeforeLast(".").replace("_", " ").ifEmpty { "Windows 11 x64" }
            }
        }
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
                actions = {
                    IconButton(onClick = { showIsoDownloadDialog = true }) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Get Windows ISO", tint = WindowsCyan)
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_vm_name"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WindowsCyan,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedContainerColor = Slate850,
                                        unfocusedContainerColor = Slate850
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Target Operating System Preset", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate300)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OsPresetCard(
                                        title = "Windows 11 ARM",
                                        subtitle = "ARM64 24H2 Native",
                                        isSelected = selectedOsType == "WINDOWS_11_ARM",
                                        onClick = {
                                            selectedOsType = "WINDOWS_11_ARM"
                                            vmName = "Windows 11 ARM64 Pro"
                                            isoFileName = "Windows_11_ARM64_Pro_24H2.iso"
                                            bypassTpm = true
                                            bypassSecureBoot = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OsPresetCard(
                                        title = "Tiny11 ARM",
                                        subtitle = "Lite 2GB RAM",
                                        isSelected = selectedOsType == "TINY11_ARM",
                                        onClick = {
                                            selectedOsType = "TINY11_ARM"
                                            vmName = "Tiny11 ARM64"
                                            isoFileName = "tiny11_arm64_lite.iso"
                                            ramMb = 2048f
                                            diskSizeGb = 32f
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
                                            isoFileName = "Windows_10_ARM64_21H2.iso"
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
                                        Text("Select custom Windows ISO from device storage", fontSize = 12.sp, color = Slate400)
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
                                val hasCustomIso = isoUriString.isNotBlank()
                                val isArm = isoInspection?.isArm64Iso ?: true
                                val effectiveName = if (isoFileName.isNotBlank()) isoFileName else if (hasCustomIso) "Custom Windows ISO" else "No ISO Selected"
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Slate850)
                                        .border(1.dp, if (hasCustomIso) KvmGreen.copy(alpha = 0.5f) else WindowsCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    imageVector = Icons.Default.Storage,
                                                    contentDescription = null,
                                                    tint = if (hasCustomIso) KvmGreen else WarningAmber,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = effectiveName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                            }

                                            // Architecture Badge
                                            Surface(
                                                color = if (hasCustomIso) {
                                                    if (isArm) KvmGreen.copy(alpha = 0.2f) else Color(0xFFFFB74D).copy(alpha = 0.2f)
                                                } else {
                                                    Slate700
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (hasCustomIso) (if (isArm) "ARM64 Native" else "x86_64 TCG") else "Optional",
                                                    color = if (hasCustomIso) (if (isArm) KvmGreen else Color(0xFFFFB74D)) else Slate300,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (isoInspection != null && hasCustomIso) {
                                                "Size: ${isoInspection!!.fileSizeFormatted} • ${isoInspection!!.detectedOs}\n${isoInspection!!.summaryNotes}"
                                            } else if (hasCustomIso) {
                                                "Custom disc image attached. Fully bootable with UEFI EDK2 & VirtIO drivers."
                                            } else {
                                                "Click 'Browse ISO' above to select your downloaded Windows 11/10 ISO file (.iso) from your phone storage."
                                            },
                                            fontSize = 11.sp,
                                            color = Slate400,
                                            lineHeight = 15.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { showDiagnosticsDialog = true },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WindowsCyan),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Boot Diagnostics", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { showIsoDownloadDialog = true },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Download ISO Guide", fontSize = 11.sp)
                                            }
                                        }
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

                                // Disk Size Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Virtual Disk Capacity", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${diskSizeGb.toInt()} GB", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WindowsBlue)
                                }
                                Text("Dynamic QCOW2 image expands as guest creates files", fontSize = 11.sp, color = Slate400)
                                Slider(
                                    value = diskSizeGb,
                                    onValueChange = { diskSizeGb = it },
                                    valueRange = 16f..256f,
                                    steps = 15,
                                    colors = SliderDefaults.colors(
                                        thumbColor = WindowsBlue,
                                        activeTrackColor = WindowsBlue,
                                        inactiveTrackColor = Slate700
                                    )
                                )
                            }
                        }

                        // KVM Hypervisor Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = if (useKvm) KvmGreen else Slate400,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("KVM Hardware Acceleration", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (hardware.isKvmAvailable) "Kernel /dev/kvm available. Enables 60 FPS near-native virtualization." else "KVM not detected. VM will run via QEMU TCG JIT compiler.",
                                        fontSize = 11.sp,
                                        color = Slate400
                                    )
                                }
                                Switch(
                                    checked = useKvm,
                                    onCheckedChange = { useKvm = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = KvmGreen, checkedTrackColor = Slate800)
                                )
                            }
                        }
                    }

                    2 -> {
                        // Windows 11 Tweaks Tab
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = WindowsCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Windows 11 Requirement Bypasses", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Text("Automatically applies registry LabConfig keys during boot so setup never halts.", fontSize = 11.sp, color = Slate400)

                                HorizontalDivider(color = Slate800)

                                ConfigToggleRow("Bypass TPM 2.0 Check", "Permits installation without hardware cryptoprocessor", bypassTpm) { bypassTpm = it }
                                ConfigToggleRow("Bypass Secure Boot", "Permits loading unsigned UEFI drivers and custom kernels", bypassSecureBoot) { bypassSecureBoot = it }
                                ConfigToggleRow("Bypass 4GB RAM Requirement", "Allows Windows 11 to boot on devices with 2GB to 4GB RAM", bypassRamCheck) { bypassRamCheck = it }
                                ConfigToggleRow("Bypass Microsoft Account (OOBE\\BYPASSNRO)", "Enables offline local account creation without internet login", bypassOobeNetwork) { bypassOobeNetwork = it }
                                ConfigToggleRow("VirtIO SCSI & Net Driver Injection", "Auto-loads Red Hat VirtIO storage drivers into WinPE", virtIoDriversEnabled) { virtIoDriversEnabled = it }
                            }
                        }
                    }

                    3 -> {
                        // Display & Network Tab
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(16.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DisplaySettings, contentDescription = null, tint = WindowsCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Display & GPU Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GpuOptionCard("VIRGL 3D", "OpenGL accel", gpuMode == "VIRGL") { gpuMode = "VIRGL" }
                                    GpuOptionCard("VirtIO GPU", "Standard 2D", gpuMode == "VIRTIO_GPU") { gpuMode = "VIRTIO_GPU" }
                                    GpuOptionCard("RAMFB", "UEFI Basic", gpuMode == "RAMFB") { gpuMode = "RAMFB" }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Slate800)
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Port Forwarding (Host to Guest)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate300)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("RDP Remote Desktop: Localhost:$portForwardRdp -> Guest 3389", fontSize = 11.sp, color = Slate400)
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
                                val detectedArch = if (isoInspection?.isArm64Iso == false || selectedOsType.contains("X64")) "x86_64" else "aarch64"
                                val finalIsoName = when {
                                    isoFileName.isNotBlank() -> isoFileName
                                    isoUriString.isNotBlank() -> isoUriString.substringAfterLast("/")
                                    else -> ""
                                }
                                val newVm = VirtualMachine(
                                    name = vmName.ifEmpty { if (detectedArch == "aarch64") "Windows 11 ARM64" else "Windows 11 x64" },
                                    osType = selectedOsType,
                                    arch = detectedArch,
                                    cpuCores = cpuCores.toInt(),
                                    ramMb = ramMb.toInt(),
                                    diskSizeGb = diskSizeGb.toInt(),
                                    diskFormat = diskFormat,
                                    isoPath = isoUriString,
                                    isoName = finalIsoName,
                                    isoSizeBytes = isoSizeBytes,
                                    useKvm = useKvm && detectedArch == "aarch64",
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
                                    osVersionDisplay = if (detectedArch == "aarch64") "Windows 11 Pro (ARM64)" else "Windows 11 Pro (x86_64 TCG)"
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

    // Official ISO Downloader Dialog
    if (showIsoDownloadDialog) {
        val sources = IsoInspector.getOfficialIsoSources()
        AlertDialog(
            onDismissRequest = { showIsoDownloadDialog = false },
            containerColor = Slate900,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = WindowsCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get Windows 11 ARM64 ISO", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "For high-speed 60 FPS emulation on Android ARM64 devices, a genuine Windows on ARM (ARM64) ISO is required. Choose a source below to download:",
                        fontSize = 12.sp,
                        color = Slate300,
                        lineHeight = 16.sp
                    )

                    sources.forEach { source ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {}
                                },
                            colors = CardDefaults.cardColors(containerColor = Slate850),
                            shape = RoundedCornerShape(10.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(source.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)
                                    Surface(
                                        color = if (source.isRecommended) KvmGreen.copy(alpha = 0.2f) else Slate800,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = source.tag,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (source.isRecommended) KvmGreen else Slate400,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(source.description, fontSize = 11.sp, color = Slate400, lineHeight = 14.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = WindowsCyan, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open in Browser: ${source.url}", fontSize = 10.sp, color = WindowsCyan, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIsoDownloadDialog = false }) {
                    Text("Done", color = WindowsCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Diagnostics Dialog
    if (showDiagnosticsDialog) {
        val inspection = isoInspection ?: IsoInspector.analyzeIso(isoFileName, isoSizeBytes)
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            containerColor = Slate900,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = KvmGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ISO Bootloader Inspection", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    inspection.diagnosticDetails.forEach { detail ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate850, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = detail,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (detail.startsWith("⚠️")) Color(0xFFFFB74D) else Slate300
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "LabConfig Bypass Table Status: Injecting TPM 2.0 / SecureBoot bypass into guest registry memory.",
                        fontSize = 11.sp,
                        color = KvmGreen,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnosticsDialog = false }) {
                    Text("Close", color = WindowsCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
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
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) WindowsCyan.copy(alpha = 0.15f) else Slate850)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) WindowsCyan else Slate700,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) WindowsCyan else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Slate400
            )
        }
    }
}

@Composable
fun ConfigToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(description, fontSize = 10.sp, color = Slate400, lineHeight = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = WindowsCyan, checkedTrackColor = Slate800)
        )
    }
}

@Composable
fun GpuOptionCard(
    title: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) WindowsCyan.copy(alpha = 0.15f) else Slate850)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) WindowsCyan else Slate700,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) WindowsCyan else MaterialTheme.colorScheme.onSurface)
            Text(desc, fontSize = 9.sp, color = Slate400)
        }
    }
}

@Composable
fun WindowsLogoMini(size: Int) {
    Box(modifier = Modifier.size(size.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0xFF00ADEF)))
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0xFF00ADEF)))
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0xFF00ADEF)))
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0xFF00ADEF)))
            }
        }
    }
}
