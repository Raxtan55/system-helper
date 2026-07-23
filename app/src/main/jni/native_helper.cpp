#include <jni.h>
#include <string>
#include <vector>
#include <cstring>
#include <unistd.h>
#include <sys/mman.h>
#include <dlfcn.h>
#include <android/log.h>
#include <random>
#include <chrono>

#define LOG_TAG "SystemHelper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Offsets (obfuscated names)
namespace Config {
    // Main offsets
    constexpr uintptr_t O1 = 0x21B47F8;  // PlayerControl.get_Data
    constexpr uintptr_t O2 = 0x8;        // PlayerControl.AllPlayerControls
    constexpr uintptr_t O3 = 0x0;        // PlayerControl.LocalPlayer
    constexpr uintptr_t O4 = 0x2415BDC;  // NetworkedPlayerInfo.get_PlayerName
    constexpr uintptr_t O5 = 0x24166C4;  // NetworkedPlayerInfo.get_Color
    constexpr uintptr_t O6 = 0x50;       // NetworkedPlayerInfo.RoleType
    constexpr uintptr_t O7 = 0x78;       // NetworkedPlayerInfo.IsDead
    constexpr uintptr_t O8 = 0x10;       // PlayerOutfit.ColorId

    // Static offsets (need to be calculated from base)
    constexpr uintptr_t S1 = 0x4913778;  // AmongUsClient.Instance
    constexpr uintptr_t S2 = 0x4913A90;  // GameManager
    constexpr uintptr_t S3 = 0x4913B28;  // PlayerControl.AllPlayerControls static

    // Role constants
    constexpr int R0 = 0;   // Crewmate
    constexpr int R1 = 1;   // Impostor
    constexpr int R2 = 2;   // Scientist
    constexpr int R3 = 3;   // Engineer
    constexpr int R4 = 4;   // Guardian Angel
    constexpr int R5 = 5;   // Shapeshifter
    constexpr int R6 = 6;   // Crewmate Ghost
    constexpr int R7 = 7;   // Impostor Ghost
    constexpr int R8 = 8;   // Noisemaker
    constexpr int R9 = 9;   // Phantom
    constexpr int R10 = 10; // Tracker
    constexpr int R12 = 12; // Detective
    constexpr int R18 = 18; // Viper
}

static uintptr_t baseAddr = 0;
static std::mt19937 rng(std::chrono::steady_clock::now().time_since_epoch().count());

// Anti-detection: random delay
void safeDelay() {
    std::uniform_int_distribution<int> dist(5, 25);
    usleep(dist(rng) * 1000);
}

// Anti-detection: obfuscated memory read
template<typename T>
T readMem(uintptr_t addr) {
    safeDelay();
    T val;
    memcpy(&val, reinterpret_cast<void*>(addr), sizeof(T));
    return val;
}

// Anti-detection: obfuscated string read
std::string readStr(uintptr_t addr) {
    if (addr == 0) return "";

    safeDelay();
    int32_t len = readMem<int32_t>(addr + 0x10);
    if (len <= 0 || len > 1000) return "";

    std::string result;
    result.reserve(len);

    for (int i = 0; i < len; i++) {
        char16_t ch = readMem<char16_t>(addr + 0x14 + (i * 2));
        if (ch == 0) break;
        result.push_back(static_cast<char>(ch));
    }

    return result;
}

// Anti-detection: obfuscated list access
uintptr_t getListData(uintptr_t listAddr) {
    return readMem<uintptr_t>(listAddr + 0x10);
}

int getListCount(uintptr_t listAddr) {
    return readMem<int32_t>(listAddr + 0x18);
}

const char* colorName(int id) {
    switch (id) {
        case 0: return "Kirmizi";
        case 1: return "Mavi";
        case 2: return "Yesil";
        case 3: return "Pembe";
        case 4: return "Turuncu";
        case 5: return "Sari";
        case 6: return "Siyah";
        case 7: return "Beyaz";
        case 8: return "Mor";
        case 9: return "Kahverengi";
        case 10: return "Cyan";
        case 11: return "Lime";
        case 12: return "Bordo";
        case 13: return "Rose";
        case 14: return "Muz";
        case 15: return "Gri";
        case 16: return "Ten";
        case 17: return "Mercan";
        default: return "?";
    }
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_systemhelper_NativeHelper_init(JNIEnv *env, jclass clazz, jlong addr) {
    baseAddr = static_cast<uintptr_t>(addr);
    LOGD("Initialized: 0x%lx", baseAddr);
    return baseAddr != 0;
}

JNIEXPORT jboolean JNICALL
Java_com_systemhelper_NativeHelper_isGameRunning(JNIEnv *env, jclass clazz) {
    if (baseAddr == 0) return JNI_FALSE;

    try {
        uintptr_t client = readMem<uintptr_t>(baseAddr + Config::S1);
        if (client == 0) return JNI_FALSE;

        uintptr_t manager = readMem<uintptr_t>(baseAddr + Config::S2);
        return manager != 0;
    } catch (...) {
        return JNI_FALSE;
    }
}

JNIEXPORT jobjectArray JNICALL
Java_com_systemhelper_NativeHelper_getPlayerList(JNIEnv *env, jclass clazz) {
    if (baseAddr == 0) return nullptr;

    try {
        uintptr_t list = readMem<uintptr_t>(baseAddr + Config::S3);
        if (list == 0) return nullptr;

        uintptr_t items = getListData(list);
        int count = getListCount(list);

        if (items == 0 || count <= 0 || count > 15) return nullptr;

        jclass infoClass = env->FindClass("com/systemhelper/NativeHelper$PlayerInfo");
        jobjectArray result = env->NewObjectArray(count, infoClass, nullptr);

        for (int i = 0; i < count; i++) {
            uintptr_t player = readMem<uintptr_t>(items + 0x20 + (i * 0x8));
            if (player == 0) continue;

            uintptr_t data = readMem<uintptr_t>(player + 0x38);
            if (data == 0) continue;

            uintptr_t namePtr = readMem<uintptr_t>(data + 0x40);
            std::string name = readStr(namePtr);

            int role = readMem<int>(data + Config::O6);
            bool dead = readMem<bool>(data + Config::O7);

            uintptr_t outfit = readMem<uintptr_t>(data + 0x20);
            int color = 0;
            if (outfit != 0) {
                color = readMem<int>(outfit + Config::O8);
            }

            jobject obj = env->AllocObject(infoClass);

            jfieldID fName = env->GetFieldID(infoClass, "name", "Ljava/lang/String;");
            jfieldID fColor = env->GetFieldID(infoClass, "colorName", "Ljava/lang/String;");
            jfieldID fRole = env->GetFieldID(infoClass, "role", "I");
            jfieldID fDead = env->GetFieldID(infoClass, "isDead", "Z");
            jfieldID fImp = env->GetFieldID(infoClass, "isImpostor", "Z");

            env->SetObjectField(obj, fName, env->NewStringUTF(name.c_str()));
            env->SetObjectField(obj, fColor, env->NewStringUTF(colorName(color)));
            env->SetIntField(obj, fRole, role);
            env->SetBooleanField(obj, fDead, dead ? JNI_TRUE : JNI_FALSE);
            env->SetBooleanField(obj, fImp,
                                 (role == 1 || role == 5 || role == 7 ||
                                  role == 9 || role == 18) ? JNI_TRUE : JNI_FALSE);

            env->SetObjectArrayElement(result, i, obj);
        }

        return result;
    } catch (const std::exception& e) {
        LOGE("Error: %s", e.what());
        return nullptr;
    }
}

JNIEXPORT jlong JNICALL
Java_com_systemhelper_NativeHelper_getModuleBase(JNIEnv *env, jclass clazz) {
    return static_cast<jlong>(baseAddr);
}

}
