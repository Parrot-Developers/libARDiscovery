LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CATEGORY_PATH := dragon/libs
LOCAL_MODULE := libARDiscovery
LOCAL_DESCRIPTION := ARSDK Discovery and Connection Management Layer

LOCAL_LIBRARIES := ARSDKBuildUtils libARSAL libARNetwork libARNetworkAL json

ifndef ARSDK_BUILD_FOR_APP
LOCAL_LIBRARIES += avahi
endif

LOCAL_EXPORT_LDLIBS := -lardiscovery

# Copy in build dir so bootstrap files are generated in build dir
LOCAL_AUTOTOOLS_COPY_TO_BUILD_DIR := 1

# Configure script is not at the root
LOCAL_AUTOTOOLS_CONFIGURE_SCRIPT := Build/configure

#Autotools variables
LOCAL_AUTOTOOLS_CONFIGURE_ARGS := \
	--with-libARSALInstallDir="" \
	--with-libARNetworkALInstallDir="" \
	--with-libARNetworkInstallDir="" \
	--with-jsonInstallDir=""

ifeq ("$(TARGET_OS_FLAVOUR)","android")

LOCAL_AUTOTOOLS_CONFIGURE_ARGS += \
	--disable-static \
	--enable-shared \
	--disable-so-version

# Temporary fix to Android build on Mac
LOCAL_AUTOTOOLS_CONFIGURE_ENV := \
	ac_cv_objc_compiler_gnu=no

else ifneq ($(filter iphoneos iphonesimulator, $(TARGET_OS_FLAVOUR)),)

LOCAL_AUTOTOOLS_CONFIGURE_ARGS += \
	--enable-static \
	--disable-shared \
	OBJCFLAGS=" -x objective-c -fobjc-arc -std=gnu99 $(TARGET_GLOBAL_CFLAGS)" \
	OBJC="$(TARGET_CC)" \
	CFLAGS=" -std=gnu99 -x c $(TARGET_GLOBAL_CFLAGS)"

else ifneq ("$(TARGET_OS_FLAVOUR)","native")
  LOCAL_AUTOTOOLS_CONFIGURE_ARGS += --enable-avahi-nodbus
  LOCAL_AUTOTOOLS_CONFIGURE_ENV := ac_cv_header_avahi_client_client_h=no

endif

# User define command to be launch before configure step.
# Generates files used by configure
define LOCAL_AUTOTOOLS_CMD_POST_UNPACK
	$(Q) cd $(PRIVATE_SRC_DIR)/Build && ./bootstrap
endef

include $(BUILD_AUTOTOOLS)
