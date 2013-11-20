#ifndef _ARDISCOVERY_DISCOVERY_H_
#define _ARDISCOVERY_DISCOVERY_H_
#include <inttypes.h>

/**
 * Enum characterizing every Parrot's product and categorizing them
 */
typedef enum
{
    ARDISCOVERY_PRODUCT_NSNETSERVICE = 0,                               ///< WiFi products category
    ARDISCOVERY_PRODUCT_ARDRONE = ARDISCOVERY_PRODUCT_NSNETSERVICE,     ///< AR DRONE product
    ARDISCOVERY_PRODUCT_JS,                                             ///< JUMPING SUMO product
    
    ARDISCOVERY_PRODUCT_BLESERVICE,                                     ///< BlueTooth products category
    ARDISCOVERY_PRODUCT_ARDRONE_MINI = ARDISCOVERY_PRODUCT_BLESERVICE,         ///< DELOS product
    
    ARDISCOVERY_PRODUCT_MAX                                             ///< Max of products
} eARDISCOVERY_PRODUCT;

/**
 * @brief Converts a product enumerator in product ID
 * This function is the only one knowing the correspondance
 * between the enumerator and the products' IDs.
 * @param product The product's enumerator
 * @return The corresponding product ID
 */
uint16_t ARDISCOVERY_getProductID(eARDISCOVERY_PRODUCT product);

#endif // _ARDISCOVERY_DISCOVERY_H_
