package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VirtualMachine
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
    var isoName by remember { mutableStateOf(vm.isoName) }
    var bypassTpm by remember { mutableStateOf(vm.bypassTpm) }
    var bypassSecureBoot by remember { mutableStateOf(vm.bypassSecureBoot) }
    var bypassRamCheck by remember { mutableStateOf(vm.bypassRamCheck) }
    var useKvm by remember { mutableStateOf(vm.useKvm) }

    var showSnapshotDialog by remember { mutableStateOf(false) }
    var snapshotTitle by remember { mutableStateOf("") }
    var showQemuCliDialog by remember { mutableStateOf(false) }

    val qemuCommand = remember(vm, cpuCores, ramMb, useKvm, isoName) {
        val updated = vm.copy(
            cpuCores = cpuCores.toInt(),
            ramMb = ramMb.toInt(),
            useKvm = useKvm,
            isoName = isoName
        )
        QemuCommandBuilder.buildCommandLineString(updated)
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
                    Column {
                        Text(vm.osVersionDisplay, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Architecture: ${vm.arch} (ARM64)", fontSize = 12.sp, color = Slate400)
                    }
                    Button(
                        onClick = {
                            val updated = vm.copy(
                                name = name,
                                cpuCores = cpuCores.toInt(),
                                ramMb = ramMb.toInt(),
                                isoName = isoName,
                                bypassTpm = bypassTpm,
                                bypassSecureBoot = bypassSecureBoot,
                                bypassRamCheck = bypassRamCheck,
                                useKvm = useKvm
                            )
                            viewModel.saveVm(updated)
                            onLaunchVm(updated)
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

            // General & Hardware Config
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Machine Configuration", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WindowsCyan,
                            unfocusedBorderColor = Slate700,
                            focusedContainerColor = Slate850,
                            unfocusedContainerColor = Slate850
                        )
                    )

                    // CPU Slider
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
                    }

                    // Virtual Storage & CD-ROM ISO
                    Text("Storage & Drives", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate300)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate850)
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = WindowsCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Virtual Hard Drive: ${vm.diskSizeGb} GB (${vm.diskFormat.uppercase()})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("CD-ROM: ${if (isoName.isNotEmpty()) isoName else "No ISO mounted"}", fontSize = 12.sp, color = Slate400)
                        }
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

            // Save Changes Button
            Button(
                onClick = {
                    val updated = vm.copy(
                        name = name,
                        cpuCores = cpuCores.toInt(),
                        ramMb = ramMb.toInt(),
                        isoName = isoName,
                        bypassTpm = bypassTpm,
                        bypassSecureBoot = bypassSecureBoot,
                        bypassRamCheck = bypassRamCheck,
                        useKvm = useKvm
                    )
                    viewModel.saveVm(updated)
                    Toast.makeText(context, "Configuration saved!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("btn_save_vm_changes")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Configuration", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Snapshot Dialog
    if (showSnapshotDialog) {
        AlertDialog(
            onDismissRequest = { showSnapshotDialog = false },
            title = { Text("Create VM Snapshot") },
            text = {
                Column {
                    Text("Enter a label for this virtual machine restore point:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = snapshotTitle,
                        onValueChange = { snapshotTitle = it },
                        placeholder = { Text("e.g. Fresh Windows 11 Install") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.takeSnapshot(vm.id, snapshotTitle)
                    showSnapshotDialog = false
                    snapshotTitle = ""
                }) {
                    Text("Save Snapshot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnapshotDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // QEMU CLI / Termux Script Dialog
    if (showQemuCliDialog) {
        AlertDialog(
            onDismissRequest = { showQemuCliDialog = false },
            title = { Text("QEMU ARM64 Command Line") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("You can run this virtual machine via Termux, Shizuku, or native Linux shell on Android with full KVM acceleration:", fontSize = 12.sp, color = Slate300)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate850)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = qemuCommand,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = WindowsCyan
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clipMan = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipMan.setPrimaryClip(ClipData.newPlainText("QEMU Command", qemuCommand))
                    Toast.makeText(context, "QEMU command copied to clipboard!", Toast.LENGTH_SHORT).show()
                    showQemuCliDialog = false
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy CLI")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQemuCliDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
