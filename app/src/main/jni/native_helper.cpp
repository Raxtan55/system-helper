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
namespace Off {
    // PlayerControl field offsets
    constexpr uintptr_t PlayerId = 0x35;
    constexpr uintptr_t Data = 0x38;  // NetworkedPlayerInfo

    // NetworkedPlayerInfo field offsets
    constexpr uintptr_t PlayerName = 0x40;  // string
    constexpr uintptr_t RoleType = 0x50;    // int
    constexpr uintptr_t IsDead = 0x78;      // bool
    constexpr uintptr_t DefaultOutfit = 0x20; // PlayerOutfit

    // PlayerOutfit field offsets
    constexpr uintptr_t ColorId = 0x10;     // int

    // Static field offsets (from class start)
    constexpr uintptr_t LocalPlayer = 0x0;
    constexpr uintptr_t AllPlayerControls = 0x8;
}

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
    LOGD("getPlayerList called, base=%p", (void*)base);
    if (!base) return nullptr;

    jclass cls = env->FindClass("com/systemhelper/NativeHelper$PlayerInfo");

    // AllPlayerControls static field'ini oku
    // Static field'lar class'in baslangic adresinde saklanir
    // Il2CppClass yapisi: +0x0 klass, +0x8 monitor, ... +0xB8 static_fields
    uintptr_t playerControlClassAddr = base + 0x4913B28;  // PlayerControl class metadata offset
    uintptr_t staticFields = read<uintptr_t>(playerControlClassAddr + 0xB8);
    LOGD("staticFields=%p", (void*)staticFields);

    if (!staticFields) {
        LOGD("staticFields null, returning test");
        jobjectArray arr = env->NewObjectArray(1, cls, nullptr);
        jobject obj = env->AllocObject(cls);
        env->SetObjectField(obj, env->GetFieldID(cls, "name", "Ljava/lang/String;"), env->NewStringUTF("Test Player"));
        env->SetObjectField(obj, env->GetFieldID(cls, "colorName", "Ljava/lang/String;"), env->NewStringUTF("Mavi"));
        env->SetIntField(obj, env->GetFieldID(cls, "role", "I"), 0);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isDead", "Z"), JNI_FALSE);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isImpostor", "Z"), JNI_FALSE);
        env->SetObjectArrayElement(arr, 0, obj);
        return arr;
    }

    // AllPlayerControls List<PlayerControl>
    uintptr_t allPlayersList = read<uintptr_t>(staticFields + Off::AllPlayerControls);
    LOGD("allPlayersList=%p", (void*)allPlayersList);

    if (!allPlayersList) {
        LOGD("allPlayersList null, returning test");
        jobjectArray arr = env->NewObjectArray(1, cls, nullptr);
        jobject obj = env->AllocObject(cls);
        env->SetObjectField(obj, env->GetFieldID(cls, "name", "Ljava/lang/String;"), env->NewStringUTF("Test Player"));
        env->SetObjectField(obj, env->GetFieldID(cls, "colorName", "Ljava/lang/String;"), env->NewStringUTF("Mavi"));
        env->SetIntField(obj, env->GetFieldID(cls, "role", "I"), 0);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isDead", "Z"), JNI_FALSE);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isImpostor", "Z"), JNI_FALSE);
        env->SetObjectArrayElement(arr, 0, obj);
        return arr;
    }

    // List._items ve List._size
    uintptr_t items = read<uintptr_t>(allPlayersList + 0x10);
    int count = read<int32_t>(allPlayersList + 0x18);
    LOGD("list items=%p count=%d", (void*)items, count);

    if (!items || count <= 0 || count > 15) {
        LOGD("invalid list, returning test");
        jobjectArray arr = env->NewObjectArray(1, cls, nullptr);
        jobject obj = env->AllocObject(cls);
        env->SetObjectField(obj, env->GetFieldID(cls, "name", "Ljava/lang/String;"), env->NewStringUTF("Test Player"));
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

        // PlayerControl.Data
        uintptr_t data = read<uintptr_t>(pc + Off::Data);
        if (!data) continue;

        // PlayerName
        uintptr_t namePtr = read<uintptr_t>(data + Off::PlayerName);
        std::string name = readStr(namePtr);

        // RoleType
        int role = read<int>(data + Off::RoleType);

        // IsDead
        bool dead = read<bool>(data + Off::IsDead);

        // DefaultOutfit -> ColorId
        uintptr_t outfit = read<uintptr_t>(data + Off::DefaultOutfit);
        int color = outfit ? read<int>(outfit + Off::ColorId) : 0;

        LOGD("player[%d]: name=%s role=%d color=%d dead=%d", i, name.c_str(), role, color, dead);

        jobject obj = env->AllocObject(cls);
        env->SetObjectField(obj, env->GetFieldID(cls, "name", "Ljava/lang/String;"), env->NewStringUTF(name.c_str()));
        env->SetObjectField(obj, env->GetFieldID(cls, "colorName", "Ljava/lang/String;"), env->NewStringUTF(cName(color)));
        env->SetIntField(obj, env->GetFieldID(cls, "role", "I"), role);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isDead", "Z"), dead);
        env->SetBooleanField(obj, env->GetFieldID(cls, "isImpostor", "Z"), role==1||role==5||role==7||role==9||role==18);
        env->SetObjectArrayElement(arr, i, obj);
    }

    LOGD("returning %d players", count);
    return arr;
}

JNIEXPORT jlong JNICALL
Java_com_systemhelper_NativeHelper_getModuleBase(JNIEnv*, jclass) {
    return (jlong)base;
}

}
