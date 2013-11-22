#include <stdio.h>

#include <libARSAL/ARSAL_Print.h>

#include "libARDiscovery/ARDISCOVERY_AvahiDiscovery.h"
#include "libARDiscovery/ARDISCOVERY_Connection.h"
#include "libARDiscovery/ARDISCOVERY_Error.h"

#define __ARDISCOVERY_AVAHIDISCOVERY_TAG__ "ARDISCOVERY_AvahiDiscovery"

#define ERR(...)    ARSAL_PRINT(ARSAL_PRINT_ERROR, __ARDISCOVERY_AVAHIDISCOVERY_TAG__, __VA_ARGS__)
#define SAY(...)    ARSAL_PRINT(ARSAL_PRINT_WARNING, __ARDISCOVERY_AVAHIDISCOVERY_TAG__, __VA_ARGS__)

static int ARDISCOVERY_AvahiDiscovery_CreateService(AvahiClient *c, ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData);

static char* ARDISCOVERY_AvahiDiscovery_BuildName(void)
{
    /* Get hostname and build the final name. */
    char hostname[HOST_NAME_MAX + 1]; /* POSIX hostname max length + the null terminating byte. */
    int error = gethostname(hostname, sizeof(hostname));
    if (error == 0)
    {
        hostname[sizeof(hostname) - 1] = '\0';
        return strdup(hostname);
    }
    return NULL;
}

static void ARDISCOVERY_AvahiDiscovery_EntryGroupCb(AvahiEntryGroup* g, AvahiEntryGroupState state, void* userdata)
{
    ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData = (ARDISCOVERY_AvahiDiscovery_ServiceData_t*) userdata;

    if (g == NULL || serviceData == NULL)
    {
        ERR("Invalid parameters");
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
            char* n = avahi_alternative_service_name(serviceData->serviceName);
            avahi_free(serviceData->serviceName);
            serviceData->serviceName = n;

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

static eARDISCOVERY_ERROR ARDISCOVERY_AvahiDiscovery_CreateService(AvahiClient* c, ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData)
{
    int ret;
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    if (c == NULL || serviceData == NULL)
    {
        ERR("Invalid parameters");
        error = ARDISCOVERY_ERROR_SERVICE_CREATION;
    }

    if (error == ARDISCOVERY_OK)
    {
        /* If this is the first time we're called, let's create a new entry group */
        if (!serviceData->entryGroup)
        {
            serviceData->entryGroup = avahi_entry_group_new(c, ARDISCOVERY_AvahiDiscovery_EntryGroupCb, (void*) serviceData);
            if (!serviceData->entryGroup)
            {
                ERR("avahi_entry_group_new() failed: %s", avahi_strerror(avahi_client_errno(c)));
                error = ARDISCOVERY_ERROR_ENTRY_GROUP;
            }
        }
    }

    if (error == ARDISCOVERY_OK)
    {
        /* Add the service for AR.Drone */
        ret = avahi_entry_group_add_service(serviceData->entryGroup, AVAHI_IF_UNSPEC, AVAHI_PROTO_UNSPEC, 0, serviceData->serviceName,
                serviceData->serviceType, ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_NETWORK, NULL, serviceData->devicePort, NULL, NULL);
        if (ret < 0)
        {
            ERR("Failed to add %s service: %s\n", serviceData->serviceType, avahi_strerror(ret));
            error = ARDISCOVERY_ERROR_ADD_SERVICE;
        }
    }

    /* Tell the server to register the service */
    if (error == ARDISCOVERY_OK)
    {
        ret = avahi_entry_group_commit(serviceData->entryGroup);
        if (ret < 0)
        {
            ERR("Failed to commit entry_group: %s", avahi_strerror(ret));
            error = ARDISCOVERY_ERROR_GROUP_COMMIT;
        }
    }

    if (error != ARDISCOVERY_OK)
    {
        ERR("Quitting simple poll");
        avahi_simple_poll_quit(serviceData->simplePoll);
    }

    return error;
}

static void ARDISCOVERY_AvahiDiscovery_ClientCb(AvahiClient* c, AvahiClientState state, void* userdata)
{
    ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData = (ARDISCOVERY_AvahiDiscovery_ServiceData_t*) userdata;
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    if (c == NULL || serviceData == NULL)
    {
        ERR("Invalid parameters");
        return;
    }

    /* Called whenever the client or server state changes */

    switch (state)
    {
        case AVAHI_CLIENT_S_RUNNING:
        {
            /* The server has startup successfully and registered its host
             * name on the network, so it's time to create our services */
            if (!serviceData->entryGroup)
            {
                error = ARDISCOVERY_AvahiDiscovery_CreateService(c, serviceData);
                if (error != ARDISCOVERY_OK)
                {
                    ERR("Error creating service");
                }
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

void ARDISCOVERY_AvahiDiscovery_Init(ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData)
{
    /* Init Avahi data */
    serviceData->entryGroup = NULL;
    serviceData->simplePoll = NULL;
    serviceData->devicePort = ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_PUBLISHED_PORT;
}

void ARDISCOVERY_AvahiDiscovery_Publish(ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData)
{
    AvahiClient *client = NULL;
    int avahiError;
    int shouldTerminate = 0;
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    if (serviceData == NULL)
    {
        ERR("Invalid parameters");
        error = ARDISCOVERY_ERROR;
    }

    if (error == ARDISCOVERY_OK)
    {
        /* Allocate main loop object */
        serviceData->simplePoll = avahi_simple_poll_new();
        if (serviceData->simplePoll == NULL)
        {
            ERR("Failed to create simple poll object.");
            error = ARDISCOVERY_ERROR_SIMPLE_POLL;
        }
    }

    if (error == ARDISCOVERY_OK)
    {
        /* Build service name */
        serviceData->serviceName = ARDISCOVERY_AvahiDiscovery_BuildName();
        if (serviceData->serviceName == NULL)
        {
            ERR("Failed to build service name.");
            error = ARDISCOVERY_ERROR_BUILD_NAME;
        }
    }

    if (error == ARDISCOVERY_OK)
    {
        /* Allocate a new client */
        client = avahi_client_new(avahi_simple_poll_get(serviceData->simplePoll), 0, ARDISCOVERY_AvahiDiscovery_ClientCb, serviceData, &avahiError);
        if (client = NULL)
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
}

void ARDISCOVERY_AvahiDiscovery_StopPublishing(ARDISCOVERY_AvahiDiscovery_ServiceData_t* serviceData)
{
    avahi_simple_poll_quit(serviceData->simplePoll);
}
