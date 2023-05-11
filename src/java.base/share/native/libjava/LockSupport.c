#include "jni.h"
#include "jvm.h"

#include "java_util_concurrent_locks_LockSupport.h"

#define ARRAY_LENGTH(a) (sizeof(a)/sizeof(a[0]))

static JNINativeMethod methods[] = {
    {"recordParking", "(I)V", (void*)&JVM_RecordParking},
};

JNIEXPORT void JNICALL
Java_java_util_concurrent_locks_LockSupport_registerNatives(JNIEnv *env, jclass cls)
{
    (*env)->RegisterNatives(env, cls, methods, ARRAY_LENGTH(methods));
}