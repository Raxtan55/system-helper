#include <jni.h>
#include <string>
#include <cstring>
#include <android/log.h>

#define LOG_TAG "Helper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace Cfg {
    constexpr uintptr_t O1 = 0x21B47F8;
    constexpr uintptr_t O2 = 0x8;
    constexpr uintptr_t O4 = 0x2415BDC;
    constexpr uintptr_t O6 = 0x50;
    constexpr uintptr_t O7 = 0x78;
    constexpr uintptr_t O8 = 0x10;
    constexpr uintptr_t S1 = 0x4913778;
    constexpr uintptr_t S2 = 0x4913A90;
    constexpr uintptr_t S3 = 0x4913B28;
}

static uintptr_t base = 0;

template<typename T>
T read(uintptr_t addr) {
    T v;
    memcpy(&v, reinterpret_cast<void*>(addr), sizeof(T));
    return v;
}

std::string readStr(uintptr_t addr) {
    if (!addr) return "";
    int32_t len = read<int32_t>(addr + 0x10);
    if (len <= 0 || len > 1000) return "";
    std::string r;
    for (int i = 0; i < len; i++) {
        char16_t ch = read<char16_t>(addr + 0x14 + i * 2);
        if (!ch) break;
        r += (char)ch;
    }
    return r;
}

const char* cName(int id) {
    switch (id) {
        case 0: return "Kirmizi"; case 1: return "Mavi";
        case 2: return "Yesil"; case 3: return "Pembe";
        case 4: return "Turuncu"; case 5: return "Sari";
        case 6: return "Siyah"; case 7: return "Beyaz";
        case 8: return "Mor"; case 9: return "Kahverengi";
        case 10: return "Cyan"; case 11: return "Lime";
        default: return "?";
    }
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_systemhelper_NativeHelper_init(JNIEnv*, jclass, jlong addr) {
    base = (uintptr_t)addr;
    return base != 0;
}

JNIEXPORT jboolean JNICALL
Java_com_systemhelper_NativeHelper_isGameRunning(JNIEnv*, jclass) {
    if (!base) return JNI_FALSE;
    return read<uintptr_t>(base + Cfg::S1) && read<uintptr_t>(base + Cfg::S2);
}

JNIEXPORT jobjectArray JNICALL
Java_com_systemhelper_NativeHelper_getPlayerList(JNIEnv* env, jclass) {
    if (!base) return nullptr;
    uintptr_t list = read<uintptr_t>(base + Cfg::S3);
    if (!list) return nullptr;

    uintptr_t items = read<uintptr_t>(list + 0x10);
    int count = read<int32_t>(list + 0x18);
    if (!items || count <= 0 || count > 15) return nullptr;

    jclass cls = env->FindClass("com/systemhelper/NativeHelper$PlayerInfo");
    jobjectArray arr = env->NewObjectArray(count, cls, nullptr);

    for (int i = 0; i < count; i++) {
        uintptr_t pc = read<uintptr_t>(items + 0x20 + i * 8);
        if (!pc) continue;
        uintptr_t data = read<uintptr_t>(pc + 0x38);
        if (!data) continue;

        std::string name = readStr(read<uintptr_t>(data + 0x40));
        int role = read<int>(data + Cfg::O6);
        bool dead = read<bool>(data + Cfg::O7);

        uintptr_t outfit = read<uintptr_t>(data + 0x20);
        int color = outfit ? read<int>(outfit + Cfg::O8) : 0;

        jobject obj = env->AllocObject(cls);
        env->SetObjectField(obj, env->GetFieldID(cls, "name", "Ljava/lang/String;"), env->NewStringUTF(name.c_str()));
        env->SetObjectField(obj, env->GetFieldID(cls, "colorName", "Ljava/lang/String;"), env->NewStringUTF(cName(color)));
        env->SetIntField(obj, env->GetFieldID(cls, "role", "I"), role);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isDead", "Z"), dead);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isImpostor", "Z"), role==1||role==5||role==7||role==9||role==18);
        env->SetObjectArrayElement(arr, i, obj);
    }
    return arr;
}

JNIEXPORT jlong JNICALL
Java_com_systemhelper_NativeHelper_getModuleBase(JNIEnv*, jclass) {
    return (jlong)base;
}

}
