###############################################################################
# THE LIBRARIES TO BUILD
###############################################################################
ACLOCAL_AMFLAGS                                     =   -I m4


# Common part (Negociation and ARNetwork creation ...)
HEADER_FILES                                        =   ../Includes/libARDiscovery/ARDiscovery.h \
                                                        ../Includes/libARDiscovery/ARDISCOVERY_Connection.h \
                                                        ../Includes/libARDiscovery/ARDISCOVERY_Discovery.h \
                                                        ../Includes/libARDiscovery/ARDISCOVERY_NetworkConfiguration.h \
                                                        ../Includes/libARDiscovery/ARDISCOVERY_Device.h \
                                                        ../Includes/libARDiscovery/ARDISCOVERY_Error.h
SOURCE_FILES                                        =   ../Sources/ARDISCOVERY_Connection.c \
                                                        ../Sources/ARDISCOVERY_Discovery.c \
                                                        ../Sources/ARDISCOVERY_Device.c \
                                                        ../Sources/Wifi/ARDISCOVERY_DEVICE_Wifi.h \
                                                        ../Sources/Wifi/ARDISCOVERY_DEVICE_Wifi.c \
                                                        ../Sources/BLE/ARDISCOVERY_DEVICE_Ble.h \
                                                        ../Sources/BLE/ARDISCOVERY_DEVICE_Ble.c \
                                                        ../gen/Sources/ARDISCOVERY_Error.c
# $(HEADER_FILES) will be added to $(SOURCE_FILES) later, in the discovery parts

if AVAHI_NODBUS # Embedded case: dont use DBUS to communicate with avahi daemon
HEADER_FILES                                        +=  ../Includes/libARDiscovery/ARDISCOVERY_AvahiDiscovery.h
SOURCE_FILES                                        +=  $(HEADER_FILES) \
                                                        ../Sources/ARDISCOVERY_AvahiDiscovery_nodbus.c \
                                                        ../Sources/ARDISCOVERY_AvahiDiscovery_nodbus.h
else
if HAVE_BONJOUR # Bonjour specific files (iOS/OS X)
HEADER_FILES                                        +=  ../Includes/libARDiscovery/ARDISCOVERY_BonjourDiscovery.h
SOURCE_FILES                                        +=  $(HEADER_FILES) \
                                                        ../Sources/ARDISCOVERY_BonjourDiscovery.m
else
if HAVE_AVAHI # Avahi specific file (Linux)
HEADER_FILES                                        +=  ../Includes/libARDiscovery/ARDISCOVERY_AvahiDiscovery.h
SOURCE_FILES                                        +=  $(HEADER_FILES) \
                                                        ../Sources/ARDISCOVERY_AvahiDiscovery.c
else # Nether Bonjour nor Avahi (Android) --> Discovery is done by external files
SOURCE_FILES                                        +=  $(HEADER_FILES)
endif
endif
endif

# The library names to build (note we are building static and shared libs)
if DEBUG_MODE
lib_LTLIBRARIES                                     =   libardiscovery_dbg.la
libardiscovery_dbg_ladir                            =   $(includedir)/libARDiscovery
libardiscovery_dbg_la_HEADERS                       =   $(HEADER_FILES)
libardiscovery_dbg_la_SOURCES                       =   $(SOURCE_FILES)
else
lib_LTLIBRARIES                                     =   libardiscovery.la
libardiscovery_ladir                                =   $(includedir)/libARDiscovery
libardiscovery_la_HEADERS                           =   $(HEADER_FILES)
libardiscovery_la_SOURCES                           =   $(SOURCE_FILES)
endif

CLEAN_FILES                                         =   libardiscovery.la       \
                                                        libardiscovery_dbg.la

EXTRA_DIST                                          =   bootstrap

# Ensure the distribution of the doxygen configuration file
# Doxygen support
if HAVE_DOXYGEN
include doxygen.am
EXTRA_DIST                                          +=  doxygen.ac  \
                                                        doxygen.am  \
                                                        $(DX_CONFIG)
endif
