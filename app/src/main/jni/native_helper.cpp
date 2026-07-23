#include <jni.h>
#include <string>
#include <cstring>
#include <android/log.h>

#define LOG_TAG "Helper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

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

// Among Us offsetleri (dump.cs'den)
// PlayerControl_StaticFields: base + 0x4913B28
// StaticFields + 0x0 = LocalPlayer
// StaticFields + 0x8 = AllPlayerControls
// PlayerControl.Data: 0x38
// NetworkedPlayerInfo.PlayerName: 0x40
// NetworkedPlayerInfo.RoleType: 0x50
// NetworkedPlayerInfo.IsDead: 0x78
// NetworkedPlayerInfo.DefaultOutfit: 0x20
// PlayerOutfit.ColorId: 0x10

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_systemhelper_NativeHelper_init(JNIEnv* env, jclass, jlong addr) {
    base = (uintptr_t)addr;
    LOGD("init base=%p", (void*)base);
    return base != 0;
}

JNIEXPORT jboolean JNICALL
Java_com_systemhelper_NativeHelper_isGameRunning(JNIEnv*, jclass) {
    return base != 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobjectArray JNICALL
Java_com_systemhelper_NativeHelper_getPlayerList(JNIEnv* env, jclass) {
    if (!base) return nullptr;

    jclass cls = env->FindClass("com/systemhelper/NativeHelper$PlayerInfo");

    // PlayerControl_StaticFields
    uintptr_t staticFields = read<uintptr_t>(base + 0x4913B28 + 0xB8);
    LOGD("staticFields=%p", (void*)staticFields);

    if (!staticFields) {
        // Test dondur
        jobjectArray arr = env->NewObjectArray(1, cls, nullptr);
        jobject obj = env->AllocObject(cls);
        env->SetObjectField(obj, env->GetFieldID(cls, "name", "Ljava/lang/String;"), env->NewStringUTF("Test"));
        env->SetObjectField(obj, env->GetFieldID(cls, "colorName", "Ljava/lang/String;"), env->NewStringUTF("Mavi"));
        env->SetIntField(obj, env->GetFieldID(cls, "role", "I"), 0);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isDead", "Z"), JNI_FALSE);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isImpostor", "Z"), JNI_FALSE);
        env->SetObjectArrayElement(arr, 0, obj);
        return arr;
    }

    // AllPlayerControls
    uintptr_t allPlayers = read<uintptr_t>(staticFields + 0x8);
    LOGD("allPlayers=%p", (void*)allPlayers);

    if (!allPlayers) {
        jobjectArray arr = env->NewObjectArray(1, cls, nullptr);
        jobject obj = env->AllocObject(cls);
        env->SetObjectField(obj, env->GetFieldID(cls, "name", "Ljava/lang/String;"), env->NewStringUTF("Test"));
        env->SetObjectField(obj, env->GetFieldID(cls, "colorName", "Ljava/lang/String;"), env->NewStringUTF("Mavi"));
        env->SetIntField(obj, env->GetFieldID(cls, "role", "I"), 0);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isDead", "Z"), JNI_FALSE);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isImpostor", "Z"), JNI_FALSE);
        env->SetObjectArrayElement(arr, 0, obj);
        return arr;
    }

    // List._items ve List._size
    uintptr_t items = read<uintptr_t>(allPlayers + 0x10);
    int count = read<int32_t>(allPlayers + 0x18);
    LOGD("items=%p count=%d", (void*)items, count);

    if (!items || count <= 0 || count > 15) {
        jobjectArray arr = env->NewObjectArray(1, cls, nullptr);
        jobject obj = env->AllocObject(cls);
        env->SetObjectField(obj, env->GetFieldID(cls, "name", "Ljava/lang/String;"), env->NewStringUTF("Test"));
        env->SetObjectField(obj, env->GetFieldID(cls, "colorName", "Ljava/lang/String;"), env->NewStringUTF("Mavi"));
        env->SetIntField(obj, env->GetFieldID(cls, "role", "I"), 0);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isDead", "Z"), JNI_FALSE);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isImpostor", "Z"), JNI_FALSE);
        env->SetObjectArrayElement(arr, 0, obj);
        return arr;
    }

    jobjectArray arr = env->NewObjectArray(count, cls, nullptr);

    for (int i = 0; i < count; i++) {
        uintptr_t pc = read<uintptr_t>(items + 0x20 + i * 8);
        if (!pc) continue;

        // Data
        uintptr_t data = read<uintptr_t>(pc + 0x38);
        if (!data) continue;

        // PlayerName
        uintptr_t namePtr = read<uintptr_t>(data + 0x40);
        std::string name = namePtr ? readStr(namePtr) : "Unknown";

        // RoleType
        int role = read<int>(data + 0x50);

        // IsDead
        bool dead = read<bool>(data + 0x78);

        // DefaultOutfit -> ColorId
        uintptr_t outfit = read<uintptr_t>(data + 0x20);
        int color = outfit ? read<int>(outfit + 0x10) : 0;

        LOGD("[%d] name=%s role=%d color=%d", i, name.c_str(), role, color);

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
