LOCAL_PATH := $(call my-dir)

# JNI Wrapper
include $(CLEAR_VARS)

LOCAL_CFLAGS := -g
ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    LOCAL_CFLAGS += -mfloat-abi=softfp -mfpu=neon
endif
LOCAL_MODULE := libardiscovery_android
LOCAL_SRC_FILES := JNI/c/ARDISCOVERY_JNI_Connection.c JNI/c/ARDISCOVERY_JNI_Device.c JNI/c/ARDISCOVERY_JNI_Discovery.c
LOCAL_LDLIBS := -llog -lz
LOCAL_SHARED_LIBRARIES := libARDiscovery-prebuilt json-prebuilt libARSAL-prebuilt
include $(BUILD_SHARED_LIBRARY)
