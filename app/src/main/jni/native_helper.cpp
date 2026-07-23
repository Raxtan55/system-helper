#include <jni.h>
#include <string>
#include <cstring>
#include <dlfcn.h>
#include <android/log.h>

#define LOG_TAG "Helper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static uintptr_t base = 0;
static void* il2cppHandle = nullptr;

// IL2CPP API function pointers
static void* (*p_il2cpp_domain_get)() = nullptr;
static void** (*p_il2cpp_domain_get_assemblies)(void* domain, size_t* size) = nullptr;
static void* (*p_il2cpp_assembly_get_image)(void* assembly) = nullptr;
static void* (*p_il2cpp_class_from_name)(void* image, const char* namespaze, const char* name) = nullptr;
static void* (*p_il2cpp_class_get_field_from_name)(void* klass, const char* name) = nullptr;
static void (*p_il2cpp_field_static_get_value)(void* field, void* value) = nullptr;
static const char* (*p_il2cpp_image_get_name)(void* image) = nullptr;

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

bool initIL2CPP() {
    il2cppHandle = dlopen("libil2cpp.so", RTLD_LAZY);
    if (!il2cppHandle) {
        LOGD("dlopen failed: %s", dlerror());
        return false;
    }
    LOGD("libil2cpp.so loaded");

    p_il2cpp_domain_get = (void* (*)())dlsym(il2cppHandle, "il2cpp_domain_get");
    p_il2cpp_domain_get_assemblies = (void** (*)(void*, size_t*))dlsym(il2cppHandle, "il2cpp_domain_get_assemblies");
    p_il2cpp_assembly_get_image = (void* (*)(void*))dlsym(il2cppHandle, "il2cpp_assembly_get_image");
    p_il2cpp_class_from_name = (void* (*)(void*, const char*, const char*))dlsym(il2cppHandle, "il2cpp_class_from_name");
    p_il2cpp_class_get_field_from_name = (void* (*)(void*, const char*))dlsym(il2cppHandle, "il2cpp_class_get_field_from_name");
    p_il2cpp_field_static_get_value = (void (*)(void*, void*))dlsym(il2cppHandle, "il2cpp_field_static_get_value");
    p_il2cpp_image_get_name = (const char* (*)(void*))dlsym(il2cppHandle, "il2cpp_image_get_name");

    LOGD("domain_get=%p", p_il2cpp_domain_get);
    LOGD("class_from_name=%p", p_il2cpp_class_from_name);
    LOGD("field_static_get_value=%p", p_il2cpp_field_static_get_value);

    return p_il2cpp_domain_get && p_il2cpp_class_from_name && p_il2cpp_field_static_get_value;
}

void* findAssemblyCSharp() {
    if (!p_il2cpp_domain_get || !p_il2cpp_domain_get_assemblies || !p_il2cpp_assembly_get_image) {
        LOGD("API not ready");
        return nullptr;
    }

    size_t count;
    void** assemblies = p_il2cpp_domain_get_assemblies(p_il2cpp_domain_get(), &count);
    LOGD("assemblies count=%zu", count);

    for (size_t i = 0; i < count; i++) {
        void* image = p_il2cpp_assembly_get_image(assemblies[i]);
        if (image && p_il2cpp_image_get_name) {
            const char* name = p_il2cpp_image_get_name(image);
            LOGD("assembly[%zu]=%s", i, name ? name : "null");
            if (name && strstr(name, "Assembly-CSharp")) {
                LOGD("Found Assembly-CSharp at %zu", i);
                return image;
            }
        }
    }
    return nullptr;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_systemhelper_NativeHelper_init(JNIEnv* env, jclass, jlong addr) {
    base = (uintptr_t)addr;
    LOGD("init base=%p", (void*)base);

    if (initIL2CPP()) {
        LOGD("IL2CPP API initialized");
        void* image = findAssemblyCSharp();
        if (image) {
            LOGD("Assembly-CSharp found: %p", image);
        } else {
            LOGD("Assembly-CSharp not found");
        }
    } else {
        LOGD("IL2CPP API init failed");
    }

    return base != 0;
}

JNIEXPORT jboolean JNICALL
Java_com_systemhelper_NativeHelper_isGameRunning(JNIEnv*, jclass) {
    return base != 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobjectArray JNICALL
Java_com_systemhelper_NativeHelper_getPlayerList(JNIEnv* env, jclass) {
    LOGD("getPlayerList called");
    if (!base) return nullptr;

    jclass cls = env->FindClass("com/systemhelper/NativeHelper$PlayerInfo");

    // Assembly-CSharp image'ini bul
    void* image = findAssemblyCSharp();
    if (!image) {
        LOGD("Assembly-CSharp not found, returning test");
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

    // PlayerControl class'ini bul
    void* playerControlClass = p_il2cpp_class_from_name(image, "", "PlayerControl");
    LOGD("PlayerControl class=%p", playerControlClass);

    if (!playerControlClass) {
        LOGD("PlayerControl class not found, returning test");
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

    // AllPlayerControls field'ini bul
    void* allPlayersField = p_il2cpp_class_get_field_from_name(playerControlClass, "AllPlayerControls");
    LOGD("AllPlayerControls field=%p", allPlayersField);

    if (!allPlayersField) {
        LOGD("AllPlayerControls field not found, returning test");
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

    // Static field degerini oku
    void* allPlayersList = nullptr;
    p_il2cpp_field_static_get_value(allPlayersField, &allPlayersList);
    LOGD("allPlayersList=%p", allPlayersList);

    if (!allPlayersList) {
        LOGD("allPlayersList is null, returning test");
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

    // List size ve items oku
    uintptr_t listAddr = (uintptr_t)allPlayersList;
    void* items = read<void*>(listAddr + 0x10);
    int count = read<int32_t>(listAddr + 0x18);
    LOGD("list items=%p count=%d", items, count);

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
        uintptr_t pc = read<uintptr_t>((uintptr_t)items + 0x20 + i * 8);
        LOGD("player[%d] pc=%p", i, (void*)pc);
        if (!pc) continue;

        // PlayerControl.Data (offset 0x38)
        uintptr_t data = read<uintptr_t>(pc + 0x38);
        LOGD("player[%d] data=%p", i, (void*)data);
        if (!data) continue;

        // PlayerName (offset 0x40)
        uintptr_t namePtr = read<uintptr_t>(data + 0x40);
        std::string name = readStr(namePtr);
        LOGD("player[%d] name=%s", i, name.c_str());

        // RoleType (offset 0x50)
        int role = read<int>(data + 0x50);
        LOGD("player[%d] role=%d", i, role);

        // IsDead (offset 0x78)
        bool dead = read<bool>(data + 0x78);

        // DefaultOutfit (offset 0x20)
        uintptr_t outfit = read<uintptr_t>(data + 0x20);
        int color = outfit ? read<int>(outfit + 0x10) : 0;
        LOGD("player[%d] color=%d", i, color);

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
