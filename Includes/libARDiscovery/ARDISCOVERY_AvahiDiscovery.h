#ifndef _ARDISCOVERY_AVAHIDISCOVERY_H_
#define _ARDISCOVERY_AVAHIDISCOVERY_H_

#include <avahi-client/client.h>
#include <avahi-client/publish.h>

#include <avahi-common/alternative.h>
#include <avahi-common/simple-watch.h>
#include <avahi-common/malloc.h>
#include <avahi-common/error.h>
#include <avahi-common/timeval.h>

#define ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_NETWORK "local"
#define ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_PUBLISHED_PORT 43210
#define ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_CONTROLLER_PORT 54321

/**
 * @brief structure to allow service data sharing across discovery process
 */
typedef struct ARDISCOVERY_AvahiDiscovery_ServiceData_t
{
    char* serviceName;              // Specific to each device ("ARDrone_21452365")
    char* serviceType;              // Specific to each platform ("_ardrone3._tcp")
    int devicePort;                 // Port advertised by device
    AvahiEntryGroup *entryGroup;    // Avahi entry group
    AvahiSimplePoll *simplePoll;    // Avahi simple poll

} ARDISCOVERY_AvahiDiscovery_ServiceData_t;

/**
 * @brief Initialize Avahi data
 */
void ARDISCOVERY_AvahiDiscovery_Init(ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData);

/**
 * @brief Start Avahi process of service advertisement
 */
void ARDISCOVERY_AvahiDiscovery_Publish(ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData);

/**
 * @brief Stop Avahi process of service advertisement
 */
void ARDISCOVERY_AvahiDiscovery_StopPublishing(ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData);

#endif /* _ARDISCOVERY_AVAHIDISCOVERY_H_ */
