LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CATEGORY_PATH := dragon/libs
LOCAL_MODULE := libARDiscovery
LOCAL_DESCRIPTION := ARSDK Discovery and Connection Management Layer

LOCAL_LIBRARIES := ARSDKBuildUtils libARSAL libARNetwork libARNetworkAL avahi json
LOCAL_EXPORT_LDLIBS := -lardiscovery

# Copy in build dir so bootstrap files are generated in build dir
LOCAL_AUTOTOOLS_COPY_TO_BUILD_DIR := 1

# Configure script is not at the root
LOCAL_AUTOTOOLS_CONFIGURE_SCRIPT := Build/configure

#Autotools variables
LOCAL_AUTOTOOLS_CONFIGURE_ARGS := \
	--with-libARSALInstallDir="" \
	--with-jsonInstallDir="" \
	--with-libARNetworkInstallDir="" \
	--with-libARNetworkALInstallDir=""

ifneq ("$(TARGET_OS_FLAVOUR)","native")
  LOCAL_AUTOTOOLS_CONFIGURE_ARGS += --enable-avahi-nodbus
  LOCAL_AUTOTOOLS_CONFIGURE_ENV := ac_cv_header_avahi_client_client_h=no
endif

# User define command to be launch before configure step.
# Generates files used by configure
define LOCAL_AUTOTOOLS_CMD_POST_UNPACK
	$(Q) cd $(PRIVATE_SRC_DIR)/Build && ./bootstrap
endef

include $(BUILD_AUTOTOOLS)
