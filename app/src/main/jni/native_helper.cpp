#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "Helper"

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_systemhelper_NativeHelper_init(JNIEnv *env, jclass clazz, jlong addr) {
    return addr != 0;
}

JNIEXPORT jboolean JNICALL
Java_com_systemhelper_NativeHelper_isGameRunning(JNIEnv *env, jclass clazz) {
    return JNI_FALSE;
}

JNIEXPORT jobjectArray JNICALL
Java_com_systemhelper_NativeHelper_getPlayerList(JNIEnv *env, jclass clazz) {
    return nullptr;
}

JNIEXPORT jlong JNICALL
Java_com_systemhelper_NativeHelper_getModuleBase(JNIEnv *env, jclass clazz) {
    return 0;
}

}
