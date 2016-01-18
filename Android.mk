LOCAL_PATH := $(call my-dir)

# JNI Wrapper
include $(CLEAR_VARS)

LOCAL_CFLAGS := -g -I$(LOCAL_PATH)/Sources
ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    LOCAL_CFLAGS += -mfloat-abi=softfp -mfpu=neon
endif
LOCAL_MODULE := libardiscovery_android
LOCAL_SRC_FILES := JNI/c/ARDISCOVERY_JNI_Connection.c JNI/c/ARDISCOVERY_JNI_Device.c JNI/c/ARDISCOVERY_JNI_Discovery.c JNI/c/ARDISCOVERY_JNI_DEVICE_Ble.c
LOCAL_LDLIBS := -llog -lz
LOCAL_SHARED_LIBRARIES := libARDiscovery-prebuilt json-prebuilt libARSAL-prebuilt libARNetworkAL-prebuilt
include $(BUILD_SHARED_LIBRARY)
