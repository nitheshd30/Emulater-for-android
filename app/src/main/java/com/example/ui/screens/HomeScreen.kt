package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VirtualMachine
import com.example.engine.DeviceHardwareInfo
import com.example.engine.VmBootState
import com.example.ui.components.KvmChip
import com.example.ui.components.StatusBadge
import com.example.ui.components.WindowsLogoMini
import com.example.ui.theme.ErrorRose
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

enum class VmStatusFilter {
    ALL,
    RUNNING,
    SUSPENDED,
    STOPPED
}

@Composable
fun HomeScreen(
    viewModel: VmViewModel,
    onNavigateCreate: () -> Unit,
    onNavigateDetail: (VirtualMachine) -> Unit,
    onNavigateRunner: (VirtualMachine) -> Unit,
    onNavigateGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vms by viewModel.allVms.collectAsStateWithLifecycle()
    val hardware by viewModel.hardwareInfo.collectAsStateWithLifecycle()
    val runtimeState by viewModel.runtimeState.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(VmStatusFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var vmToDelete by remember { mutableStateOf<VirtualMachine?>(null) }
    var vmForSnapshot by remember { mutableStateOf<VirtualMachine?>(null) }
    var snapshotTitleInput by remember { mutableStateOf("") }

    // Compute effective status for each VM considering active runtimeState
    fun getEffectiveStatus(vm: VirtualMachine): String {
        return if (runtimeState.vm?.id == vm.id) {
            when (runtimeState.bootState) {
                VmBootState.OFF -> "STOPPED"
                VmBootState.PAUSED -> "SUSPENDED"
                else -> "RUNNING"
            }
        } else {
            when (vm.status.uppercase()) {
                "RUNNING" -> "RUNNING"
                "SUSPENDED", "PAUSED" -> "SUSPENDED"
                else -> "STOPPED"
            }
        }
    }

    val runningCount = vms.count { getEffectiveStatus(it) == "RUNNING" }
    val suspendedCount = vms.count { getEffectiveStatus(it) == "SUSPENDED" }
    val stoppedCount = vms.count { getEffectiveStatus(it) == "STOPPED" }

    val filteredVms = vms.filter { vm ->
        val status = getEffectiveStatus(vm)
        val matchesFilter = when (selectedFilter) {
            VmStatusFilter.ALL -> true
            VmStatusFilter.RUNNING -> status == "RUNNING"
            VmStatusFilter.SUSPENDED -> status == "SUSPENDED"
            VmStatusFilter.STOPPED -> status == "STOPPED"
        }
        val matchesSearch = if (searchQuery.isBlank()) true else {
            vm.name.contains(searchQuery, ignoreCase = true) ||
                    vm.osVersionDisplay.contains(searchQuery, ignoreCase = true) ||
                    vm.isoName.contains(searchQuery, ignoreCase = true)
        }
        matchesFilter && matchesSearch
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Dashboard Header & Top Controls
            item {
                DashboardHeader(
                    onNavigateGuide = onNavigateGuide,
                    isSearchActive = isSearchActive,
                    onToggleSearch = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    }
                )
            }

            // Search Bar (if active)
            if (isSearchActive) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vm_search_input"),
                        placeholder = { Text("Filter by VM name, OS, or ISO...", fontSize = 13.sp, color = Slate400) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = WindowsCyan)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WindowsCyan,
                            unfocusedBorderColor = Slate700,
                            focusedContainerColor = Slate900,
                            unfocusedContainerColor = Slate900
                        )
                    )
                }
            }

            // Hardware & Hypervisor Telemetry Summary Card
            item {
                HardwareBanner(
                    hardware = hardware,
                    onRefresh = { viewModel.refreshHardware() }
                )
            }

            // Status Dashboard Metrics Cards
            item {
                VmMetricsDashboardRow(
                    totalVms = vms.size,
                    runningCount = runningCount,
                    suspendedCount = suspendedCount,
                    stoppedCount = stoppedCount,
                    selectedFilter = selectedFilter,
                    onSelectFilter = { selectedFilter = it }
                )
            }

            // Active VM Live Banner (Quick jump to running VM display)
            if (runtimeState.vm != null && runtimeState.bootState != VmBootState.OFF) {
                item {
                    ActiveRunningBanner(
                        vm = runtimeState.vm!!,
                        bootState = runtimeState.bootState,
                        telemetry = runtimeState.telemetry,
                        onOpen = { onNavigateRunner(runtimeState.vm!!) },
                        onPause = { viewModel.suspendVm(runtimeState.vm!!) },
                        onResume = { viewModel.resumeVm(runtimeState.vm!!) },
                        onStop = { viewModel.stopActiveVm() }
                    )
                }
            }

            // Section Header & Status Filter Pills
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VIRTUAL MACHINES (${filteredVms.size}/${vms.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )

                        if (runningCount > 0) {
                            TextButton(
                                onClick = { viewModel.stopActiveVm() },
                                modifier = Modifier.testTag("btn_stop_all")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = ErrorRose, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop Active VM", fontSize = 12.sp, color = ErrorRose, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Tabs Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            StatusFilterChip(
                                title = "All",
                                count = vms.size,
                                isSelected = selectedFilter == VmStatusFilter.ALL,
                                badgeColor = WindowsCyan,
                                onClick = { selectedFilter = VmStatusFilter.ALL }
                            )
                        }
                        item {
                            StatusFilterChip(
                                title = "Running",
                                count = runningCount,
                                isSelected = selectedFilter == VmStatusFilter.RUNNING,
                                badgeColor = KvmGreen,
                                onClick = { selectedFilter = VmStatusFilter.RUNNING }
                            )
                        }
                        item {
                            StatusFilterChip(
                                title = "Suspended",
                                count = suspendedCount,
                                isSelected = selectedFilter == VmStatusFilter.SUSPENDED,
                                badgeColor = WarningAmber,
                                onClick = { selectedFilter = VmStatusFilter.SUSPENDED }
                            )
                        }
                        item {
                            StatusFilterChip(
                                title = "Stopped",
                                count = stoppedCount,
                                isSelected = selectedFilter == VmStatusFilter.STOPPED,
                                badgeColor = Slate400,
                                onClick = { selectedFilter = VmStatusFilter.STOPPED }
                            )
                        }
                    }
                }
            }

            // Virtual Machine List Items
            if (filteredVms.isEmpty()) {
                item {
                    if (vms.isEmpty()) {
                        EmptyVmState(
                            onCreateClick = onNavigateCreate,
                            onTemplateClick = { template -> viewModel.createQuickTemplate(template) }
                        )
                    } else {
                        NoFilterMatchCard(
                            currentFilter = selectedFilter.name,
                            onResetFilter = {
                                selectedFilter = VmStatusFilter.ALL
                                searchQuery = ""
                            }
                        )
                    }
                }
            } else {
                items(filteredVms, key = { it.id }) { vm ->
                    val effectiveStatus = getEffectiveStatus(vm)
                    val isLiveActive = runtimeState.vm?.id == vm.id && runtimeState.bootState != VmBootState.OFF

                    VmCard(
                        vm = vm,
                        status = effectiveStatus,
                        isLiveActive = isLiveActive,
                        onStart = {
                            viewModel.launchVm(vm)
                            onNavigateRunner(vm)
                        },
                        onResume = {
                            viewModel.resumeVm(vm)
                            onNavigateRunner(vm)
                        },
                        onSuspend = {
                            viewModel.suspendVm(vm)
                        },
                        onStop = {
                            viewModel.stopVm(vm)
                        },
                        onRestart = {
                            viewModel.restartVm(vm)
                            onNavigateRunner(vm)
                        },
                        onOpenDisplay = {
                            onNavigateRunner(vm)
                        },
                        onSettings = {
                            viewModel.selectVmForEdit(vm)
                            onNavigateDetail(vm)
                        },
                        onTakeSnapshot = {
                            vmForSnapshot = vm
                            snapshotTitleInput = "${vm.name} - ${System.currentTimeMillis() % 10000}"
                        },
                        onDuplicate = { viewModel.duplicateVm(vm) },
                        onDelete = { vmToDelete = vm }
                    )
                }
            }
        }

        // Floating Action Button to Create New VM
        FloatingActionButton(
            onClick = onNavigateCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("btn_create_new_vm"),
            containerColor = WindowsCyan,
            contentColor = Slate900
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Virtual Machine")
                Spacer(modifier = Modifier.width(6.dp))
                Text("New VM", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Delete Confirmation Dialog
    if (vmToDelete != null) {
        AlertDialog(
            onDismissRequest = { vmToDelete = null },
            title = { Text("Delete Virtual Machine", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "Are you sure you want to delete '${vmToDelete?.name}' and all associated virtual disk data?",
                    color = Slate300
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vmToDelete?.let { viewModel.deleteVm(it) }
                        vmToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { vmToDelete = null }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    // Snapshot Modal Dialog
    if (vmForSnapshot != null) {
        AlertDialog(
            onDismissRequest = { vmForSnapshot = null },
            title = { Text("Take VM Snapshot", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text(
                        "Capture a point-in-time state of '${vmForSnapshot?.name}' including virtual disk and RAM state.",
                        fontSize = 13.sp,
                        color = Slate400
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = snapshotTitleInput,
                        onValueChange = { snapshotTitleInput = it },
                        label = { Text("Snapshot Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WindowsCyan,
                            unfocusedBorderColor = Slate700
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vmForSnapshot?.let {
                            viewModel.takeSnapshot(it.id, snapshotTitleInput)
                        }
                        vmForSnapshot = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900)
                ) {
                    Text("Save Snapshot", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vmForSnapshot = null }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
fun DashboardHeader(
    onNavigateGuide: () -> Unit,
    isSearchActive: Boolean,
    onToggleSearch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WindowsLogoMini(size = 32)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "WinDroid",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Windows 11 ARM VM Manager & Hypervisor",
                    fontSize = 12.sp,
                    color = Slate400
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier.testTag("btn_search_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search VMs",
                    tint = if (isSearchActive) WindowsCyan else Slate400
                )
            }
            IconButton(
                onClick = onNavigateGuide,
                modifier = Modifier.testTag("btn_open_guide")
            ) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = "Setup Guide & ISO Hub",
                    tint = WindowsCyan
                )
            }
        }
    }
}

@Composable
fun VmMetricsDashboardRow(
    totalVms: Int,
    runningCount: Int,
    suspendedCount: Int,
    stoppedCount: Int,
    selectedFilter: VmStatusFilter,
    onSelectFilter: (VmStatusFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricSummaryCard(
            title = "TOTAL",
            count = totalVms,
            label = "Virtual Machines",
            accentColor = WindowsCyan,
            isSelected = selectedFilter == VmStatusFilter.ALL,
            modifier = Modifier.weight(1f),
            onClick = { onSelectFilter(VmStatusFilter.ALL) }
        )

        MetricSummaryCard(
            title = "RUNNING",
            count = runningCount,
            label = "Active Sessions",
            accentColor = KvmGreen,
            isSelected = selectedFilter == VmStatusFilter.RUNNING,
            modifier = Modifier.weight(1f),
            onClick = { onSelectFilter(VmStatusFilter.RUNNING) }
        )

        MetricSummaryCard(
            title = "SUSPENDED",
            count = suspendedCount,
            label = "Paused RAM",
            accentColor = WarningAmber,
            isSelected = selectedFilter == VmStatusFilter.SUSPENDED,
            modifier = Modifier.weight(1f),
            onClick = { onSelectFilter(VmStatusFilter.SUSPENDED) }
        )

        MetricSummaryCard(
            title = "STOPPED",
            count = stoppedCount,
            label = "Powered Off",
            accentColor = Slate400,
            isSelected = selectedFilter == VmStatusFilter.STOPPED,
            modifier = Modifier.weight(1f),
            onClick = { onSelectFilter(VmStatusFilter.STOPPED) }
        )
    }
}

@Composable
fun MetricSummaryCard(
    title: String,
    count: Int,
    label: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Slate800,
        label = "border_color"
    )

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("metric_card_${title.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Slate850 else Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(borderColor, borderColor)))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else Slate400,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                color = Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusFilterChip(
    title: String,
    count: Int,
    isSelected: Boolean,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) badgeColor.copy(alpha = 0.2f) else Slate900,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    if (isSelected) badgeColor else Slate800,
                    if (isSelected) badgeColor else Slate800
                )
            )
        ),
        modifier = Modifier.testTag("filter_chip_${title.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$title ($count)",
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) badgeColor else Slate300
            )
        }
    }
}

@Composable
fun HardwareBanner(
    hardware: DeviceHardwareInfo,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (hardware.isKvmAvailable) KvmGreen.copy(alpha = 0.2f) else WindowsCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = if (hardware.isKvmAvailable) KvmGreen else WindowsCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = hardware.modelName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${hardware.primaryAbi.uppercase()} • ${hardware.cpuCores} Cores @ Hardware Level",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }
                KvmChip(isKvm = hardware.isKvmAvailable)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate850)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Device RAM", fontSize = 10.sp, color = Slate400)
                    Text(
                        text = String.format("%.1f GB", hardware.totalRamMb / 1024f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text("Available Free RAM", fontSize = 10.sp, color = Slate400)
                    Text(
                        text = String.format("%.1f GB", hardware.availableRamMb / 1024f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WindowsCyan
                    )
                }
                Column {
                    Text("Recommended VM RAM", fontSize = 10.sp, color = Slate400)
                    Text(
                        text = "${hardware.recommendedVmRamMb / 1024} GB",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KvmGreen
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveRunningBanner(
    vm: VirtualMachine,
    bootState: VmBootState,
    telemetry: com.example.engine.VmTelemetry,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate850),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (bootState == VmBootState.PAUSED) listOf(WarningAmber, Slate700) else listOf(KvmGreen, WindowsCyan)
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                (if (bootState == VmBootState.PAUSED) WarningAmber else KvmGreen).copy(
                                    alpha = if (bootState == VmBootState.PAUSED) 1f else alpha
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Live Active: ${vm.name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (bootState == VmBootState.PAUSED) "Suspended • Execution Frozen in RAM" else "Running • ${telemetry.fps} FPS • CPU ${telemetry.cpuUsagePercent}%",
                            fontSize = 11.sp,
                            color = if (bootState == VmBootState.PAUSED) WarningAmber else WindowsCyan
                        )
                    }
                }

                StatusBadge(status = if (bootState == VmBootState.PAUSED) "SUSPENDED" else "RUNNING")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Control Action Buttons for the Active VM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1.3f).testTag("btn_banner_open_display"),
                    colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Outlined.Laptop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Display", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (bootState == VmBootState.PAUSED) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.weight(1f).testTag("btn_banner_resume"),
                        colors = ButtonDefaults.buttonColors(containerColor = KvmGreen, contentColor = Slate900),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier.weight(1f).testTag("btn_banner_suspend"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Suspend", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f).testTag("btn_banner_stop"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun VmCard(
    vm: VirtualMachine,
    status: String,
    isLiveActive: Boolean,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onSuspend: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onOpenDisplay: () -> Unit,
    onSettings: () -> Unit,
    onTakeSnapshot: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val isRunning = status.equals("RUNNING", ignoreCase = true)
    val isSuspended = status.equals("SUSPENDED", ignoreCase = true) || status.equals("PAUSED", ignoreCase = true)
    val isStopped = !isRunning && !isSuspended

    val cardBorderColor = when {
        isRunning -> KvmGreen.copy(alpha = 0.5f)
        isSuspended -> WarningAmber.copy(alpha = 0.4f)
        else -> Slate800
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("vm_card_${vm.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(cardBorderColor, Slate800)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title, OS Display & Status Badge + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    WindowsLogoMini(size = 24)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = vm.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = vm.osVersionDisplay,
                            fontSize = 11.sp,
                            color = Slate400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = status)
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Slate400)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(Slate850)
                        ) {
                            DropdownMenuItem(
                                text = { Text("VM Settings & Hardware", color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = WindowsCyan) },
                                onClick = {
                                    menuExpanded = false
                                    onSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Take Snapshot", color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = KvmGreen) },
                                onClick = {
                                    menuExpanded = false
                                    onTakeSnapshot()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate VM", color = MaterialTheme.colorScheme.onSurface) },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = Slate300) },
                                onClick = {
                                    menuExpanded = false
                                    onDuplicate()
                                }
                            )
                            if (isRunning || isSuspended) {
                                DropdownMenuItem(
                                    text = { Text("Restart Machine", color = WarningAmber) },
                                    leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = WarningAmber) },
                                    onClick = {
                                        menuExpanded = false
                                        onRestart()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete Virtual Machine", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specs badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpecBadge(label = "CPU", value = "${vm.cpuCores}c")
                SpecBadge(label = "RAM", value = "${vm.ramMb / 1024} GB")
                SpecBadge(label = "Disk", value = "${vm.diskSizeGb}GB")
                SpecBadge(label = "GPU", value = vm.gpuMode)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ISO / Drive info pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate850)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = WindowsCyan,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (vm.isoName.isNotEmpty()) "ISO: ${vm.isoName}" else "Drive only (No ISO attached)",
                    fontSize = 11.sp,
                    color = Slate300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // QUICK-ACTION BUTTONS ROW (Status-Aware)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    isRunning -> {
                        // Action 1: Open Live Console
                        Button(
                            onClick = onOpenDisplay,
                            modifier = Modifier.weight(1.2f).testTag("btn_console_vm_${vm.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = KvmGreen, contentColor = Slate900),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.Laptop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Console", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Action 2: Suspend VM
                        OutlinedButton(
                            onClick = onSuspend,
                            modifier = Modifier.weight(1f).testTag("btn_suspend_vm_${vm.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Suspend", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suspend", fontSize = 12.sp)
                        }

                        // Action 3: Stop VM
                        OutlinedButton(
                            onClick = onStop,
                            modifier = Modifier.weight(0.9f).testTag("btn_stop_vm_${vm.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", fontSize = 12.sp)
                        }
                    }

                    isSuspended -> {
                        // Action 1: Resume VM
                        Button(
                            onClick = onResume,
                            modifier = Modifier.weight(1.2f).testTag("btn_resume_vm_${vm.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = KvmGreen, contentColor = Slate900),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Action 2: View Display
                        OutlinedButton(
                            onClick = onOpenDisplay,
                            modifier = Modifier.weight(1f).testTag("btn_view_vm_${vm.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WindowsCyan)
                        ) {
                            Icon(Icons.Outlined.Laptop, contentDescription = "Display", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View", fontSize = 12.sp)
                        }

                        // Action 3: Stop / Power Off
                        OutlinedButton(
                            onClick = onStop,
                            modifier = Modifier.weight(0.9f).testTag("btn_poweroff_vm_${vm.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Power Off", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", fontSize = 12.sp)
                        }
                    }

                    else -> {
                        // STOPPED VM Actions
                        // Action 1: Start Machine
                        Button(
                            onClick = onStart,
                            modifier = Modifier.weight(1.3f).testTag("btn_start_vm_${vm.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start VM", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Action 2: Settings
                        OutlinedButton(
                            onClick = onSettings,
                            modifier = Modifier.weight(0.9f).testTag("btn_settings_vm_${vm.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Config", fontSize = 12.sp)
                        }

                        // Action 3: Quick Snapshot
                        OutlinedButton(
                            onClick = onTakeSnapshot,
                            modifier = Modifier.weight(0.9f).testTag("btn_snapshot_vm_${vm.id}"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Snapshot", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Snap", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecBadge(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Slate800)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$label: $value",
            fontSize = 10.sp,
            color = Slate300,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun NoFilterMatchCard(
    currentFilter: String,
    onResetFilter: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No $currentFilter Virtual Machines",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "There are no virtual machines matching your current status filter.",
                fontSize = 12.sp,
                color = Slate400
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onResetFilter,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Show All VMs", color = WindowsCyan)
            }
        }
    }
}

@Composable
fun EmptyVmState(
    onCreateClick: () -> Unit,
    onTemplateClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WindowsLogoMini(size = 48)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Virtual Machines Found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Create a new Windows 11 ARM64 virtual machine from an ISO file or start with a pre-configured template.",
                fontSize = 13.sp,
                color = Slate400,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Custom VM from ISO", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("OR QUICK TEMPLATES", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onTemplateClick("WIN11_PRO") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Win 11 Pro", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onTemplateClick("WIN11_TINY") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tiny11 Lite", fontSize = 12.sp)
                }
            }
        }
    }
}
