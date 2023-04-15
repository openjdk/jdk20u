#include "jni.h"
#include "jvm.h"

#include "java_lang_AdaptiveThreadFactory.h"

#define ARRAY_LENGTH(a) (sizeof(a)/sizeof(a[0]))

static JNINativeMethod methods[] = {
    {"adaptiveThreadFactoryTest", "()I", (void *)&JVM_AdaptiveThreadFactoryTest},
};

JNIEXPORT void JNICALL
Java_java_lang_AdaptiveThreadFactory_registerNatives(JNIEnv *env, jclass cls)
{
    (*env)->RegisterNatives(env, cls, methods, ARRAY_LENGTH(methods));
}