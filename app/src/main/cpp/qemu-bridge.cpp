#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <thread>
#include <chrono>
#include <atomic>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "QemuBridge", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "QemuBridge", __VA_ARGS__)

// Forward declaration of the QEMU main entry point.
extern "C" int qemu_main(int argc, char **argv, char **envp);

std::atomic<bool> is_qemu_running(false);

extern "C" JNIEXPORT jint JNICALL
Java_com_example_engine_QemuNative_startQemu(JNIEnv* env, jobject /* this */, jobjectArray args) {
    int argc = env->GetArrayLength(args);
    std::vector<std::string> arg_strings;
    std::vector<char*> argv;

    for (int i = 0; i < argc; i++) {
        jstring string = (jstring) env->GetObjectArrayElement(args, i);
        const char* rawString = env->GetStringUTFChars(string, 0);
        arg_strings.push_back(std::string(rawString));
        env->ReleaseStringUTFChars(string, rawString);
    }

    for (auto& s : arg_strings) {
        argv.push_back(&s[0]);
    }
    
    argv.push_back(nullptr);

    LOGI("Starting Native QEMU Engine with %d arguments", argc);
    for (int i = 0; i < argc; i++) {
        LOGI("ARG[%d]: %s", i, argv[i]);
    }

#ifdef HAS_REAL_QEMU_SOURCE
    is_qemu_running = true;
    int result = qemu_main(argc, argv.data(), nullptr);
    is_qemu_running = false;
    LOGI("QEMU Engine exited with code %d", result);
    return result;
#else
    LOGE("Mock QEMU Bridge invoked. Real QEMU libraries are required to actually emulate.");
    is_qemu_running = true;
    
    // Simulate the engine blocking while running
    while (is_qemu_running) {
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
    }
    
    return 0; 
#endif
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_engine_QemuNative_stopQemu(JNIEnv* env, jobject /* this */) {
    LOGI("Sending stop signal to QEMU Engine");
    is_qemu_running = false;
    // In a real implementation, qemu_system_powerdown() would be called here.
}
