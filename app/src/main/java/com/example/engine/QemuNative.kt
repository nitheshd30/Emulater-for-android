package com.example.engine

import android.util.Log

/**
 * JNI Bindings for the native QEMU ARM engine.
 * Requires the actual compiled QEMU shared libraries (e.g. libqemu-system-aarch64.so)
 * to be placed in app/src/main/jniLibs/arm64-v8a/
 */
object QemuNative {

    private const val TAG = "QemuNative"
    var isLoaded = false
        private set

    init {
        try {
            System.loadLibrary("qemu-bridge")
            isLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load qemu-bridge native library. Ensure NDK and CMake are configured and QEMU .so files are present.", e)
        }
    }

    /**
     * Executes the main QEMU binary with the provided command line arguments.
     * This is a blocking call that runs the QEMU event loop natively.
     */
    external fun startQemu(args: Array<String>): Int

    /**
     * Gracefully requests the QEMU engine to power down the virtual machine.
     */
    external fun stopQemu()
}
