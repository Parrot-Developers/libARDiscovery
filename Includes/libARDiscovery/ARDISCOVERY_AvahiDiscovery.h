#ifndef _ARDISCOVERY_AVAHIDISCOVERY_H_
#define _ARDISCOVERY_AVAHIDISCOVERY_H_

#define ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_OUTPUT_PORT 43210
#define ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_NETWORK "local"

/**
 * @brief structure to allow service data sharing across discovery process
 */
typedef struct ARDISCOVERY_AvahiDiscovery_ServiceData_t
{
    char* serviceName;
    char* serviceType;
} ARDISCOVERY_AvahiDiscovery_ServiceData_t;

/**
 * @brief Start Avahi process of service advertisement
 */
void ARDISCOVERY_AvahiDiscovery_Start(char* serviceType);

/**
 * @brief Stop Avahi process of service advertisement
 */
void ARDISCOVERY_AvahiDiscovery_Stop(void);

#endif /* _ARDISCOVERY_AVAHIDISCOVERY_H_ */
