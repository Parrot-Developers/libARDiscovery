#ifndef _ARDISCOVERY_AVAHIDISCOVERY_PRIVATE_H_
#define _ARDISCOVERY_AVAHIDISCOVERY_PRIVATE_H_

#include <avahi-client/client.h>
#include <avahi-client/publish.h>
#include <avahi-client/lookup.h>

#include <avahi-common/alternative.h>
#include <avahi-common/simple-watch.h>
#include <avahi-common/malloc.h>
#include <avahi-common/error.h>

#include <libARDiscovery/ARDISCOVERY_AvahiDiscovery.h>
#include <libARDiscovery/ARDISCOVERY_Discovery.h>

/**
 * @brief Service strings lengths
 */
#define ARDISCOVERY_AVAHIDISCOVERY_SERVICENAME_SIZE 64
#define ARDISCOVERY_AVAHIDISCOVERY_SERVICETYPE_SIZE 64

/**
 * @brief structure to allow service data sharing across publishing process
 */
struct ARDISCOVERY_AvahiDiscovery_PublisherData_t
{
    uint8_t* serviceName;           // Specific to each device ("ARDrone_21452365")
    uint8_t* serviceType;           // Specific to each platform ("_ardrone3._ucp")
    uint32_t devicePort;            // Port advertised by device
    AvahiClient *client;            // Avahi client
    AvahiEntryGroup *entryGroup;    // Avahi entry group
    AvahiSimplePoll *simplePoll;    // Avahi simple poll
};

/**
 * @brief structure to allow service data sharing across browsing process
 */
struct ARDISCOVERY_AvahiDiscovery_BrowserData_t
{
    uint8_t** serviceTypes;         // Service types to browse for
    uint8_t serviceTypesNb;         // Number of service types to browse for
    AvahiServiceBrowser* serviceBrowsers[ARDISCOVERY_AVAHIDISCOVERY_SERVICE_NB_MAX]; // Avahi service browsers
    AvahiClient *client;            // Avahi client
    AvahiSimplePoll *simplePoll;    // Avahi simple poll
    ARDISCOVERY_AvahiDiscovery_Browser_Callback_t callback; // Service browsing callback
    void* customData;               // Custom data to forward to callback
};

#endif /* _ARDISCOVERY_AVAHIDISCOVERY_PRIVATE_H_ */
