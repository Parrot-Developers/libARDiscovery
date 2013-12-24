#include <stdio.h>
#include <stdlib.h>

#include <libARSAL/ARSAL_Print.h>

#include <libARDiscovery/ARDISCOVERY_AvahiDiscovery.h>
#include "ARDISCOVERY_AvahiDiscovery.h"

#define __ARDISCOVERY_AVAHIDISCOVERY_TAG__ "ARDISCOVERY_AvahiDiscovery"

#define ERR(...)    ARSAL_PRINT(ARSAL_PRINT_ERROR, __ARDISCOVERY_AVAHIDISCOVERY_TAG__, __VA_ARGS__)
#define SAY(...)    ARSAL_PRINT(ARSAL_PRINT_WARNING, __ARDISCOVERY_AVAHIDISCOVERY_TAG__, __VA_ARGS__)

/*
 * Private header
 */

/**
 * @brief Build final name
 * @return Pointer to name
 */
static uint8_t* ARDISCOVERY_AvahiDiscovery_BuildName(void);

/**
 * @brief Create service to be published
 * @param[in] c Avahi client
 * @param[in] serviceData service data
 * @return error during execution
 */
static eARDISCOVERY_ERROR ARDISCOVERY_AvahiDiscovery_CreateService(AvahiClient *c, ARDISCOVERY_AvahiDiscovery_PublisherData_t* serviceData);

/**
 * @brief Client callback
 * @param[in] c Avahi client
 * @param[in] state Client state
 * @param[in] userData service data
 */
static void ARDISCOVERY_AvahiDiscovery_ClientCb(AvahiClient* c, AvahiClientState state, void* userdata);

/*
 * Publisher
 */

ARDISCOVERY_AvahiDiscovery_PublisherData_t* ARDISCOVERY_AvahiDiscovery_Publisher_New(uint8_t* serviceName, uint8_t* serviceType, uint32_t publishedPort, eARDISCOVERY_ERROR* errorPtr)
{
    /*
     * Create and initialize discovery data
     */
    ARDISCOVERY_AvahiDiscovery_PublisherData_t *serviceData = NULL;
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    if (serviceName == NULL || serviceType == NULL)
    {
        ERR("Null parameter");
        error = ARDISCOVERY_ERROR;
    }

    if (error == ARDISCOVERY_OK)
    {
        serviceData = malloc(sizeof(ARDISCOVERY_AvahiDiscovery_PublisherData_t));
        if (serviceData != NULL)
        {
            /* Init Avahi data */
            serviceData->entryGroup = NULL;
            serviceData->simplePoll = NULL;
            serviceData->devicePort = publishedPort;

            /* Set Service Type */
            if (error == ARDISCOVERY_OK)
            {
                serviceData->serviceType = (uint8_t *) malloc(sizeof(uint8_t) * ARDISCOVERY_AVAHIDISCOVERY_SERVICETYPE_SIZE);
                if (serviceData->serviceType != NULL)
                {
                    strcpy((char *)serviceData->serviceType, (char *)serviceType);
                }
                else
                {
                    error = ARDISCOVERY_ERROR_ALLOC;
                }
            }

            /* Set Service Name */
            if (error == ARDISCOVERY_OK)
            {
                serviceData->serviceName = (uint8_t *) malloc(sizeof(uint8_t) * ARDISCOVERY_AVAHIDISCOVERY_SERVICENAME_SIZE);
                if (serviceData->serviceName != NULL)
                {
                    strcpy((char *)serviceData->serviceName, (char *)serviceName);
                }
                else
                {
                    error = ARDISCOVERY_ERROR_ALLOC;
                }
            }
        }
    }

    /* Delete connection data if an error occurred */
    if (error != ARDISCOVERY_OK)
    {
        ERR("error: %s", ARDISCOVERY_Error_ToString (error));
        ARDISCOVERY_AvahiDiscovery_Publisher_Delete(&serviceData);
    }

    if (errorPtr != NULL)
    {
        *errorPtr = error;
    }

    return serviceData;
}

static uint8_t* ARDISCOVERY_AvahiDiscovery_BuildName(void)
{
    /*
     * Get hostname and build the final name
     */
    uint8_t hostname[HOST_NAME_MAX + 1]; /* POSIX hostname max length + the null terminating byte. */
    int error = gethostname((char *)hostname, sizeof(hostname));
    if (error == 0)
    {
        hostname[sizeof(hostname) - 1] = '\0';
        return (uint8_t *)strdup((const char *)hostname);
    }
    return NULL;
}

static void ARDISCOVERY_AvahiDiscovery_EntryGroupCb(AvahiEntryGroup* g, AvahiEntryGroupState state, void* userdata)
{
    /*
     * Avahi entry group callback
     */
    ARDISCOVERY_AvahiDiscovery_PublisherData_t* serviceData = (ARDISCOVERY_AvahiDiscovery_PublisherData_t*) userdata;

    if (g == NULL || serviceData == NULL)
    {
        ERR("Null parameter");
        return;
    }

    /* Called whenever the entry group state changes */
    switch (state)
    {
    case AVAHI_ENTRY_GROUP_ESTABLISHED:
    {
        /* The entry group has been established successfully */
        SAY("Service '%s' successfully established.", serviceData->serviceName);
        break;
    }
    case AVAHI_ENTRY_GROUP_COLLISION:
    {
        /* A service name collision happened. Let's pick a new name */
        char* n = avahi_alternative_service_name((const char *)serviceData->serviceName);
        avahi_free(serviceData->serviceName);
        serviceData->serviceName = (uint8_t *)n;

        ERR("Service name collision, renaming service to '%s'", serviceData->serviceName);

        /* And recreate the services */
        ARDISCOVERY_AvahiDiscovery_CreateService(avahi_entry_group_get_client(g), serviceData);
        break;
    }

    case AVAHI_ENTRY_GROUP_FAILURE:
    {
        ERR( "Entry group failure: %s", avahi_strerror(avahi_client_errno(avahi_entry_group_get_client(g))));

        /* Some kind of failure happened while we were registering our services */
        avahi_simple_poll_quit(serviceData->simplePoll);
        break;
    }
    case AVAHI_ENTRY_GROUP_UNCOMMITED:
    case AVAHI_ENTRY_GROUP_REGISTERING:
    default:
        break;
    }
}

static eARDISCOVERY_ERROR ARDISCOVERY_AvahiDiscovery_CreateService(AvahiClient* c, ARDISCOVERY_AvahiDiscovery_PublisherData_t* serviceData)
{
    /*
     * Create Avahi service
     */
    int ret;
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    if (c == NULL || serviceData == NULL)
    {
        ERR("Null parameter");
        error = ARDISCOVERY_ERROR;
    }

    if (error == ARDISCOVERY_OK)
    {
        /* If this is the first time we're called, let's create a new entry group */
        if (!serviceData->entryGroup)
        {
            serviceData->entryGroup = avahi_entry_group_new(c, ARDISCOVERY_AvahiDiscovery_EntryGroupCb, (void*) serviceData);
            if (!serviceData->entryGroup)
            {
                error = ARDISCOVERY_ERROR_ENTRY_GROUP;
            }
        }
    }

    if (error == ARDISCOVERY_OK)
    {
        /* Add the service for AR.Drone */
        ret = avahi_entry_group_add_service(serviceData->entryGroup, AVAHI_IF_UNSPEC, AVAHI_PROTO_UNSPEC, 0, (const char *)serviceData->serviceName,
                                            (const char *)serviceData->serviceType, ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_NETWORK, NULL, serviceData->devicePort, NULL, NULL);
        if (ret < 0)
        {
            error = ARDISCOVERY_ERROR_ADD_SERVICE;
        }
    }

    /* Tell the server to register the service */
    if (error == ARDISCOVERY_OK)
    {
        ret = avahi_entry_group_commit(serviceData->entryGroup);
        if (ret < 0)
        {
            error = ARDISCOVERY_ERROR_GROUP_COMMIT;
        }
    }

    if (error != ARDISCOVERY_OK)
    {
        ERR("error: %s", ARDISCOVERY_Error_ToString (error));
        avahi_simple_poll_quit(serviceData->simplePoll);
    }

    return error;
}

static void ARDISCOVERY_AvahiDiscovery_ClientCb(AvahiClient* c, AvahiClientState state, void* userdata)
{
    /*
     * Avahi client callback
     * Called whenever the client or server state changes
     */
    ARDISCOVERY_AvahiDiscovery_PublisherData_t* serviceData = (ARDISCOVERY_AvahiDiscovery_PublisherData_t*) userdata;

    if (c == NULL || serviceData == NULL)
    {
        ERR("Null parameter");
        return;
    }

    switch (state)
    {
    case AVAHI_CLIENT_S_RUNNING:
    {
        /* The server has startup successfully and registered its host
         * name on the network, so it's time to create our services */
        if (!serviceData->entryGroup)
        {
            ARDISCOVERY_AvahiDiscovery_CreateService(c, serviceData);
        }
        break;
    }
    case AVAHI_CLIENT_FAILURE:
    {
        ERR("Client failure: %s", avahi_strerror(avahi_client_errno(c)));
        avahi_simple_poll_quit(serviceData->simplePoll);
        break;
    }
    case AVAHI_CLIENT_S_COLLISION:
    {
        /* Let's drop our registered services. When the server is back
         * in AVAHI_SERVER_RUNNING state we will register them
         * again with the new host name. */
    }
    case AVAHI_CLIENT_S_REGISTERING:
    {
        /* The server records are now being established. This
         * might be caused by a host name change. We need to wait
         * for our own records to register until the host name is
         * properly esatblished. */
        if (serviceData->entryGroup)
        {
            avahi_entry_group_reset(serviceData->entryGroup);
        }
        break;
    }
    case AVAHI_CLIENT_CONNECTING:
    default:
        break;
    }
}

void ARDISCOVERY_AvahiDiscovery_Publish(ARDISCOVERY_AvahiDiscovery_PublisherData_t* serviceData)
{
    AvahiClient *client = NULL;
    int avahiError;
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    if (serviceData == NULL)
    {
        ERR("Null parameter");
        error = ARDISCOVERY_ERROR;
    }

    if (error == ARDISCOVERY_OK)
    {
        /* Allocate main loop object */
        serviceData->simplePoll = avahi_simple_poll_new();
        if (serviceData->simplePoll == NULL)
        {
            error = ARDISCOVERY_ERROR_SIMPLE_POLL;
        }
    }

    if (error == ARDISCOVERY_OK)
    {
        /* Build service name */
        serviceData->serviceName = ARDISCOVERY_AvahiDiscovery_BuildName();
        if (serviceData->serviceName == NULL)
        {
            error = ARDISCOVERY_ERROR_BUILD_NAME;
        }
    }

    if (error == ARDISCOVERY_OK)
    {
        /* Allocate a new client */
        client = avahi_client_new(avahi_simple_poll_get(serviceData->simplePoll), 0, ARDISCOVERY_AvahiDiscovery_ClientCb, serviceData, &avahiError);
        if (client == NULL)
        {
            ERR("Failed to create client: %s\n", avahi_strerror(avahiError));
            error = ARDISCOVERY_ERROR_CLIENT;
        }
    }

    if (error == ARDISCOVERY_OK)
    {
        /* Run the main loop */
        avahi_simple_poll_loop(serviceData->simplePoll);
    }

    if (error == ARDISCOVERY_OK)
    {
        /* Main loop exited, cleanup */
        if (client)
        {
            avahi_client_free(client);
        }
        if (serviceData)
        {
            if (serviceData->simplePoll)
            {
                avahi_simple_poll_free(serviceData->simplePoll);
            }
            avahi_free(serviceData->serviceName);
        }
    }

    if (error != ARDISCOVERY_OK)
    {
        ERR("error: %s", ARDISCOVERY_Error_ToString (error));
    }
}

void ARDISCOVERY_AvahiDiscovery_StopPublishing(ARDISCOVERY_AvahiDiscovery_PublisherData_t* serviceData)
{
    if (serviceData == NULL)
    {
        return;
    }
    /*
     * Stop publishing service
     */
    avahi_simple_poll_quit(serviceData->simplePoll);
}

void ARDISCOVERY_AvahiDiscovery_Publisher_Delete(ARDISCOVERY_AvahiDiscovery_PublisherData_t** serviceDataPtrAddr)
{
    /*
     * Free discovery data
     */
    ARDISCOVERY_AvahiDiscovery_PublisherData_t *serviceDataPtr = NULL;

    if (serviceDataPtrAddr != NULL)
    {
        serviceDataPtr = *serviceDataPtrAddr;

        if (serviceDataPtr != NULL)
        {
            ARDISCOVERY_AvahiDiscovery_StopPublishing(serviceDataPtr);

            if (serviceDataPtr->serviceName)
            {
                free(serviceDataPtr->serviceName);
                serviceDataPtr->serviceName = NULL;
            }
            if (serviceDataPtr->serviceType)
            {
                free(serviceDataPtr->serviceType);
                serviceDataPtr->serviceType = NULL;
            }

            free(serviceDataPtr);
            serviceDataPtr = NULL;
        }

        *serviceDataPtrAddr = NULL;
    }
}

/*
 * Browser
 */
