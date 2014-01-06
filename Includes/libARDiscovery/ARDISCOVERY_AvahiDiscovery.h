#ifndef _ARDISCOVERY_AVAHIDISCOVERY_H_
#define _ARDISCOVERY_AVAHIDISCOVERY_H_

#include <libARDiscovery/ARDISCOVERY_Error.h>

/**
 * @brief Default service parameters
 */
#define ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_NETWORK "local"

#define ARDISCOVERY_AVAHIDISCOVERY_SERVICE_NB_MAX ARDISCOVERY_PRODUCT_MAX

/**
 * @brief Structure to allow data sharing across discovery process
 */
typedef struct ARDISCOVERY_AvahiDiscovery_PublisherData_t ARDISCOVERY_AvahiDiscovery_PublisherData_t;

/**
 * @brief Structure to allow data sharing across service browsing process
 */
typedef struct ARDISCOVERY_AvahiDiscovery_BrowserData_t ARDISCOVERY_AvahiDiscovery_BrowserData_t;

/**
 * @brief callback to notify whether a service of the subscribed type has appeared or disappeared
 * @param[in] state Service appeared (1) or disappeared (0)
 * @param[in] serviceName Service name
 * @param[in] serviceType Service type
 * @param[in] ipAddr IP address of the host for said service
 * @return error during callback execution
 */
typedef eARDISCOVERY_ERROR (*ARDISCOVERY_AvahiDiscovery_Browser_Callback_t) (void* custom, uint8_t state, uint8_t* serviceName, uint8_t* serviceType, uint8_t* ipAddr);

/**
 * @brief Initialize Publication related Avahi data
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

/**
 * @brief Initialize Browsing related Avahi data
 * @param[in] callback Callback called whenever a service appears or disappears
 * @param[in] customData Data forwarded to callback
 * @param[in] serviceTypes Table of pointers to service type strings
 * @param[in] serviceTypesNb Number of service type strings in table
 * @param[in] errorPtr Error during execution
 * @return Pointer to allocated browser data
 */
ARDISCOVERY_AvahiDiscovery_BrowserData_t* ARDISCOVERY_AvahiDiscovery_Browser_New(ARDISCOVERY_AvahiDiscovery_Browser_Callback_t callback, void* customData, uint8_t** serviceTypes, uint8_t serviceTypesNb, eARDISCOVERY_ERROR* errorPtr);

/**
 * @brief Start Avahi process of service browsing
 * @param[in] browserData Browser data
 */
void ARDISCOVERY_AvahiDiscovery_Browse(ARDISCOVERY_AvahiDiscovery_BrowserData_t* browserData);

/**
 * @brief Stop Avahi process of service browsing
 * @param[in] browserData Browser data
 */
void ARDISCOVERY_AvahiDiscovery_StopBrowsing(ARDISCOVERY_AvahiDiscovery_BrowserData_t* browserData);

/*
 * @brief Free data structures
 * @param[in] browserDataPtrAddr Pointer to browser data
 */
void ARDISCOVERY_AvahiDiscovery_Browser_Delete(ARDISCOVERY_AvahiDiscovery_BrowserData_t** browserDataPtrAddr);

#endif /* _ARDISCOVERY_AVAHIDISCOVERY_H_ */
