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
 * @file ARDISCOVERY_Device.c
 * @brief Discovery Device contains the informations of a device discovered
 * @date 02/03/2015
 * @author maxime.maitre@parrot.com
 */

#include <stdlib.h>
#include <libARSAL/ARSAL_Print.h>
#include <libARNetworkAL/ARNETWORKAL_Manager.h>
#include <libARNetworkAL/ARNETWORKAL_Error.h>
#include <libARDiscovery/ARDISCOVERY_Connection.h>
#include <libARDiscovery/ARDISCOVERY_Device.h>

#include "ARDISCOVERY_Device.h"


#define ARDISCOVERY_DEVICE_TAG "ARDISCOVERY_Device"

/*************************
 * Private header
 *************************/



/*************************
 * Implementation
 *************************/

ARNETWORKAL_Manager_t *ARDISCOVERY_Device_NewARNetworkAL (ARDISCOVERY_DiscoveryDevice_t *discoveryDevice, eARDISCOVERY_ERROR *error, eARNETWORKAL_ERROR *errorAL)
{
    // -- create a new ARNetworkAL --
    
    //local declarations
    eARDISCOVERY_ERROR localError = ARDISCOVERY_OK;
    eARNETWORKAL_ERROR localErrorAL = ARNETWORKAL_OK;
    ARNETWORKAL_Manager_t *networkALManager = NULL;
    
    
    // check parameters
    if (discoveryDevice == NULL)
    {
        localError = ARDISCOVERY_ERROR_BAD_PARAMETER;
    }
    // No Else: the checking parameters sets localError to ARNETWORK_ERROR_BAD_PARAMETER and stop the processing
    
    if (localError == ARDISCOVERY_OK)
    {
        if ((discoveryDevice->newNetworkAL != NULL) && (discoveryDevice->deleteNetworkAL != NULL))
        {
            networkALManager = discoveryDevice->newNetworkAL (&localErrorAL);
        }
        else
        {
            localError = ARDISCOVERY_ERROR_DEVICE_OPERATION_NOT_SUPPORTED;
        }
    }
    
    // delete the Network Controller if an error occurred
    if ((localError != ARDISCOVERY_OK) || (localErrorAL != ARNETWORKAL_OK))
    {
        ARSAL_PRINT (ARSAL_PRINT_ERROR, ARDISCOVERY_DEVICE_TAG, "error: %s", ARDISCOVERY_Error_ToString (localError));
        
        // not NULL pointer already checked
        discoveryDevice->deleteNetworkAL (&networkALManager);
    }
    // No else: skipped by an error 

    // return the error */
    if (error != NULL)
    {
        *error = localError;
    }
    // No else: error is not returned
    
    if (errorAL != NULL)
    {
        *errorAL = localErrorAL;
    }
    // No else: error is not returned 
    
    return networkALManager;
}

eARDISCOVERY_ERROR ARDISCOVERY_Device_DeleteARNetworkAL (ARDISCOVERY_DiscoveryDevice_t *discoveryDevice, ARNETWORKAL_Manager_t **networkALManager)
{
    //local declarations
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    // check parameters
    if (discoveryDevice == NULL)
    {
        error = ARDISCOVERY_ERROR_BAD_PARAMETER;
    }
    // No Else: the checking parameters sets localError to ARNETWORK_ERROR_BAD_PARAMETER and stop the processing
    
    if (error == ARDISCOVERY_OK)
    {
        if (discoveryDevice->deleteNetworkAL != NULL)
        {
            discoveryDevice->deleteNetworkAL (networkALManager);
        }
        else
        {
            error = ARDISCOVERY_ERROR_DEVICE_OPERATION_NOT_SUPPORTED;
        }
    }
    
    return error;
}

ARDISCOVERY_NetworkConfiguration_t ARDISCOVERY_Device_GetNetworkCongifuration (ARDISCOVERY_DiscoveryDevice_t *discoveryDevice, eARDISCOVERY_ERROR *error)
{
    //local declarations
    eARDISCOVERY_ERROR localError = ARDISCOVERY_OK;
    ARDISCOVERY_NetworkConfiguration_t networkConfiguration;
    
    // init networkConfiguration
    networkConfiguration.controllerToDeviceNotAckId = -1;
    networkConfiguration.controllerToDeviceAckId = -1;
    networkConfiguration.controllerToDeviceHightPriority = -1;
    networkConfiguration.controllerToDeviceARStreamAck = -1;
    networkConfiguration.deviceToControllerNotAckId = -1;
    networkConfiguration.deviceToControllerAckId = -1;
    //networkConfiguration.deviceToControllerHightPriority = -1;
    networkConfiguration.deviceToControllerARStreamData = -1;
    
    networkConfiguration.numberOfControllerToDeviceParam = 0;
    networkConfiguration.controllerToDeviceParams = NULL;
    networkConfiguration.numberOfDeviceToControllerParam  = 0;
    networkConfiguration.deviceToControllerParams = NULL;
    
    networkConfiguration.bleNotificationIDs = NULL;
    networkConfiguration.pingDelayMs =-1;
    
    networkConfiguration.numberOfDeviceToControllerCommandsBufferIds = 0;
    networkConfiguration.deviceToControllerCommandsBufferIds = NULL;
    
    
    // check parameters
    if (discoveryDevice == NULL)
    {
        localError = ARDISCOVERY_ERROR_BAD_PARAMETER;
    }
    // No Else: the checking parameters sets localError to ARNETWORK_ERROR_BAD_PARAMETER and stop the processing
    
    if (localError == ARDISCOVERY_OK)
    {
        if (discoveryDevice->newNetworkAL != NULL)
        {
            networkConfiguration = discoveryDevice->getNetworkCongifuration ();
        }
        else
        {
            localError = ARDISCOVERY_ERROR_DEVICE_OPERATION_NOT_SUPPORTED;
        }
    }

    // return the error */
    if (error != NULL)
    {
        *error = localError;
    }
    // No else: error is not returned
    
    return networkConfiguration;
}
 
 /*************************
 * local Implementation
 *************************/

