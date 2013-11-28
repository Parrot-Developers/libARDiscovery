#ifndef _ARDISCOVERY_AVAHIDISCOVERY_H_
#define _ARDISCOVERY_AVAHIDISCOVERY_H_

#include <libARDiscovery/ARDISCOVERY_Error.h>

/**
 * @brief Default service parameters
 */
#define ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_NETWORK "local"
#define ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_PUBLISHED_PORT 43210
#define ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_CONTROLLER_PORT 54321

/**
 * @brief Structures to allow data sharing across discovery process
 */
typedef struct ARDISCOVERY_AvahiDiscovery_ServiceData_t ARDISCOVERY_AvahiDiscovery_ServiceData_t;

/**
 * @brief Initialize Avahi data
 * @param[in] serviceName Discovery service name
 * @param[in] serviceType Discovery service type
 * @param[in] errorPtr Error during execution
 * @return Pointer to allocated service data
 */
ARDISCOVERY_AvahiDiscovery_ServiceData_t* ARDISCOVERY_AvahiDiscovery_New(uint8_t* serviceName, uint8_t* serviceType, eARDISCOVERY_ERROR* errorPtr);

/**
 * @brief Start Avahi process of service advertisement
 * @param[in] serviceData Service data
 */
void ARDISCOVERY_AvahiDiscovery_Publish(ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData);

/**
 * @brief Stop Avahi process of service advertisement
 * @param[in] serviceData Service data
 */
void ARDISCOVERY_AvahiDiscovery_StopPublishing(ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData);

/*
 * @brief Free data structures
 * @param[in] serviceDataPtrAddr Pointer to Service data
 */
void ARDISCOVERY_AvahiDiscovery_Delete(ARDISCOVERY_AvahiDiscovery_ServiceData_t** serviceDataPtrAddr);

#endif /* _ARDISCOVERY_AVAHIDISCOVERY_H_ */
