#ifndef _ARDISCOVERY_AVAHIDISCOVERY_H_
#define _ARDISCOVERY_AVAHIDISCOVERY_H_

#include <libARDiscovery/ARDISCOVERY_Error.h>

/**
 * @brief Default service parameters
 */
#define ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_NETWORK "local"

/**
 * @brief Structures to allow data sharing across discovery process
 */
typedef struct ARDISCOVERY_AvahiDiscovery_PublisherData_t ARDISCOVERY_AvahiDiscovery_PublisherData_t;

/**
 * @brief Initialize Avahi data
 * @param[in] serviceName Discovery service name
 * @param[in] serviceType Discovery service type
 * @param[in] errorPtr Error during execution
 * @return Pointer to allocated service data
 */
ARDISCOVERY_AvahiDiscovery_PublisherData_t* ARDISCOVERY_AvahiDiscovery_Publisher_New(uint8_t* serviceName, uint8_t* serviceType, uint32_t publishedPort, eARDISCOVERY_ERROR* errorPtr);

/**
 * @brief Start Avahi process of service advertisement
 * @param[in] serviceData Service data
 */
void ARDISCOVERY_AvahiDiscovery_Publish(ARDISCOVERY_AvahiDiscovery_PublisherData_t* serviceData);

/**
 * @brief Reset the entry group then re-create the service
 * @param[in] c Avahi client
 * @param[in] serviceData service data
 * @return error during execution
 */
eARDISCOVERY_ERROR ARDISCOVERY_AvahiDiscovery_ResetService(ARDISCOVERY_AvahiDiscovery_PublisherData_t* serviceData);

/**
 * @brief Stop Avahi process of service advertisement
 * @param[in] serviceData Service data
 */
void ARDISCOVERY_AvahiDiscovery_StopPublishing(ARDISCOVERY_AvahiDiscovery_PublisherData_t* serviceData);

/*
 * @brief Free data structures
 * @param[in] serviceDataPtrAddr Pointer to Service data
 */
void ARDISCOVERY_AvahiDiscovery_Publisher_Delete(ARDISCOVERY_AvahiDiscovery_PublisherData_t** serviceDataPtrAddr);

#endif /* _ARDISCOVERY_AVAHIDISCOVERY_H_ */
