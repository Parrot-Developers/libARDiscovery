#ifndef _ARDISCOVERY_AVAHIDISCOVERY_PRIVATE_H_
#define _ARDISCOVERY_AVAHIDISCOVERY_PRIVATE_H_

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
    char* serviceName;              // Specific to each device ("ARDrone_21452365")
    char* serviceType;              // Specific to each platform ("_ardrone3._ucp")
    uint32_t devicePort;            // Port advertised by device
};

#define HOST_NAME_MAX (128)

#endif /* _ARDISCOVERY_AVAHIDISCOVERY_PRIVATE_H_ */
