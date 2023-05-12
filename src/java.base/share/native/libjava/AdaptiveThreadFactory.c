#include "jni.h"
#include "jvm.h"

#include "java_lang_AdaptiveThreadFactory.h"

#define ARRAY_LENGTH(a) (sizeof(a)/sizeof(a[0]))

static JNINativeMethod methods[] = {
    {"addMonitor", "(I)V", (void*)&JVM_AddMonitor},
    {"removeMonitor", "(I)V", (void*)&JVM_RemoveMonitor},
    {"setMonitorParameters", "(IJJJJ)V", (void*)&JVM_SetMonitorParameters},
    {"queryMonitor", "(I)Z", (void*)&JVM_QueryMonitor},
    {"registerJavaThreadAndAssociateOSThreadWithMonitor", "(IJ)V", (void*)&JVM_RegisterWithMonitor},
    {"deregisterJavaThreadAndDisassociateOSThreadFromMonitor", "(IJ)V", (void*)&JVM_DeregisterFromMonitor},
    {"associateOSThreadWithMonitor", "(IJ)V", (void*)&JVM_AssociateWithMonitor},
    {"disassociateOSThreadFromMonitor", "(IJ)V", (void*)&JVM_DisassociateFromMonitor},
};

JNIEXPORT void JNICALL
Java_java_lang_AdaptiveThreadFactory_registerNatives(JNIEnv *env, jclass cls)
{
    (*env)->RegisterNatives(env, cls, methods, ARRAY_LENGTH(methods));
}