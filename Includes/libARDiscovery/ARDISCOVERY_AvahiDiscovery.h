#ifndef _ARDISCOVERY_AVAHIDISCOVERY_H_
#define _ARDISCOVERY_AVAHIDISCOVERY_H_

#define ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_CONNECTION_PORT 43210
#define ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_NETWORK "local"

/**
 * @brief structure to allow service data sharing across discovery process
 */
typedef struct ARDISCOVERY_AvahiDiscovery_ServiceData_t
{
    char* serviceName;  // Specific to each device ("ARDrone_21452365")
    char* serviceType;  // Specific to each platform ("_ardrone3._tcp")
    int inboundPort;    // UDP connection device -> controller (known from device config)
    int outboundPort;   // UDP connection controller -> device (got from controller)
    char* controllerIP; // Connected controller IP

} ARDISCOVERY_AvahiDiscovery_ServiceData_t;

/**
 * @brief Start Avahi process of service advertisement
 */
void ARDISCOVERY_AvahiDiscovery_StartPublishing(ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData);

/**
 * @brief Stop Avahi process of service advertisement
 */
void ARDISCOVERY_AvahiDiscovery_StopPublishing(void);

#endif /* _ARDISCOVERY_AVAHIDISCOVERY_H_ */
