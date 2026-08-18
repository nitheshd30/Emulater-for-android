package com.example.engine

import com.example.data.model.VirtualMachine

object QemuCommandBuilder {

    fun buildQemuArguments(vm: VirtualMachine): List<String> {
        val args = mutableListOf<String>()

        // Base Binary & Machine Type
        args.add("qemu-system-aarch64")
        args.add("-M")
        args.add("virt,highmem=on,virtualization=on")

        // CPU & Hypervisor
        if (vm.useKvm) {
            args.add("-enable-kvm")
            args.add("-cpu")
            args.add("host")
        } else {
            args.add("-cpu")
            args.add(vm.cpuModel.ifEmpty { "cortex-a76" })
        }

        // SMP Cores & RAM
        args.add("-smp")
        args.add("cores=${vm.cpuCores},threads=1,sockets=1")
        args.add("-m")
        args.add("${vm.ramMb}M")

        // UEFI Firmware & NVRAM
        args.add("-drive")
        args.add("if=pflash,format=raw,readonly=on,file=QEMU_EFI.fd")
        args.add("-drive")
        args.add("if=pflash,format=raw,file=vars.fd")

        // Hard Disk
        val diskFile = if (vm.diskPath.isNotEmpty()) vm.diskPath else "win11_arm.${vm.diskFormat}"
        args.add("-device")
        args.add("virtio-blk-pci,drive=drive0,bootindex=1")
        args.add("-drive")
        args.add("if=none,id=drive0,file=$diskFile,format=${vm.diskFormat},cache=writeback,discard=unmap")

        // CD-ROM / ISO image
        if (vm.isoPath.isNotEmpty() || vm.isoName.isNotEmpty()) {
            val isoTarget = if (vm.isoName.isNotEmpty()) vm.isoName else "windows_11_arm.iso"
            args.add("-device")
            args.add("usb-storage,drive=cdrom0,bootindex=0")
            args.add("-drive")
            args.add("if=none,id=cdrom0,file=$isoTarget,media=cdrom,readonly=on")
        }

        // VirtIO Windows Drivers ISO (if enabled)
        if (vm.virtIoDriversEnabled) {
            args.add("-device")
            args.add("usb-storage,drive=virtio_iso")
            args.add("-drive")
            args.add("if=none,id=virtio_iso,file=virtio-win.iso,media=cdrom,readonly=on")
        }

        // Display & GPU
        when (vm.gpuMode) {
            "VIRGL" -> {
                args.add("-device")
                args.add("virtio-gpu-gl-pci")
                args.add("-display")
                args.add("sdl,gl=on")
            }
            "VIRTIO_GPU" -> {
                args.add("-device")
                args.add("virtio-gpu-pci,xres=1600,yres=900")
            }
            "RAMFB" -> {
                args.add("-device")
                args.add("ramfb")
            }
            else -> {
                args.add("-device")
                args.add("virtio-vga")
            }
        }

        // Mouse, Keyboard & Peripherals
        args.add("-device")
        args.add("qemu-xhci,id=xhci")
        args.add("-device")
        args.add("usb-tablet,bus=xhci.0") // Absolute touch pointer tracking
        args.add("-device")
        args.add("usb-kbd,bus=xhci.0")

        // Audio
        if (vm.audioDevice != "NONE") {
            args.add("-audiodev")
            args.add("oboe,id=snd0")
            args.add("-device")
            args.add("intel-hda")
            args.add("-device")
            args.add("hda-duplex,audiodev=snd0")
        }

        // Networking & Port Forwarding (RDP 3389, SSH 2222)
        if (vm.networkMode == "USER_SLIRP") {
            val netdev = StringBuilder("user,id=net0")
            netdev.append(",hostfwd=tcp::${vm.portForwardRdp}-:3389")
            netdev.append(",hostfwd=tcp::${vm.portForwardSsh}-:22")
            if (vm.portForwardWeb > 0) {
                netdev.append(",hostfwd=tcp::${vm.portForwardWeb}-:80")
            }
            args.add("-netdev")
            args.add(netdev.toString())
            args.add("-device")
            args.add("virtio-net-pci,netdev=net0")
        }

        // TPM 2.0 Emulator (if not bypassed or emulated via swtpm)
        if (!vm.bypassTpm) {
            args.add("-chardev")
            args.add("socket,id=chrtpm,path=/tmp/swtpm-sock")
            args.add("-tpmdev")
            args.add("emulator,id=tpm0,chardev=chrtpm")
            args.add("-device")
            args.add("tpm-tis-device,tpmdev=tpm0")
        }

        // Shared Directory (9pfs / VirtFS)
        if (vm.sharedFolderPath.isNotEmpty()) {
            args.add("-fsdev")
            args.add("local,security_model=none,id=fsdev0,path=${vm.sharedFolderPath}")
            args.add("-device")
            args.add("virtio-9p-pci,fsdev=fsdev0,mount_tag=host_share")
        }

        // Serial / Monitor Console
        args.add("-serial")
        args.add("mon:stdio")
        args.add("-vnc")
        args.add("127.0.0.1:0")

        return args
    }

    fun buildCommandLineString(vm: VirtualMachine): String {
        return buildQemuArguments(vm).joinToString(" ")
    }

    fun buildTermuxScript(vm: VirtualMachine): String {
        val cmd = buildCommandLineString(vm)
        return """
            #!/data/data/com.termux/files/usr/bin/bash
            # ========================================================
            # UTM WinARM VM Launcher for Android (Windows 11 ARM64)
            # Machine: ${vm.name}
            # Cores: ${vm.cpuCores} | RAM: ${vm.ramMb}MB | KVM: ${vm.useKvm}
            # ========================================================
            
            echo "Starting WinARM VM: ${vm.name}..."
            echo "Checking KVM acceleration support..."
            
            if [ -e /dev/kvm ]; then
                echo "[OK] /dev/kvm accessible! Using bare-metal virtualization."
                chmod 666 /dev/kvm 2>/dev/null || true
            else
                echo "[INFO] KVM node not detected, fallback to TCG JIT compiler."
            fi
            
            # Execute QEMU ARM64 Hypervisor
            $cmd
        """.trimIndent()
    }
}
