#include <libARDiscovery/ARDISCOVERY_Discovery.h>

static const uint16_t ARDISCOVERY_Discovery_ProductTable[ARDISCOVERY_PRODUCT_MAX] =
{
    // NSNet Service
    [ARDISCOVERY_PRODUCT_ARDRONE]       = 0x0901,
    [ARDISCOVERY_PRODUCT_JS]            = 0x0902,

    // BLE Service
    [ARDISCOVERY_PRODUCT_ARDRONE_MINI]  = 0x0900
};

uint16_t ARDISCOVERY_getProductID(eARDISCOVERY_PRODUCT product)
{
    return ARDISCOVERY_Discovery_ProductTable[product];
}
