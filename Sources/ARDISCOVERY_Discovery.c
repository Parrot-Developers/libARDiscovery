/*
  Copyright (C) 2014 Parrot SA

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:
  * Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.
  * Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in
  the documentation and/or other materials provided with the
  distribution.
  * Neither the name of Parrot nor the names
  of its contributors may be used to endorse or promote products
  derived from this software without specific prior written
  permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
  AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  SUCH DAMAGE.
*/
#include <libARDiscovery/ARDISCOVERY_Discovery.h>
#include <string.h>

static const uint16_t ARDISCOVERY_Discovery_ProductTable[ARDISCOVERY_PRODUCT_MAX] =
{
    // BLE Service
    [ARDISCOVERY_PRODUCT_MINIDRONE]     = 0x0900,
    [ARDISCOVERY_PRODUCT_MINIDRONE_EVO_LIGHT] = 0x0907,
    [ARDISCOVERY_PRODUCT_MINIDRONE_EVO_BRICK] = 0x0909,
    [ARDISCOVERY_PRODUCT_MINIDRONE_EVO_HYDROFOIL] = 0x090a,

    // NSNet Service
    [ARDISCOVERY_PRODUCT_ARDRONE]       = 0x0901,
    [ARDISCOVERY_PRODUCT_JS]            = 0x0902,
    [ARDISCOVERY_PRODUCT_SKYCONTROLLER] = 0x0903,
    [ARDISCOVERY_PRODUCT_JS_EVO_LIGHT]  = 0x0905,
    [ARDISCOVERY_PRODUCT_JS_EVO_RACE]   = 0x0906,
    [ARDISCOVERY_PRODUCT_BEBOP_2]       = 0x090c,
    [ARDISCOVERY_PRODUCT_UNKNOWN_PRODUCT_1]      = 0x090d,
};

static const char* ARDISCOVERY_Discovery_ProductNameTable[ARDISCOVERY_PRODUCT_MAX] =
{
    // BLE Service
    [ARDISCOVERY_PRODUCT_MINIDRONE]     = "Rolling Spider",
    [ARDISCOVERY_PRODUCT_MINIDRONE_EVO_LIGHT] = "Airborne Night",
    [ARDISCOVERY_PRODUCT_MINIDRONE_EVO_BRICK] = "Airborne Cargo",
    [ARDISCOVERY_PRODUCT_MINIDRONE_EVO_HYDROFOIL] = "Hydrofoil",

    // NSNet Service
    [ARDISCOVERY_PRODUCT_ARDRONE]       = "Bebop Drone",
    [ARDISCOVERY_PRODUCT_JS]            = "Jumping Sumo",
    [ARDISCOVERY_PRODUCT_SKYCONTROLLER] = "SkyController",
    [ARDISCOVERY_PRODUCT_JS_EVO_LIGHT]  = "Jumping Night",
    [ARDISCOVERY_PRODUCT_JS_EVO_RACE]   = "Jumping Race",
    [ARDISCOVERY_PRODUCT_BEBOP_2]       = "Bebop 2",
    [ARDISCOVERY_PRODUCT_UNKNOWN_PRODUCT_1]      = "Unknown Product 1",
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
            while (*index != '\0')
            {
                if (*index == '.' ||
                    *index == ' ')
                {
                    *index = '_';
                }
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

eARDISCOVERY_PRODUCT ARDISCOVERY_getProductFromPathName(const char *name)
{
    uint8_t product = ARDISCOVERY_PRODUCT_MAX;
    int i = 0;

    char buffer[256];

    if (name == NULL)
        return ARDISCOVERY_PRODUCT_MAX;
    int namelen = strlen(name);
    for (i = 0; (product == ARDISCOVERY_PRODUCT_MAX) && (i < ARDISCOVERY_PRODUCT_MAX); i++)
    {
        ARDISCOVERY_getProductPathName(i, buffer, 256);
        if(namelen < strlen(buffer))
            continue;
        if(strncmp(name, buffer, strlen(buffer)) == 0)
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

eARDISCOVERY_PRODUCT_FAMILY ARDISCOVERY_getProductFamily(eARDISCOVERY_PRODUCT product)
{
    eARDISCOVERY_PRODUCT_FAMILY family = ARDISCOVERY_PRODUCT_FAMILY_MAX;

    switch (product)
    {
    case ARDISCOVERY_PRODUCT_ARDRONE:
    case ARDISCOVERY_PRODUCT_BEBOP_2:
        family = ARDISCOVERY_PRODUCT_FAMILY_ARDRONE;
        break;
    case ARDISCOVERY_PRODUCT_JS:
    case ARDISCOVERY_PRODUCT_JS_EVO_LIGHT:
    case ARDISCOVERY_PRODUCT_JS_EVO_RACE:
        family = ARDISCOVERY_PRODUCT_FAMILY_JS;
        break;
    case ARDISCOVERY_PRODUCT_SKYCONTROLLER:
        family = ARDISCOVERY_PRODUCT_FAMILY_SKYCONTROLLER;
        break;
    case ARDISCOVERY_PRODUCT_MINIDRONE:
    case ARDISCOVERY_PRODUCT_MINIDRONE_EVO_LIGHT:
    case ARDISCOVERY_PRODUCT_MINIDRONE_EVO_BRICK:
    case ARDISCOVERY_PRODUCT_MINIDRONE_EVO_HYDROFOIL:
        family = ARDISCOVERY_PRODUCT_FAMILY_MINIDRONE;
        break;
    case ARDISCOVERY_PRODUCT_UNKNOWN_PRODUCT_1:
        family = ARDISCOVERY_PRODUCT_FAMILY_UNKNOWN_PRODUCT_1;
        break;
    default:
        break;
    }

    return family;
}
