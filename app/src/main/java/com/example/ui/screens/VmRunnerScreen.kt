package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VirtualMachine
import com.example.engine.ActiveWindow
import com.example.engine.VmBootState
import com.example.ui.components.WindowsLogoMini
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.KvmGreen
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WindowsCyan
import com.example.ui.theme.WindowsDeepBlue
import com.example.ui.viewmodel.VmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VmRunnerScreen(
    vm: VirtualMachine,
    viewModel: VmViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val runtimeState by viewModel.runtimeState.collectAsStateWithLifecycle()
    val runtimeManager = viewModel.runtimeManager

    var isMenuOpen by remember { mutableStateOf(false) }
    var isIsoSelectorOpen by remember { mutableStateOf(false) }

    LaunchedEffect(runtimeState.toastMessage) {
        runtimeState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            runtimeManager.clearToast()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // VM Top Control Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WindowsLogoMini(size = 18)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(vm.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate100)
                        Text(
                            text = "${runtimeState.bootState.name} • ${if (vm.useKvm) "KVM ACCELERATED" else "TCG ARM64"}",
                            fontSize = 10.sp,
                            color = if (runtimeState.bootState == VmBootState.WINDOWS_DESKTOP) KvmGreen else WindowsCyan
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate300)
                }
            },
            actions = {
                // Keyboard Toggle
                IconButton(onClick = { runtimeManager.toggleKeyboard() }) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Keyboard",
                        tint = if (runtimeState.isKeyboardVisible) WindowsCyan else Slate400
                    )
                }
                // Trackpad / Touch mode
                IconButton(onClick = { runtimeManager.toggleTrackpadMode() }) {
                    Icon(
                        imageVector = if (runtimeState.isTrackpadMode) Icons.Default.Mouse else Icons.Default.TouchApp,
                        contentDescription = "Mouse Mode",
                        tint = if (runtimeState.isTrackpadMode) WindowsCyan else Slate400
                    )
                }
                // Serial Console Toggle
                IconButton(onClick = { runtimeManager.toggleSerialConsole() }) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Serial Console",
                        tint = if (runtimeState.isSerialConsoleVisible) WindowsCyan else Slate400
                    )
                }
                // Power Actions Menu
                Box {
                    IconButton(onClick = { isMenuOpen = true }) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", tint = ErrorRose)
                    }
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false },
                        modifier = Modifier.background(Slate850)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ctrl + Alt + Del", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                isMenuOpen = false
                                runtimeManager.sendKeyCombination("CTRL_ALT_DEL")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Pause / Resume", color = MaterialTheme.colorScheme.onSurface) },
                            leadingIcon = { Icon(Icons.Default.Pause, contentDescription = null, tint = WarningAmber) },
                            onClick = {
                                isMenuOpen = false
                                runtimeManager.pauseVm()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Restart Machine", color = MaterialTheme.colorScheme.onSurface) },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = WindowsCyan) },
                            onClick = {
                                isMenuOpen = false
                                runtimeManager.resetVm()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Swap / Eject ISO", color = MaterialTheme.colorScheme.onSurface) },
                            leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, tint = Slate300) },
                            onClick = {
                                isMenuOpen = false
                                isIsoSelectorOpen = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Shut Down", color = ErrorRose) },
                            leadingIcon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = ErrorRose) },
                            onClick = {
                                isMenuOpen = false
                                runtimeManager.stopVm()
                                onNavigateBack()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Slate900,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Telemetry HUD Pill (Top Floating)
        AnimatedVisibility(visible = runtimeState.isTelemetryHudVisible) {
            TelemetryHud(telemetry = runtimeState.telemetry)
        }

        // Main Virtual Machine Screen Display Viewport
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            // Pointer input tracker on the VM display
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                val xPct = (offset.x / size.width.toFloat()).coerceIn(0.01f, 0.99f)
                                val yPct = (offset.y / size.height.toFloat()).coerceIn(0.01f, 0.99f)
                                runtimeManager.updateMousePosition(xPct, yPct)
                                runtimeManager.triggerMouseClick(isRightClick = false)
                            },
                            onLongPress = { offset ->
                                val xPct = (offset.x / size.width.toFloat()).coerceIn(0.01f, 0.99f)
                                val yPct = (offset.y / size.height.toFloat()).coerceIn(0.01f, 0.99f)
                                runtimeManager.updateMousePosition(xPct, yPct)
                                runtimeManager.triggerMouseClick(isRightClick = true)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val cur = runtimeState.mouseState
                            val nextX = (cur.xPercent + (dragAmount.x / size.width.toFloat())).coerceIn(0.01f, 0.99f)
                            val nextY = (cur.yPercent + (dragAmount.y / size.height.toFloat())).coerceIn(0.01f, 0.99f)
                            runtimeManager.updateMousePosition(nextX, nextY)
                        }
                    }
            ) {
                // Render content depending on Boot State
                when (runtimeState.bootState) {
                    VmBootState.OFF -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Virtual Machine Powered Off", fontSize = 16.sp, color = Slate400)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { runtimeManager.startVm(vm) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Power On", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    VmBootState.UEFI_INIT -> {
                        UefiBootScreen(
                            vm = vm,
                            progress = runtimeState.bootProgress,
                            logs = runtimeState.bootLogLines
                        )
                    }
                    VmBootState.WINDOWS_BOOTING -> {
                        WindowsBootAnimationScreen()
                    }
                    VmBootState.WINDOWS_SETUP_OOBE -> {
                        WindowsSetupOobeScreen(
                            vm = vm,
                            isoName = runtimeState.currentIsoName,
                            progress = runtimeState.setupProgressPercent,
                            step = runtimeState.setupCurrentStep,
                            onFinish = { runtimeManager.finishSetupWizard() },
                            onStep = { pct, step -> runtimeManager.stepSetupProgress(pct, step) }
                        )
                    }
                    VmBootState.WINDOWS_DESKTOP -> {
                        WindowsDesktopScreen(
                            vm = vm,
                            runtimeState = runtimeState,
                            runtimeManager = runtimeManager
                        )
                    }
                    VmBootState.PAUSED -> {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("VM PAUSED", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { runtimeManager.pauseVm() },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Slate900)
                                ) {
                                    Text("Resume Machine", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Virtual Mouse Cursor Overlay
                if (runtimeState.bootState != VmBootState.OFF) {
                    val mouseX = (runtimeState.mouseState.xPercent * screenWidth.value).dp
                    val mouseY = (runtimeState.mouseState.yPercent * screenHeight.value).dp

                    Box(
                        modifier = Modifier
                            .offset(x = mouseX, y = mouseY)
                            .size(18.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width, size.height * 0.7f)
                                lineTo(size.width * 0.55f, size.height * 0.7f)
                                lineTo(size.width * 0.8f, size.height)
                                lineTo(size.width * 0.6f, size.height * 1.1f)
                                lineTo(size.width * 0.35f, size.height * 0.8f)
                                lineTo(0f, size.height * 1.0f)
                                close()
                            }
                            drawPath(path, color = Color.White)
                            drawPath(path, color = Color.Black, style = Stroke(width = 2f))
                        }
                    }
                }
            }

            // Serial Console Overlay (if enabled)
            if (runtimeState.isSerialConsoleVisible) {
                SerialConsoleOverlay(
                    logs = runtimeState.bootLogLines,
                    onClose = { runtimeManager.toggleSerialConsole() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Virtual On-Screen PC Keyboard Drawer (Expandable)
        AnimatedVisibility(visible = runtimeState.isKeyboardVisible) {
            VirtualPcKeyboard(
                runtimeManager = runtimeManager,
                onSendKey = { key -> runtimeManager.sendKeyCombination(key) }
            )
        }

        // Quick Bottom VM Controls Toolbar
        VmBottomControlBar(
            runtimeManager = runtimeManager,
            onLeftClick = { runtimeManager.triggerMouseClick(isRightClick = false) },
            onRightClick = { runtimeManager.triggerMouseClick(isRightClick = true) }
        )
    }

    // ISO Swapper Dialog
    if (isIsoSelectorOpen) {
        val sampleIsos = listOf(
            "Windows_11_ARM64_Pro_24H2.iso",
            "Tiny11_ARM64_Lightweight.iso",
            "virtio-win-0.1.240.iso (Drivers)",
            "Ubuntu_24.04_ARM64.iso"
        )

        DropdownMenu(
            expanded = isIsoSelectorOpen,
            onDismissRequest = { isIsoSelectorOpen = false },
            modifier = Modifier.background(Slate850)
        ) {
            sampleIsos.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        runtimeManager.mountIso(name)
                        isIsoSelectorOpen = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Eject Current CD-ROM", color = ErrorRose) },
                onClick = {
                    runtimeManager.ejectIso()
                    isIsoSelectorOpen = false
                }
            )
        }
    }
}

@Composable
fun TelemetryHud(telemetry: com.example.engine.VmTelemetry) {
    Surface(
        color = Slate900,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudPill(label = "FPS", value = "${telemetry.fps}", color = KvmGreen)
            HudPill(label = "CPU", value = "${telemetry.cpuUsagePercent}%", color = WindowsCyan)
            HudPill(label = "RAM", value = "${telemetry.guestRamUsedMb}MB", color = WindowsCyan)
            HudPill(label = "IO", value = String.format("%.1f MB/s", telemetry.diskReadMbSec), color = WarningAmber)
            HudPill(label = "TEMP", value = String.format("%.1f°C", telemetry.hostTempCelsius), color = Slate300)
        }
    }
}

@Composable
fun HudPill(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun UefiBootScreen(
    vm: VirtualMachine,
    progress: Float,
    logs: List<String>
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // EDK2 UEFI Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TianoCore EDK II UEFI Firmware v2024.08 (ARM64)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "CPU: ${vm.cpuCores} Cores | RAM: ${vm.ramMb} MB",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = WindowsCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = WindowsCyan,
                trackColor = Slate800
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Boot log lines
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f)
            ) {
                items(logs) { log ->
                    Text(
                        text = ">> $log",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (log.contains("EFI") || log.contains("ACPI")) KvmGreen else Slate300
                    )
                }
            }
        }
    }
}

@Composable
fun WindowsBootAnimationScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "win_spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WindowsLogoMini(size = 64)
            Spacer(modifier = Modifier.height(56.dp))

            // Windows 11 spinning dots
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .rotate(rotation)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val dotRadius = 3.5f
                    for (i in 0 until 5) {
                        val angle = (i * 30.0) * (Math.PI / 180.0)
                        val x = (center.x + radius * Math.cos(angle)).toFloat()
                        val y = (center.y + radius * Math.sin(angle)).toFloat()
                        drawCircle(
                            color = Color.White.copy(alpha = 1f - (i * 0.18f)),
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Starting Windows 11 ARM...",
                fontSize = 12.sp,
                color = Slate400,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun WindowsSetupOobeScreen(
    vm: VirtualMachine,
    isoName: String,
    progress: Int,
    step: String,
    onFinish: () -> Unit,
    onStep: (Int, String) -> Unit
) {
    var stepIndex by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF0F2B48), Color(0xFF07121E))))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.95f)),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(WindowsCyan, WindowsDeepBlue)))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WindowsLogoMini(size = 28)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Windows 11 Setup (ARM64)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (stepIndex == 0) {
                    Text("Select Target Installation Drive", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WindowsCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate850)
                            .border(1.dp, WindowsCyan, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = WindowsCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Drive 0: VirtIO SCSI Disk (${vm.diskSizeGb}.0 GB)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Unallocated Space • VirtIO drivers ready", fontSize = 11.sp, color = Slate400)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // Bypasses Applied status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("✔ TPM 2.0 Bypass Injected", fontSize = 10.sp, color = KvmGreen)
                        Text("✔ SecureBoot Bypassed", fontSize = 10.sp, color = KvmGreen)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            stepIndex = 1
                            onStep(25, "Copying Windows files (25%)...")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Install Windows 11 Now", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Installing Windows 11 ARM64", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WindowsCyan)
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = WindowsCyan,
                        trackColor = Slate800
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(step, fontSize = 12.sp, color = Slate300)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (progress < 100) {
                        Button(
                            onClick = {
                                val next = (progress + 35).coerceAtMost(100)
                                if (next >= 100) {
                                    onFinish()
                                } else {
                                    onStep(next, "Expanding Windows ARM packages ($next%)...")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Fast Forward Installation", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onFinish,
                            colors = ButtonDefaults.buttonColors(containerColor = KvmGreen, contentColor = Slate900),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Boot into Desktop", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WindowsDesktopScreen(
    vm: VirtualMachine,
    runtimeState: com.example.engine.VmRuntimeState,
    runtimeManager: com.example.engine.VmRuntimeManager
) {
    val currentTime = remember { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F3A66),
                        Color(0xFF071930),
                        Color(0xFF020914)
                    )
                )
            )
    ) {
        // Windows 11 Desktop Bloom Vector Wallpaper in center
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width * 0.5f, size.height * 0.45f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00ADEF).copy(alpha = 0.22f), Color.Transparent),
                    center = centerOffset,
                    radius = size.width * 0.6f
                ),
                center = centerOffset,
                radius = size.width * 0.6f
            )
        }

        // Desktop App Icons Grid (Top-Left)
        Column(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DesktopIcon(
                title = "This PC",
                icon = Icons.Default.Storage,
                tint = WindowsCyan,
                onClick = { runtimeManager.openWindow(ActiveWindow.FILE_EXPLORER) }
            )
            DesktopIcon(
                title = "Edge (ARM64)",
                icon = Icons.Default.OpenInBrowser,
                tint = WindowsCyan,
                onClick = { runtimeManager.openWindow(ActiveWindow.EDGE_BROWSER) }
            )
            DesktopIcon(
                title = "Terminal",
                icon = Icons.Default.Terminal,
                tint = Color.White,
                onClick = { runtimeManager.openWindow(ActiveWindow.TERMINAL) }
            )
            DesktopIcon(
                title = "Task Manager",
                icon = Icons.Default.Memory,
                tint = KvmGreen,
                onClick = { runtimeManager.openWindow(ActiveWindow.TASK_MANAGER) }
            )
            DesktopIcon(
                title = "Notepad",
                icon = Icons.Default.Folder,
                tint = WarningAmber,
                onClick = { runtimeManager.openWindow(ActiveWindow.NOTEPAD) }
            )
        }

        // Active Application Windows Layer
        when (runtimeState.activeWindow) {
            ActiveWindow.TERMINAL -> {
                WindowTerminal(
                    history = runtimeState.terminalHistory,
                    onExecute = { cmd -> runtimeManager.executeTerminalCommand(cmd) },
                    onClose = { runtimeManager.closeWindow(ActiveWindow.TERMINAL) },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.72f)
                        .align(Alignment.Center)
                )
            }
            ActiveWindow.TASK_MANAGER -> {
                WindowTaskManager(
                    telemetry = runtimeState.telemetry,
                    vm = vm,
                    onClose = { runtimeManager.closeWindow(ActiveWindow.TASK_MANAGER) },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.72f)
                        .align(Alignment.Center)
                )
            }
            ActiveWindow.FILE_EXPLORER -> {
                WindowFileExplorer(
                    vm = vm,
                    isoName = runtimeState.currentIsoName,
                    onClose = { runtimeManager.closeWindow(ActiveWindow.FILE_EXPLORER) },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.72f)
                        .align(Alignment.Center)
                )
            }
            ActiveWindow.EDGE_BROWSER -> {
                WindowEdgeBrowser(
                    onClose = { runtimeManager.closeWindow(ActiveWindow.EDGE_BROWSER) },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.72f)
                        .align(Alignment.Center)
                )
            }
            ActiveWindow.NOTEPAD -> {
                WindowNotepad(
                    text = runtimeState.notepadText,
                    onTextChange = { runtimeManager.updateNotepadText(it) },
                    onClose = { runtimeManager.closeWindow(ActiveWindow.NOTEPAD) },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.72f)
                        .align(Alignment.Center)
                )
            }
            ActiveWindow.START_MENU -> {
                WindowsStartMenu(
                    vm = vm,
                    onOpenApp = { win ->
                        runtimeManager.openWindow(win)
                    },
                    onShutDown = { runtimeManager.stopVm() },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(340.dp)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 52.dp)
                )
            }
            ActiveWindow.NONE, ActiveWindow.SETTINGS -> {
                // Background Desktop
            }
        }

        // Windows 11 Center Taskbar (Bottom)
        WindowsTaskbar(
            activeWindow = runtimeState.activeWindow,
            timeString = currentTime,
            onStartClick = { runtimeManager.toggleStartMenu() },
            onExplorerClick = { runtimeManager.openWindow(ActiveWindow.FILE_EXPLORER) },
            onEdgeClick = { runtimeManager.openWindow(ActiveWindow.EDGE_BROWSER) },
            onTerminalClick = { runtimeManager.openWindow(ActiveWindow.TERMINAL) },
            onTaskManagerClick = { runtimeManager.openWindow(ActiveWindow.TASK_MANAGER) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun DesktopIcon(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Slate900.copy(alpha = 0.6f))
                .border(0.5.dp, Slate700, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun WindowsTaskbar(
    activeWindow: ActiveWindow,
    timeString: String,
    onStartClick: () -> Unit,
    onExplorerClick: () -> Unit,
    onEdgeClick: () -> Unit,
    onTerminalClick: () -> Unit,
    onTaskManagerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Slate900.copy(alpha = 0.9f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left space / widget preview
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = "VirtIO Net", tint = WindowsCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("VirtIO 10G", fontSize = 10.sp, color = Slate400)
            }

            // Center Taskbar App Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Windows 11 Start Button
                TaskbarIcon(
                    icon = { WindowsLogoMini(size = 20) },
                    isActive = activeWindow == ActiveWindow.START_MENU,
                    onClick = onStartClick
                )
                // Search
                TaskbarIcon(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = WindowsCyan, modifier = Modifier.size(20.dp)) },
                    isActive = false,
                    onClick = onStartClick
                )
                // File Explorer
                TaskbarIcon(
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Explorer", tint = WarningAmber, modifier = Modifier.size(20.dp)) },
                    isActive = activeWindow == ActiveWindow.FILE_EXPLORER,
                    onClick = onExplorerClick
                )
                // Edge Browser
                TaskbarIcon(
                    icon = { Icon(Icons.Default.OpenInBrowser, contentDescription = "Edge", tint = WindowsCyan, modifier = Modifier.size(20.dp)) },
                    isActive = activeWindow == ActiveWindow.EDGE_BROWSER,
                    onClick = onEdgeClick
                )
                // Terminal
                TaskbarIcon(
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Terminal", tint = Color.White, modifier = Modifier.size(20.dp)) },
                    isActive = activeWindow == ActiveWindow.TERMINAL,
                    onClick = onTerminalClick
                )
                // Task Manager
                TaskbarIcon(
                    icon = { Icon(Icons.Default.Memory, contentDescription = "Task Manager", tint = KvmGreen, modifier = Modifier.size(20.dp)) },
                    isActive = activeWindow == ActiveWindow.TASK_MANAGER,
                    onClick = onTaskManagerClick
                )
            }

            // Right System Tray (Clock & Status)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeString,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun TaskbarIcon(
    icon: @Composable () -> Unit,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) WindowsCyan.copy(alpha = 0.25f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
fun WindowsStartMenu(
    vm: VirtualMachine,
    onOpenApp: (ActiveWindow) -> Unit,
    onShutDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.95f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Slate850)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Type here to search apps, files, settings...", fontSize = 11.sp, color = Slate400)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Pinned Applications", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate300)
            Spacer(modifier = Modifier.height(8.dp))

            // Pinned Apps Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StartMenuItem("Terminal", Icons.Default.Terminal, Color.White) { onOpenApp(ActiveWindow.TERMINAL) }
                StartMenuItem("Task Mgr", Icons.Default.Memory, KvmGreen) { onOpenApp(ActiveWindow.TASK_MANAGER) }
                StartMenuItem("Explorer", Icons.Default.Folder, WarningAmber) { onOpenApp(ActiveWindow.FILE_EXPLORER) }
                StartMenuItem("Edge", Icons.Default.OpenInBrowser, WindowsCyan) { onOpenApp(ActiveWindow.EDGE_BROWSER) }
                StartMenuItem("Notepad", Icons.Default.Storage, Color(0xFF38BDF8)) { onOpenApp(ActiveWindow.NOTEPAD) }
            }

            Spacer(modifier = Modifier.weight(1f))

            // User & Power footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate850)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(WindowsCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Administrator (ARM64)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }

                IconButton(onClick = onShutDown) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = "Shutdown", tint = ErrorRose, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun StartMenuItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Slate850),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, fontSize = 9.sp, color = Slate300)
    }
}

@Composable
fun WindowTerminal(
    history: List<com.example.engine.TerminalLine>,
    onExecute: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cmdInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C0C)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Window Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate900)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Windows PowerShell (ARM64)", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(16.dp))
                }
            }

            // Command log
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                items(history) { line ->
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (line.isCommand) WindowsCyan else if (line.isError) ErrorRose else Color(0xFFCCCCCC)
                    )
                }
            }

            // Command Input Prompt
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate900)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PS C:\\Users\\Admin>", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = WindowsCyan)
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedTextField(
                    value = cmdInput,
                    onValueChange = { cmdInput = it },
                    singleLine = true,
                    placeholder = { Text("type 'help', 'systeminfo', 'tweak'...", fontSize = 11.sp, color = Slate400) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                IconButton(onClick = {
                    onExecute(cmdInput)
                    cmdInput = ""
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Run", tint = WindowsCyan)
                }
            }
        }
    }
}

@Composable
fun WindowTaskManager(
    telemetry: com.example.engine.VmTelemetry,
    vm: VirtualMachine,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = KvmGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Task Manager (ARM64 Performance)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Performance Graphs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // CPU Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate850)
                        .padding(10.dp)
                ) {
                    Column {
                        Text("CPU Utilization", fontSize = 11.sp, color = Slate400)
                        Text("${telemetry.cpuUsagePercent}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)
                        Text("${vm.cpuCores} Cores @ 2.84 GHz", fontSize = 9.sp, color = Slate400)
                    }
                }
                // Memory Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate850)
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Memory (RAM)", fontSize = 11.sp, color = Slate400)
                        Text(String.format("%.1f / %.1f GB", telemetry.guestRamUsedMb / 1024f, vm.ramMb / 1024f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = KvmGreen)
                        Text("${((telemetry.guestRamUsedMb.toFloat() / vm.ramMb) * 100).toInt()}% Used", fontSize = 9.sp, color = Slate400)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Top Active ARM64 Processes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate300)
            Spacer(modifier = Modifier.height(6.dp))

            // Process Table
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate850)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProcessRow("System", "0.2%", "240 KB")
                ProcessRow("Windows Explorer", "2.1%", "124 MB")
                ProcessRow("Desktop Window Manager", "3.4%", "88 MB")
                ProcessRow("Microsoft Edge (ARM64)", "8.6%", "214 MB")
                ProcessRow("VirtIO Guest Service", "0.1%", "8.2 MB")
                ProcessRow("Antimalware Service Executable", "1.4%", "142 MB")
            }
        }
    }
}

@Composable
fun ProcessRow(name: String, cpu: String, mem: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(cpu, fontSize = 11.sp, color = WindowsCyan)
            Text(mem, fontSize = 11.sp, color = Slate400)
        }
    }
}

@Composable
fun WindowFileExplorer(
    vm: VirtualMachine,
    isoName: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("File Explorer - This PC", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Devices and Drives (4)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate300)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DriveItem("Local Disk (C:)", "52.4 GB free of ${vm.diskSizeGb}.0 GB • VirtIO Block", 0.2f)
                DriveItem("CD Drive (D:) $isoName", "5.24 GB • ISO Disc Image", 1.0f)
                DriveItem("CD Drive (E:) virtio-win.iso", "580 MB • Guest Drivers", 1.0f)
                DriveItem("Shared Folder (Z:)", "Android Host Storage Pass-through", 0.05f)
            }
        }
    }
}

@Composable
fun DriveItem(title: String, subtitle: String, fillFraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Slate850)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storage, contentDescription = null, tint = WindowsCyan, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { fillFraction },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = WindowsCyan,
                    trackColor = Slate700
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, fontSize = 10.sp, color = Slate400)
            }
        }
    }
}

@Composable
fun WindowEdgeBrowser(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = WindowsCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Microsoft Edge (ARM64)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Address bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate850)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("https://learn.microsoft.com/en-us/windows/arm/", fontSize = 11.sp, color = WindowsCyan)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate950)
                    .padding(12.dp)
            ) {
                Column {
                    Text("Windows on ARM Architecture", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Windows 11 ARM64 runs natively on Qualcomm Snapdragon and ARM Cortex cores with exceptional performance, full x64/x86 Prism emulation translation, and hypervisor nested virtualization.",
                        fontSize = 11.sp,
                        color = Slate300,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WindowNotepad(
    text: String,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notepad - notes.txt", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Slate700,
                    unfocusedBorderColor = Slate700,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }
    }
}

@Composable
fun SerialConsoleOverlay(
    logs: List<String>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xEE050B14),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(1.dp, WindowsCyan.copy(alpha = 0.4f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LIVE SERIAL CONSOLE (/dev/ttyAMA0)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WindowsCyan)
                IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(14.dp))
                }
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(logs.takeLast(12)) { line ->
                    Text(text = line, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = KvmGreen)
                }
            }
        }
    }
}

@Composable
fun VirtualPcKeyboard(
    runtimeManager: com.example.engine.VmRuntimeManager,
    onSendKey: (String) -> Unit
) {
    Surface(
        color = Slate900,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Function Keys Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Esc", "F1", "F2", "F3", "F4", "F5", "F8", "F11", "F12", "Del").forEach { k ->
                    KeyCap(label = k, modifier = Modifier.weight(1f)) { onSendKey(k) }
                }
            }

            // Key Combos Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                KeyCap("Ctrl+Alt+Del", modifier = Modifier.weight(1.5f), color = ErrorRose) { onSendKey("CTRL_ALT_DEL") }
                KeyCap("Alt+Tab", modifier = Modifier.weight(1.2f)) { onSendKey("ALT_TAB") }
                KeyCap("Win+E", modifier = Modifier.weight(1.2f)) { onSendKey("WIN_E") }
                KeyCap("Win+X", modifier = Modifier.weight(1.2f)) { onSendKey("WIN_X") }
                KeyCap("TaskMgr", modifier = Modifier.weight(1.2f), color = KvmGreen) { onSendKey("TASK_MGR") }
            }

            // Modifiers Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                KeyCap("Ctrl", modifier = Modifier.weight(1f)) { onSendKey("Ctrl") }
                KeyCap("Alt", modifier = Modifier.weight(1f)) { onSendKey("Alt") }
                KeyCap("Win", modifier = Modifier.weight(1f)) { onSendKey("Win") }
                KeyCap("Tab", modifier = Modifier.weight(1f)) { onSendKey("Tab") }
                KeyCap("Enter", modifier = Modifier.weight(1.5f), color = WindowsCyan) { onSendKey("Enter") }
            }
        }
    }
}

@Composable
fun KeyCap(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = Slate300,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Slate800)
            .border(0.5.dp, Slate700, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun VmBottomControlBar(
    runtimeManager: com.example.engine.VmRuntimeManager,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
    Surface(
        color = Slate900,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Virtual Mouse Left Click
            Button(
                onClick = onLeftClick,
                colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = WindowsCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Left Click", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Win Button
            Button(
                onClick = { runtimeManager.toggleStartMenu() },
                colors = ButtonDefaults.buttonColors(containerColor = WindowsCyan, contentColor = Slate900),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(0.8f)
            ) {
                WindowsLogoMini(size = 16)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Virtual Mouse Right Click
            Button(
                onClick = onRightClick,
                colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = WindowsCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Right Click", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val Slate100 = Color(0xFFF1F5F9)
