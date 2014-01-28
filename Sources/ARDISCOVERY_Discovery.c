#include <libARDiscovery/ARDISCOVERY_Discovery.h>
#include <string.h>

static const uint16_t ARDISCOVERY_Discovery_ProductTable[ARDISCOVERY_PRODUCT_MAX] =
{
    // NSNet Service
    [ARDISCOVERY_PRODUCT_ARDRONE]       = 0x0901,
    [ARDISCOVERY_PRODUCT_JS]            = 0x0902,

    // BLE Service
    [ARDISCOVERY_PRODUCT_ARDRONE_MINI]  = 0x0900
};

static const char* ARDISCOVERY_Discovery_ProductNameTable[ARDISCOVERY_PRODUCT_MAX] =
{
    // NSNet Service
    [ARDISCOVERY_PRODUCT_ARDRONE]       = "AR.Drone",
    [ARDISCOVERY_PRODUCT_JS]            = "Jumping Sumo",
    
    // BLE Service
    [ARDISCOVERY_PRODUCT_ARDRONE_MINI]  = "MiniDrone"
};


uint16_t ARDISCOVERY_getProductID(eARDISCOVERY_PRODUCT product)
{
    return ARDISCOVERY_Discovery_ProductTable[product];
}

const char* ARDISCOVERY_getProductName(eARDISCOVERY_PRODUCT product)
{
    return ARDISCOVERY_Discovery_ProductNameTable[product];
}

eARDISCOVERY_PRODUCT ARDISCOVERY_getProductFromName(const char *name)
{
    uint8_t product = ARDISCOVERY_PRODUCT_MAX;
    int i = 0;

    if (name == NULL)
        return ARDISCOVERY_PRODUCT_MAX;
    for (i = 0; (product == ARDISCOVERY_PRODUCT_MAX) && (i < ARDISCOVERY_PRODUCT_MAX); i++)
    {
        if(strcmp(name, ARDISCOVERY_Discovery_ProductNameTable[i]) == 0)
            product = i;
    }
    
    return product;
}
