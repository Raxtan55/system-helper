#include <jni.h>
#include <string>
#include <cstring>
#include <dlfcn.h>
#include <android/log.h>

#define LOG_TAG "Helper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static uintptr_t base = 0;

// IL2CPP API types
typedef void* Il2CppClass;
typedef void* FieldInfo;
typedef void* Il2CppImage;

// IL2CPP API functions
static Il2CppClass* (*il2cpp_class_from_name)(Il2CppImage* image, const char* namespaze, const char* name);
static FieldInfo* (*il2cpp_class_get_field_from_name)(Il2CppClass* klass, const char* name);
static void (*il2cpp_field_static_get_value)(FieldInfo* field, void* value);
static Il2CppImage* (*il2cpp_assembly_get_image)(void* assembly);
static void* (*il2cpp_domain_get)();
static void** (*il2cpp_domain_get_assemblies)(void* domain, size_t* size);

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
    void* handle = dlopen("libil2cpp.so", RTLD_LAZY);
    if (!handle) {
        LOGD("dlopen failed: %s", dlerror());
        return false;
    }

    il2cpp_class_from_name = (Il2CppClass* (*)(Il2CppImage*, const char*, const char*))dlsym(handle, "il2cpp_class_from_name");
    il2cpp_class_get_field_from_name = (FieldInfo* (*)(Il2CppClass*, const char*))dlsym(handle, "il2cpp_class_get_field_from_name");
    il2cpp_field_static_get_value = (void (*)(FieldInfo*, void*))dlsym(handle, "il2cpp_field_static_get_value");
    il2cpp_assembly_get_image = (Il2CppImage* (*)(void*))dlsym(handle, "il2cpp_assembly_get_image");
    il2cpp_domain_get = (void* (*)())dlsym(handle, "il2cpp_domain_get");
    il2cpp_domain_get_assemblies = (void** (*)(void*, size_t*))dlsym(handle, "il2cpp_domain_get_assemblies");

    return il2cpp_class_from_name && il2cpp_field_static_get_value;
}

Il2CppImage* findAssembly(const char* name) {
    size_t count;
    void** assemblies = il2cpp_domain_get_assemblies(il2cpp_domain_get(), &count);
    for (size_t i = 0; i < count; i++) {
        Il2CppImage* image = il2cpp_assembly_get_image(assemblies[i]);
        // Assembly adını kontrol et
        if (image) {
            // Basit kontrol - Assembly-CSharp ara
            char* imgName = (char*)image;
            if (strstr(imgName, name)) {
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
    LOGD("getPlayerList called, base=%p", (void*)base);
    if (!base) return nullptr;

    // Test verisi dondur - IL2CPP API hazir olana kadar
    jclass cls = env->FindClass("com/systemhelper/NativeHelper$PlayerInfo");
    jobjectArray arr = env->NewObjectArray(1, cls, nullptr);

    jobject obj = env->AllocObject(cls);
    env->SetObjectField(obj, env->GetFieldID(cls, "name", "Ljava/lang/String;"), env->NewStringUTF("Test Player"));
    env->SetObjectField(obj, env->GetFieldID(cls, "colorName", "Ljava/lang/String;"), env->NewStringUTF("Mavi"));
    env->SetIntField(obj, env->GetFieldID(cls, "role", "I"), 0);
    env->SetBooleanField(obj, env->GetFieldID(cls, "isDead", "Z"), JNI_FALSE);
    env->SetBooleanField(obj, env->GetFieldID(cls, "isImpostor", "Z"), JNI_FALSE);
    env->SetObjectArrayElement(arr, 0, obj);

    LOGD("test player returned");
    return arr;
}

JNIEXPORT jlong JNICALL
Java_com_systemhelper_NativeHelper_getModuleBase(JNIEnv*, jclass) {
    return (jlong)base;
}

}
