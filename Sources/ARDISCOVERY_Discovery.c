#include <libARDiscovery/ARDISCOVERY_Discovery.h>
#include <string.h>

static const uint16_t ARDISCOVERY_Discovery_ProductTable[ARDISCOVERY_PRODUCT_MAX] =
{
    // NSNet Service
    [ARDISCOVERY_PRODUCT_ARDRONE]       = 0x0901,
    [ARDISCOVERY_PRODUCT_JS]            = 0x0902,

    // BLE Service
    [ARDISCOVERY_PRODUCT_MINIDRONE]  = 0x0900
};

static const char* ARDISCOVERY_Discovery_ProductNameTable[ARDISCOVERY_PRODUCT_MAX] =
{
    // NSNet Service
    [ARDISCOVERY_PRODUCT_ARDRONE]       = "AR.Drone",
    [ARDISCOVERY_PRODUCT_JS]            = "Jumping Sumo",
    
    // BLE Service
    [ARDISCOVERY_PRODUCT_MINIDRONE]     = "Mini Drone"
};

eARDISCOVERY_PRODUCT ARDISCOVERY_getProductService(eARDISCOVERY_PRODUCT product)
{
    eARDISCOVERY_PRODUCT retval = ARDISCOVERY_PRODUCT_MAX;
    
    if(ARDISCOVERY_PRODUCT_NSNETSERVICE <= product && product < ARDISCOVERY_PRODUCT_BLESERVICE)
    {
        retval = ARDISCOVERY_PRODUCT_NSNETSERVICE;
    }
    else if(ARDISCOVERY_PRODUCT_BLESERVICE <= product && product < ARDISCOVERY_PRODUCT_MAX)
    {
        retval = ARDISCOVERY_PRODUCT_BLESERVICE;
    }
    
    return retval;
}

uint16_t ARDISCOVERY_getProductID(eARDISCOVERY_PRODUCT product)
{
    return ARDISCOVERY_Discovery_ProductTable[product];
}

const char* ARDISCOVERY_getProductName(eARDISCOVERY_PRODUCT product)
{
    char *name = NULL;
    if(product == ARDISCOVERY_PRODUCT_MAX)
        name = "";
    else
        name = (char *)ARDISCOVERY_Discovery_ProductNameTable[product];
        
    return (const char *)name;
}

void ARDISCOVERY_getProductPathName(eARDISCOVERY_PRODUCT product, char *buffer, int length)
{
    if ((buffer != NULL) && (length > 0))
    {
        const char *name = ARDISCOVERY_getProductName(product);
        int nameLen = strlen(name);
        char *index;
        
        if (length > nameLen)
        {
            strncpy(buffer, name, nameLen + 1);
            index = buffer;
            while ((index = strstr(index, " ")) != NULL)
            {
                *index = '_';
                index++;
            }
        }
        else
        {
            *buffer = '\0';
        }
    }
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

eARDISCOVERY_PRODUCT ARDISCOVERY_getProductFromProductID(uint16_t productID)
{
    uint8_t product = ARDISCOVERY_PRODUCT_MAX;
    int i = 0;

    for (i = 0; (product == ARDISCOVERY_PRODUCT_MAX) && (i < ARDISCOVERY_PRODUCT_MAX); i++)
    {
        if (ARDISCOVERY_Discovery_ProductTable[i] == productID)
        {
            product = i;
        }
    }
    
    return product;
}
