#ifndef _ARDISCOVERY_AVAHIDISCOVERY_PRIVATE_H_
#define _ARDISCOVERY_AVAHIDISCOVERY_PRIVATE_H_

#include <avahi-client/client.h>
#include <avahi-client/publish.h>

#include <avahi-common/alternative.h>
#include <avahi-common/simple-watch.h>
#include <avahi-common/malloc.h>
#include <avahi-common/error.h>
#include <avahi-common/timeval.h>

#include <libARDiscovery/ARDISCOVERY_AvahiDiscovery.h>

/**
 * @brief Service strings lengths
 */
#define ARDISCOVERY_AVAHIDISCOVERY_SERVICENAME_SIZE 64
#define ARDISCOVERY_AVAHIDISCOVERY_SERVICETYPE_SIZE 64

/**
 * @brief structure to allow service data sharing across discovery process
 */
typedef struct ARDISCOVERY_AvahiDiscovery_ServiceData_t
{
    uint8_t* serviceName;           // Specific to each device ("ARDrone_21452365")
    uint8_t* serviceType;           // Specific to each platform ("_ardrone3._ucp")
    uint32_t devicePort;            // Port advertised by device
    AvahiEntryGroup *entryGroup;    // Avahi entry group
    AvahiSimplePoll *simplePoll;    // Avahi simple poll

} ARDISCOVERY_AvahiDiscovery_ServiceData_t;

#endif /* _ARDISCOVERY_AVAHIDISCOVERY_PRIVATE_H_ */
