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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowsGuideScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WindowsLogoMini(size = 20)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Windows 11 ARM & KVM Guide", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(WindowsCyan, WindowsDeepBlue)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Running Windows 11 ARM on Android (UTM-Style)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Because your smartphone uses ARM64 architecture (Qualcomm Snapdragon, MediaTek Dimensity, Google Tensor), it can execute Windows 11 ARM64 instructions at near-native speed using hardware virtualization (KVM) or QEMU TCG JIT translation.",
                        fontSize = 12.sp,
                        color = Slate300,
                        lineHeight = 17.sp
                    )
                }
            }

            // Step 1: Downloading Windows 11 ARM ISO
            GuideCard(
                stepNumber = "1",
                title = "Obtaining Official Windows 11 ARM64 ISO",
                icon = Icons.Default.Download,
                content = {
                    Text(
                        text = "You need an official Windows 11 ARM64 build (24H2 or 23H2). Microsoft provides official VHDX/ISO images via:\n\n" +
                                "• UUP dump (uupdump.net): Select Windows 11 (ARM64) -> Create download package.\n" +
                                "• Microsoft Windows Insider ISO: Official ARM64 Developer preview.\n" +
                                "• Tiny11 ARM64: Lightweight stripped edition requiring only 2GB RAM.\n\n" +
                                "Once downloaded, tap 'Browse ISO' in the VM Creation Wizard.",
                        fontSize = 12.sp,
                        color = Slate300,
                        lineHeight = 17.sp
                    )
                }
            )

            // Step 2: KVM Hypervisor on Android
            GuideCard(
                stepNumber = "2",
                title = "Hardware KVM Hypervisor on Android",
                icon = Icons.Default.Memory,
                content = {
                    Text(
                        text = "Android 13+ on Snapdragon 8 Gen 1/2/3 and Tensor G2/G3/G4 chips support pKVM (Protected KVM) or native /dev/kvm virtualization.\n\n" +
                                "• If /dev/kvm is present: Virtual CPU executes directly on hardware cores (near 100% native speed).\n" +
                                "• If KVM is locked by OEM: WinARM seamlessly falls back to QEMU's TCG (Tiny Code Generator) JIT engine.",
                        fontSize = 12.sp,
                        color = Slate300,
                        lineHeight = 17.sp
                    )
                }
            )

            // Step 3: Windows 11 TPM & RAM Bypasses
            GuideCard(
                stepNumber = "3",
                title = "Automated TPM 2.0 & SecureBoot Bypasses",
                icon = Icons.Default.Security,
                content = {
                    Text(
                        text = "Windows 11 normally enforces TPM 2.0, SecureBoot, and 4GB RAM minimums. WinARM automatically applies the following LabConfig registry keys:\n\n" +
                                "• BypassTPMCheck = 1\n" +
                                "• BypassSecureBootCheck = 1\n" +
                                "• BypassRAMCheck = 1\n" +
                                "• BypassStorageCheck = 1\n" +
                                "• oobe\\bypassnro (Bypasses required Microsoft account on OOBE)",
                        fontSize = 12.sp,
                        color = Slate300,
                        lineHeight = 17.sp
                    )
                }
            )

            // Step 4: VirtIO Drivers
            GuideCard(
                stepNumber = "4",
                title = "VirtIO Drivers & Storage Acceleration",
                icon = Icons.Default.Storage,
                content = {
                    Text(
                        text = "WinARM mounts the virtio-win.iso driver disk containing signed ARM64 drivers for:\n\n" +
                                "• viostor / vioscsi: High performance virtual disk I/O\n" +
                                "• netkvm: 10Gbps VirtIO virtual network adapter\n" +
                                "• virtio-gpu: 3D hardware acceleration via Virgl",
                        fontSize = 12.sp,
                        color = Slate300,
                        lineHeight = 17.sp
                    )
                }
            )

            // Step 5: Termux / Shizuku QEMU CLI Script
            GuideCard(
                stepNumber = "5",
                title = "Running via Termux / Native CLI",
                icon = Icons.Default.Code,
                content = {
                    Column {
                        Text(
                            text = "You can export the full QEMU CLI script to launch Windows 11 with custom flags via Termux or Shizuku:",
                            fontSize = 12.sp,
                            color = Slate300
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val sampleScript = "pkg install qemu-system-aarch64\n" +
                                "qemu-system-aarch64 -M virt -accel kvm -cpu host -smp 4 -m 4096 \\\n" +
                                "  -bios QEMU_EFI.fd -drive file=win11.qcow2,if=virtio \\\n" +
                                "  -cdrom win11_arm64.iso -device virtio-gpu-pci -nic user,model=virtio-net-pci"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate850)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = sampleScript,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = WindowsCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val clipMan = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipMan.setPrimaryClip(ClipData.newPlainText("QEMU Script", sampleScript))
                                Toast.makeText(context, "Copied sample script!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = WindowsCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Script", fontSize = 11.sp)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun GuideCard(
    stepNumber: String,
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Slate700, Slate800)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(WindowsCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stepNumber, fontWeight = FontWeight.Bold, color = WindowsCyan, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(icon, contentDescription = null, tint = WindowsCyan, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
