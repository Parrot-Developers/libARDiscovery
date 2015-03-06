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
/**
 * @file ARDISCOVERY_Device.h
 * @brief Discovery Device contains the informations of a device discovered
 * @date 02/03/2015
 * @author maxime.maitre@parrot.com
 */

#ifndef _ARDISCOVERY_DEVICE_H_
#define _ARDISCOVERY_DEVICE_H_

#include <libARNetworkAL/ARNETWORKAL_Manager.h>
#include <libARNetworkAL/ARNETWORKAL_Error.h>
#include <libARNetwork/ARNETWORK_IOBufferParam.h>
#include <libARDiscovery/ARDISCOVERY_Error.h>
#include <libARDiscovery/ARDISCOVERY_Discovery.h>
#include <libARDiscovery/ARDISCOVERY_NetworkConfiguration.h>

/**
 * @brief Callback to create a new ARNetworkAL
 */
typedef ARNETWORKAL_Manager_t *(*ARDISCOVERY_DISCOVERYDEVICE_NewARNetworkAL_t) (eARNETWORKAL_ERROR *error);

/**
 * @brief Callback to delete the ARNetworkAL
 */
typedef void (*ARDISCOVERY_DISCOVERYDEVICE_DeleteARNetworkAL_t) (ARNETWORKAL_Manager_t **networkAL);

/**
 * @brief Callback to get the NetworkConfiguration to use with the device
 */
typedef ARDISCOVERY_NetworkConfiguration_t (*ARDISCOVERY_DISCOVERYDEVICE_GetNetworkCongifuration_t) ();

/**
 * @brief network device
 * @note This is an application-provided object, OS Dependant
 */
//typedef void* ARDISCOVERY_DISCOVERYDEVICE_NetworkDevice_t;

/**
 * @brief DiscoveryDevice contains the informations of a device discovered
 */
typedef struct ARDISCOVERY_DiscoveryDevice_t ARDISCOVERY_DiscoveryDevice_t;

/**
 * @brief DiscoveryDevice contains the informations of a device discovered
 */
struct ARDISCOVERY_DiscoveryDevice_t
{
    char *name;
    int nameLength;
    eARDISCOVERY_PRODUCT productID;
    //ARDISCOVERY_DISCOVERYDEVICE_NetworkDevice_t device;
    ARDISCOVERY_DISCOVERYDEVICE_NewARNetworkAL_t newNetworkAL;
    ARDISCOVERY_DISCOVERYDEVICE_DeleteARNetworkAL_t deleteNetworkAL;
    ARDISCOVERY_DISCOVERYDEVICE_GetNetworkCongifuration_t getNetworkCongifuration;
    void *customData;
};

/**
 * @brief Create and initialize a DiscoveryDevice
 * @param[in] name of the device
 * @param[in] length of the name of the device
 * @param[in] device the OS network device
 * @param[out] error Error code
 * @return new DiscoveryDevice
 */
//ARDISCOVERY_DiscoveryDevice_t* ARDISCOVERY_Connection_New (char *name, int nameLength, int productID, ARDISCOVERY_DISCOVERYDEVICE_NetworkDevice_t device, void* customData, eARDISCOVERY_ERROR *error);

/**
 * @brief Delete connection data
 * @param[in] connectionData Connection data
 * @param[out] error Error code
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */
//eARDISCOVERY_ERROR ARDISCOVERY_Connection_Delete (ARDISCOVERY_DiscoveryDevice_t **connectionData);


/**
 * @brief create a new ARNetworkAL
 */
ARNETWORKAL_Manager_t *ARDISCOVERY_Device_NewARNetworkAL (ARDISCOVERY_DiscoveryDevice_t *discoveryDevice, eARDISCOVERY_ERROR *error, eARNETWORKAL_ERROR *errorAL);

/**
 * @brief create a delete ARNetworkAL
 */
eARDISCOVERY_ERROR ARDISCOVERY_Device_DeleteARNetworkAL (ARDISCOVERY_DiscoveryDevice_t *discoveryDevice, ARNETWORKAL_Manager_t **networkALManager);

/**
 * @brief get the NetworkConfiguration to use with the device
 */
ARDISCOVERY_NetworkConfiguration_t ARDISCOVERY_Device_GetNetworkCongifuration (ARDISCOVERY_DiscoveryDevice_t *discoveryDevice, eARDISCOVERY_ERROR *error);

#endif // _ARDISCOVERY_DEVICE_H_
