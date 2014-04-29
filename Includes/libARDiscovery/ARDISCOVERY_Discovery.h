#ifndef _ARDISCOVERY_DISCOVERY_H_
#define _ARDISCOVERY_DISCOVERY_H_
#include <inttypes.h>

#define ARDISCOVERY_SERVICE_NET_DEVICE_DOMAIN   "local"
#define ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT   "_arsdk-%04x._udp"
#define ARDISCOVERY_SERVICE_NET_RSSI_SIGNAL_KEY "rssi_signal"

/**
 * Enum characterizing every Parrot's product and categorizing them
 */
typedef enum
{
    ARDISCOVERY_PRODUCT_NSNETSERVICE = 0,                               ///< WiFi products category
    ARDISCOVERY_PRODUCT_ARDRONE = ARDISCOVERY_PRODUCT_NSNETSERVICE,     ///< AR DRONE product
    ARDISCOVERY_PRODUCT_JS,                                             ///< JUMPING SUMO product
    
    ARDISCOVERY_PRODUCT_BLESERVICE,                                     ///< BlueTooth products category
    ARDISCOVERY_PRODUCT_MINIDRONE = ARDISCOVERY_PRODUCT_BLESERVICE,         ///< DELOS product
    
    ARDISCOVERY_PRODUCT_MAX                                             ///< Max of products
} eARDISCOVERY_PRODUCT;

/**
 * @brief Get service of product
 * This function is the only one knowing the correspondance between product 
 * and service.
 * @param product The product's enumerator
 * @return The corresponding product ID for the service
 */
eARDISCOVERY_PRODUCT ARDISCOVERY_getProductService(eARDISCOVERY_PRODUCT product);

/**
 * @brief Converts from product enumerator to product ID
 * This function is the only one knowing the correspondance
 * between the product enumerator and the products' IDs.
 * @param product The product's enumerator
 * @return The corresponding product ID
 */
uint16_t ARDISCOVERY_getProductID(eARDISCOVERY_PRODUCT product);

/**
 * @brief Converts from product enumerator to product name
 * This function is the only one knowing the correspondance
 * between the product enumerator and the products name.
 * @param product The product's enumerator
 * @return The corresponding product name
 */
const char* ARDISCOVERY_getProductName(eARDISCOVERY_PRODUCT product);

/**
 * @brief Converts from product enumerator to product path name
 * This function is the only one knowing the correspondance
 * between the product enumerator and the products path name.
 * @param product The product's enumerator
 * @param buffer The application buffer that will receive the product path name 
 * @param length The length of the application buffer that will receive the product path name
 * @return The corresponding product path name
 */
void ARDISCOVERY_getProductPathName(eARDISCOVERY_PRODUCT product, char *buffer, int length);

/**
 * @brief Converts from product name to product enumerator
 * This function is the only one knowing the correspondance
 * between the products name and the product enumerator.
 * @param name The product's name
 * @return The corresponding product enumerator
 */
eARDISCOVERY_PRODUCT ARDISCOVERY_getProductFromName(const char *name);

/**
 * @brief Converts from product ID to product enumerator
 * This function is the only one knowing the correspondance
 * between the products IDs and the product enumerator.
 * @param productID the productID of the product
 * @return The corresponding product enumerator
 */
eARDISCOVERY_PRODUCT ARDISCOVERY_getProductFromProductID(uint16_t productID);

#endif // _ARDISCOVERY_DISCOVERY_H_
