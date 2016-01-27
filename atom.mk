LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libARDiscovery
LOCAL_DESCRIPTION := ARSDK Discovery and Connection Management Layer
LOCAL_CATEGORY_PATH := dragon/libs

LOCAL_MODULE_FILENAME := libardiscovery.so

LOCAL_LIBRARIES := \
	libARSAL \
	libARNetwork \
	libARNetworkAL \
	json

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/Includes \
	$(LOCAL_PATH)/Sources

LOCAL_SRC_FILES := \
	Sources/ARDISCOVERY_Connection.c \
	Sources/ARDISCOVERY_Discovery.c \
	Sources/ARDISCOVERY_Device.c \
	Sources/Wifi/ARDISCOVERY_DEVICE_Wifi.c \
	Sources/BLE/ARDISCOVERY_DEVICE_Ble.c \
	gen/Sources/ARDISCOVERY_Error.c

LOCAL_INSTALL_HEADERS := \
	Includes/libARDiscovery/ARDiscovery.h:usr/include/libARDiscovery/ \
	Includes/libARDiscovery/ARDISCOVERY_Connection.h:usr/include/libARDiscovery/ \
	Includes/libARDiscovery/ARDISCOVERY_Discovery.h:usr/include/libARDiscovery/ \
	Includes/libARDiscovery/ARDISCOVERY_NetworkConfiguration.h:usr/include/libARDiscovery/ \
	Includes/libARDiscovery/ARDISCOVERY_Device.h:usr/include/libARDiscovery/ \
	Includes/libARDiscovery/ARDISCOVERY_Error.h:usr/include/libARDiscovery/

ifndef ARSDK_BUILD_FOR_APP

# Embedded: Use avahi without dbus
LOCAL_SRC_FILES += \
	Sources/ARDISCOVERY_AvahiDiscovery_nodbus.c
LOCAL_INSTALL_HEADERS += \
	Includes/libARDiscovery/ARDISCOVERY_AvahiDiscovery.h:usr/include/libARDiscovery/

else ifeq ("$(TARGET_OS)-$(TARGET_OS_FLAVOUR)","linux-native")

# Native linux: use avahi with dbus
LOCAL_LIBRARIES += \
	avahi
LOCAL_SRC_FILES += \
	Sources/ARDISCOVERY_AvahiDiscovery.c
LOCAL_INSTALL_HEADERS += \
	Includes/libARDiscovery/ARDISCOVERY_AvahiDiscovery.h:usr/include/libARDiscovery/

else ifeq ("$(TARGET_OS)","darwin")

# Darwin: use bonjour
LOCAL_SRC_FILES += \
	Sources/ARDISCOVERY_BonjourDiscovery.m
LOCAL_INSTALL_HEADERS += \
	Includes/libARDiscovery/ARDISCOVERY_BonjourDiscovery.h:usr/include/libARDiscovery/
LOCAL_LDLIBS += \
	-framework Foundation \
	-framework CoreBluetooth

endif

include $(BUILD_LIBRARY)
