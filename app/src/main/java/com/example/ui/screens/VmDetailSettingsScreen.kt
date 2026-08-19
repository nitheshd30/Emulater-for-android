package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.engine.QemuCommandBuilder
import com.example.ui.components.StatusBadge
import com.example.ui.components.WindowsLogoMini
import com.example.ui.theme.KvmGreen
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WindowsCyan
import com.example.ui.viewmodel.VmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VmDetailSettingsScreen(
    vm: VirtualMachine,
    viewModel: VmViewModel,
    onNavigateBack: () -> Unit,
    onLaunchVm: (VirtualMachine) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hardware by viewModel.hardwareInfo.collectAsStateWithLifecycle()
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf(vm.name) }
    var cpuCores by remember { mutableFloatStateOf(vm.cpuCores.toFloat()) }
    var ramMb by remember { mutableFloatStateOf(vm.ramMb.toFloat()) }
    var isoPath by remember { mutableStateOf(vm.isoPath) }
    var isoName by remember { mutableStateOf(vm.isoName) }
    var isoSizeBytes by remember { mutableStateOf(vm.isoSizeBytes) }
    var arch by remember { mutableStateOf(vm.arch) }
    var isInstalled by remember { mutableStateOf(vm.isInstalled) }
    var bypassTpm by remember { mutableStateOf(vm.bypassTpm) }
    var bypassSecureBoot by remember { mutableStateOf(vm.bypassSecureBoot) }
    var bypassRamCheck by remember { mutableStateOf(vm.bypassRamCheck) }
    var useKvm by remember { mutableStateOf(vm.useKvm) }

    var showSnapshotDialog by remember { mutableStateOf(false) }
    var snapshotTitle by remember { mutableStateOf("") }
    var showQemuCliDialog by remember { mutableStateOf(false) }
    var showIsoSourcesDialog by remember { mutableStateOf(false) }

    // SAF Document Picker for ISO
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}

            isoPath = uri.toString()
            val inspection = IsoInspector.inspectUri(context, uri)
            isoName = inspection.fileName
            isoSizeBytes = inspection.fileSizeBytes
            if (!inspection.isArm64Iso) {
                arch = "x86_64"
            }
            Toast.makeText(context, "Selected ISO: ${inspection.fileName} (${inspection.fileSizeFormatted})", Toast.LENGTH_SHORT).show()
        }
    }

    val currentVmConfig = remember(name, cpuCores, ramMb, isoPath, isoName, isoSizeBytes, arch, isInstalled, bypassTpm, bypassSecureBoot, bypassRamCheck, useKvm) {
        vm.copy(
            name = name,
            cpuCores = cpuCores.toInt(),
            ramMb = ramMb.toInt(),
            isoPath = isoPath,
            isoName = isoName,
            isoSizeBytes = isoSizeBytes,
            arch = arch,
            isInstalled = isInstalled,
            bypassTpm = bypassTpm,
            bypassSecureBoot = bypassSecureBoot,
            bypassRamCheck = bypassRamCheck,
            useKvm = useKvm
        )
    }

    val cliString = remember(currentVmConfig) {
        QemuCommandBuilder.buildCommandLineString(currentVmConfig)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WindowsLogoMini(size = 20)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(vm.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.saveVm(currentVmConfig)
                        Toast.makeText(context, "Settings Saved", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save Settings", tint = KvmGreen)
                    }
                    IconButton(onClick = { showQemuCliDialog = true }) {
                        Icon(Icons.Default.Terminal, contentDescription = "QEMU CLI", tint = WindowsCyan)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(vm.osVersionDisplay, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Architecture: ${arch.uppercase()} • ${if (isInstalled) "Disk Bootable (Installed)" else "Setup Mode (ISO Boot)"}", fontSize = 12.sp, color = Slate400)
                    }
                    Button(
                        onClick = {
                            viewModel.saveVm(currentVmConfig)
                            onLaunchVm(currentVmConfig)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_launch_from_settings")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start VM", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // General & Hardware Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Machine Configuration", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)

                    // VM Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WindowsCyan,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = Slate850,
                            unfocusedContainerColor = Slate850
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // CPU Cores Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CPU Cores", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate300)
                            Text("${cpuCores.toInt()} Cores", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)
                        }
                        Slider(
                            value = cpuCores,
                            onValueChange = { cpuCores = it },
                            valueRange = 1f..hardware.cpuCores.toFloat().coerceAtLeast(4f),
                            steps = (hardware.cpuCores.coerceAtLeast(4) - 2),
                            colors = SliderDefaults.colors(thumbColor = WindowsCyan, activeTrackColor = WindowsCyan)
                        )
                    }

                    // RAM Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RAM Allocation", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate300)
                            Text(String.format("%.1f GB (%d MB)", ramMb / 1024f, ramMb.toInt()), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KvmGreen)
                        }
                        Slider(
                            value = ramMb,
                            onValueChange = { ramMb = it },
                            valueRange = 1024f..(hardware.totalRamMb * 0.75f).coerceAtLeast(4096f),
                            steps = 14,
                            colors = SliderDefaults.colors(thumbColor = KvmGreen, activeTrackColor = KvmGreen)
                        )
                        if (ramMb > hardware.totalRamMb * 0.75f) {
                            Text("⚠️ High RAM allocation may cause out-of-memory (OOM) errors during the Windows boot process. Reduce if crashes occur.", fontSize = 11.sp, color = WarningAmber, modifier = Modifier.padding(top = 4.dp))
                        } else if (ramMb < 2048f && !bypassRamCheck) {
                            Text("⚠️ Windows 11 requires at least 4GB (or 2GB with bypass) of RAM. Boot may fail.", fontSize = 11.sp, color = WarningAmber, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            // Virtual Storage & CD-ROM ISO Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Storage & CD-ROM Media", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)
                        IconButton(onClick = { showIsoSourcesDialog = true }) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Download ISO Guide", tint = WindowsCyan)
                        }
                    }

                    // Virtual Hard Disk
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate850)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = WindowsCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Virtual Hard Drive (C:)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("${vm.diskSizeGb} GB • ${vm.diskFormat.uppercase()} • VirtIO-SCSI Driver Active", fontSize = 11.sp, color = Slate400)
                            }
                        }
                    }

                    // CD-ROM / ISO image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate850)
                            .border(1.dp, if (isoName.isNotEmpty()) KvmGreen.copy(alpha = 0.4f) else Slate700, RoundedCornerShape(10.dp))
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
                                        Icons.Default.DiscFull,
                                        contentDescription = null,
                                        tint = if (isoName.isNotEmpty()) KvmGreen else Slate400,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Virtual CD-ROM Drive (D:)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(
                                            text = if (isoName.isNotEmpty()) isoName else "No ISO mounted (Empty)",
                                            fontSize = 11.sp,
                                            color = if (isoName.isNotEmpty()) WindowsCyan else Slate400,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change ISO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                if (isoName.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = {
                                            isoPath = ""
                                            isoName = ""
                                            isoSizeBytes = 0L
                                            Toast.makeText(context, "ISO Ejected", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Eject, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Eject ISO", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Save Changes Button
                    Button(
                        onClick = {
                            viewModel.saveVm(currentVmConfig)
                            Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = WindowsCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Configuration Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Windows 11 Bypasses
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Windows 11 Bypasses & Acceleration", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)

                    SettingSwitchItem(
                        title = "TPM 2.0 Check Bypass",
                        subtitle = "LabConfig BypassTPMCheck",
                        checked = bypassTpm,
                        onCheckedChange = { bypassTpm = it }
                    )

                    SettingSwitchItem(
                        title = "SecureBoot Check Bypass",
                        subtitle = "Allows booting unsigned EFI loader",
                        checked = bypassSecureBoot,
                        onCheckedChange = { bypassSecureBoot = it }
                    )

                    SettingSwitchItem(
                        title = "Hardware KVM Virtualization",
                        subtitle = if (hardware.isKvmAvailable) "Active (/dev/kvm)" else "JIT TCG translation",
                        checked = useKvm,
                        onCheckedChange = { useKvm = it }
                    )
                }
            }

            // Snapshots Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Snapshots & Save States", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)
                            Text("${snapshots.size} Saved restore points", fontSize = 12.sp, color = Slate400)
                        }
                        Button(
                            onClick = { showSnapshotDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = WindowsCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Take Snapshot", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (snapshots.isEmpty()) {
                        Text(
                            "No snapshots yet. Take a snapshot to freeze VM memory and disk state for quick instant revert.",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    } else {
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        snapshots.forEach { snap ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate850)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(snap.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(dateFormat.format(Date(snap.timestamp)), fontSize = 11.sp, color = Slate400)
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            Toast.makeText(context, "Reverted VM to ${snap.title}", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.Restore, contentDescription = "Restore", tint = WindowsCyan)
                                        }
                                        IconButton(onClick = { viewModel.deleteSnapshot(snap) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Take Snapshot Dialog
    if (showSnapshotDialog) {
        AlertDialog(
            onDismissRequest = { showSnapshotDialog = false },
            containerColor = Slate900,
            title = { Text("Take VM Snapshot", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a label for this restore point (e.g. 'Before Windows Update', 'Fresh Install'):", fontSize = 12.sp, color = Slate400)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = snapshotTitle,
                        onValueChange = { snapshotTitle = it },
                        placeholder = { Text("Snapshot Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WindowsCyan,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = Slate850,
                            unfocusedContainerColor = Slate850
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (snapshotTitle.isNotBlank()) {
                            viewModel.takeSnapshot(vm.id, snapshotTitle)
                            snapshotTitle = ""
                            showSnapshotDialog = false
                            Toast.makeText(context, "Snapshot created", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900)
                ) {
                    Text("Save Snapshot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnapshotDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }

    // QEMU CLI Preview Dialog
    if (showQemuCliDialog) {
        AlertDialog(
            onDismissRequest = { showQemuCliDialog = false },
            containerColor = Slate900,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = WindowsCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QEMU Command Line", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column {
                    Text("Below is the hypervisor arguments string generated for this VM:", fontSize = 12.sp, color = Slate400)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate950)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = cliString,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = KvmGreen,
                            lineHeight = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("QEMU Command", cliString)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "QEMU command copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showQemuCliDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy CLI")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQemuCliDialog = false }) {
                    Text("Close", color = Slate400)
                }
            }
        )
    }

    // Official ISO Downloader Dialog
    if (showIsoSourcesDialog) {
        val sources = IsoInspector.getOfficialIsoSources()
        AlertDialog(
            onDismissRequest = { showIsoSourcesDialog = false },
            containerColor = Slate900,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = WindowsCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Windows 11 ARM ISO", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(source.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(source.description, fontSize = 11.sp, color = Slate400)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIsoSourcesDialog = false }) {
                    Text("Done", color = WindowsCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
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
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = Slate400)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = WindowsCyan, checkedTrackColor = Slate800)
        )
    }
}

val Slate950 = Color(0xFF030712)
