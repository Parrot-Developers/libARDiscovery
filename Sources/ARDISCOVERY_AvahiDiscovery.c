#include <stdio.h>
#include <pthread.h>

#include <libARSAL/ARSAL_Print.h>
#include <libARDiscovery/ARDISCOVERY_AvahiDiscovery.h>

#include <avahi-client/client.h>
#include <avahi-client/publish.h>

#include <avahi-common/alternative.h>
#include <avahi-common/simple-watch.h>
#include <avahi-common/malloc.h>
#include <avahi-common/error.h>
#include <avahi-common/timeval.h>

#define __TAG__ "ARDiscovery_Avahi"

#define AVAHI_DEBUG 0
#if AVAHI_DEBUG
#  define  D(...)   ARSAL_PRINT(ARSAL_PRINT_DEBUG, __TAG__, __VA_ARGS__)
#else
#  define  D(...)   ((void)0)
#endif

#define ERR(...)    ARSAL_PRINT(ARSAL_PRINT_ERROR, __TAG__, __VA_ARGS__)
#define SAY(...)    ARSAL_PRINT(ARSAL_PRINT_WARNING, __TAG__, __VA_ARGS__)

static pthread_t ARDISCOVERY_AvahiDiscovery_ThreadObj;

static AvahiEntryGroup *entryGroup = NULL;
static AvahiSimplePoll *simplePoll = NULL;

static ARDISCOVERY_AvahiDiscovery_ServiceData_t serviceData;

static void ARDISCOVERY_AvahiDiscovery_CreateService(AvahiClient *c);

static char* ARDISCOVERY_AvahiDiscovery_BuildName(void)
{
    /* Get hostname and build the final name. */
    char hostname[HOST_NAME_MAX + 1]; /* POSIX hostname max length + the null terminating byte. */
    int error = gethostname(hostname, sizeof(hostname));
    if (error == 0)
    {
        hostname[sizeof(hostname) - 1] = '\0';

        char finalname[HOST_NAME_MAX + 128];
        snprintf(finalname, sizeof(finalname) - 1, "%s", hostname);
        finalname[sizeof(finalname) - 1] = '\0';

        return strdup(finalname);
    }
    return NULL;
}

static void ARDISCOVERY_AvahiDiscovery_EntryGroupCb(AvahiEntryGroup *g, AvahiEntryGroupState state, AVAHI_GCC_UNUSED void *userdata)
{
    assert(g == entryGroup || entryGroup == NULL);

    /* Called whenever the entry group state changes */

    switch (state)
    {
        case AVAHI_ENTRY_GROUP_ESTABLISHED :
        {
            /* The entry group has been established successfully */
            SAY("Service '%s' successfully established.", serviceData.serviceName);
            break;
        }
        case AVAHI_ENTRY_GROUP_COLLISION :
        {
            char *n;

            /* A service name collision happened. Let's pick a new name */
            n = avahi_alternative_service_name(serviceData.serviceName);
            avahi_free(serviceData.serviceName);
            serviceData.serviceName = n;

            ERR("Service name collision, renaming service to '%s'", serviceData.serviceName);

            /* And recreate the services */
            ARDISCOVERY_AvahiDiscovery_CreateService(avahi_entry_group_get_client(g));
            break;
        }

        case AVAHI_ENTRY_GROUP_FAILURE :
        {
            ERR("Entry group failure: %s", avahi_strerror(avahi_client_errno(avahi_entry_group_get_client(g))));

            /* Some kind of failure happened while we were registering our services */
            avahi_simple_poll_quit(simplePoll);
            break;
        }
        case AVAHI_ENTRY_GROUP_UNCOMMITED:
        case AVAHI_ENTRY_GROUP_REGISTERING:
        ;
    }
}

static void ARDISCOVERY_AvahiDiscovery_CreateService(AvahiClient *c)
{
    int ret;
    int creationFailed = 0;
    assert(c);

    /* If this is the first time we're called, let's create a new entry group */
    if (!entryGroup)
    {
        if (!(entryGroup = avahi_entry_group_new(c, ARDISCOVERY_AvahiDiscovery_EntryGroupCb, NULL)))
        {
            ERR("avahi_entry_group_new() failed: %s", avahi_strerror(avahi_client_errno(c)));
            creationFailed = 1;
        }
    }

    if (!creationFailed)
    {
        /* Add the service for AR.Drone */
        if ((ret = avahi_entry_group_add_service(entryGroup, AVAHI_IF_UNSPEC, AVAHI_PROTO_UNSPEC, 0,
                serviceData.serviceName, serviceData.serviceType, ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_NETWORK, NULL,
                ARDISCOVERY_AVAHIDISCOVERY_DEFAULT_OUTPUT_PORT, NULL, NULL)) < 0)
        {
            ERR("Failed to add %s service: %s\n", serviceData.serviceType, avahi_strerror(ret));
            creationFailed = 1;
        }
    }

    /* Tell the server to register the service */
    if (!creationFailed && (ret = avahi_entry_group_commit(entryGroup)) < 0)
    {
        ERR("Failed to commit entry_group: %s", avahi_strerror(ret));
        creationFailed = 1;
    }

    if (creationFailed)
    {
        ERR("Quitting simple poll");
        avahi_simple_poll_quit(simplePoll);
    }
}

static void ARDISCOVERY_AvahiDiscovery_ClientCb(AvahiClient *c, AvahiClientState state, AVAHI_GCC_UNUSED void * userdata)
{
    assert(c);

    /* Called whenever the client or server state changes */

    switch (state)
    {
        case AVAHI_CLIENT_S_RUNNING:
        {
            /* The server has startup successfully and registered its host
             * name on the network, so it's time to create our services */
            if (!entryGroup)
            {
                ARDISCOVERY_AvahiDiscovery_CreateService(c);
            }
            break;
        }
        case AVAHI_CLIENT_FAILURE:
        {
            ERR("Client failure: %s", avahi_strerror(avahi_client_errno(c)));
            avahi_simple_poll_quit(simplePoll);
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
            if (entryGroup)
            {
                avahi_entry_group_reset(entryGroup);
            }
            break;
        }
        case AVAHI_CLIENT_CONNECTING:
        ;
    }
}

void* ARDISCOVERY_AvahiDiscovery_ThreadHandler(void* data)
{
    AvahiClient *client = NULL;
    int avahiError;
    int shouldTerminate = 0;
    char* serviceType = (char*) data;

    /* Allocate main loop object */
    if (!(simplePoll = avahi_simple_poll_new()))
    {
        ERR("Failed to create simple poll object.");
        shouldTerminate = 1;
    }

    /* Service type is provided by upper config */
    serviceData.serviceType = serviceType;

    serviceData.serviceName = ARDISCOVERY_AvahiDiscovery_BuildName();
    if (serviceData.serviceName == NULL)
    {
        ERR("Failed to build service name.");
        shouldTerminate = 1;
    }

    if (!shouldTerminate)
    {
        /* Allocate a new client */
        client = avahi_client_new(avahi_simple_poll_get(simplePoll), 0, ARDISCOVERY_AvahiDiscovery_ClientCb, NULL, &avahiError);

        /* Check whether creating the client object succeeded */
        if (!client)
        {
            ERR("Failed to create client: %s\n", avahi_strerror(avahiError));
            shouldTerminate = 1;
        }
    }

    if (!shouldTerminate)
    {
        /* Run the main loop */
        avahi_simple_poll_loop(simplePoll);
    }

    /* Error, end with cleanup */
    if (shouldTerminate)
    {
        if (client)
        {
            avahi_client_free(client);
        }

        if (simplePoll)
        {
            avahi_simple_poll_free(simplePoll);
        }
    }

    avahi_free(serviceData.serviceName);
}

void ARDISCOVERY_AvahiDiscovery_Start(char* serviceType)
{
    pthread_create (&ARDISCOVERY_AvahiDiscovery_ThreadObj, NULL, ARDISCOVERY_AvahiDiscovery_ThreadHandler, (void*) serviceType);
}

void ARDISCOVERY_AvahiDiscovery_Stop(void)
{
    avahi_simple_poll_quit(simplePoll);
    pthread_join(ARDISCOVERY_AvahiDiscovery_ThreadObj, NULL);
}
