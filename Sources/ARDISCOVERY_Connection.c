#include <errno.h>
#include <stdlib.h>

#include <libARSAL/ARSAL_Print.h>
#include <libARSAL/ARSAL_Socket.h>

#include <libARDiscovery/ARDISCOVERY_Connection.h>
#include "ARDISCOVERY_Connection.h"

#define ARDISCOVERY_CONNECTION_TAG "ARDISCOVERY_Connection"

#define ERR(...)    ARSAL_PRINT(ARSAL_PRINT_ERROR, ARDISCOVERY_CONNECTION_TAG, __VA_ARGS__)
#define SAY(...)    ARSAL_PRINT(ARSAL_PRINT_WARNING, ARDISCOVERY_CONNECTION_TAG, __VA_ARGS__)

/*************************
 * Private header
 *************************/

/**
 * @brief Initialize a socket
 * @param[in] socket socket to intialize
 * @return error during execution
 */
static eARDISCOVERY_ERROR ARDISCOVERY_Connection_CreateSocket (int *socket);

/**
 * @brief Initialize the TCP socket for sending
 * On controller, port is self known and connecting is done first.
 * On device, port is known once received from controller.
 * @param[in] deviceSocket socket used by the device
 * @param[in] port port to receive
 * @return error during execution
 */
static eARDISCOVERY_ERROR ARDISCOVERY_Connection_InitDeviceSocket (int *deviceSocket, int port);

/**
 * @brief Connect the Controller TCP socket
 * On controller, port is self known and connecting is done first.
 * On device, port is known once received from controller.
 * @param[in] connectionData connection data
 * @param[in] port port to receive
 * @param[in] ip IP to connect to
 * @return error during execution
 */
static eARDISCOVERY_ERROR ARDISCOVERY_Connection_InitControllerSocket (ARDISCOVERY_Connection_ConnectionData_t *connectionData, int port, const char *ip);

/**
 * @brief receive the connection data
 * @param[in] connectionData connection data
 * @return error during execution
 */
static eARDISCOVERY_ERROR ARDISCOVERY_Connection_RxPending (ARDISCOVERY_Connection_ConnectionData_t *connectionData);

/**
 * @brief send the connection data
 * @param[in] connectionData connection data
 * @return error during execution
 */
static eARDISCOVERY_ERROR ARDISCOVERY_Connection_TxPending (ARDISCOVERY_Connection_ConnectionData_t *connectionData);


/*************************
 * Implementation
 *************************/

ARDISCOVERY_Connection_ConnectionData_t* ARDISCOVERY_Connection_New (ARDISCOVERY_Connection_SendJsonCallback_t sendJsonCallback, ARDISCOVERY_Connection_ReceiveJsonCallback_t receiveJsonCallback, void *customData, eARDISCOVERY_ERROR *error)
{
    /* - Create and initialize connection data - */
    
    ARDISCOVERY_Connection_ConnectionData_t *connectionData = NULL;
    eARDISCOVERY_ERROR localError = ARDISCOVERY_OK;
    
    /* Check parameter */
    if ((sendJsonCallback == NULL) || (receiveJsonCallback == NULL))
    {
        localError = ARDISCOVERY_ERROR_BAD_PARAMETER;
    }
    
    if (localError == ARDISCOVERY_OK)
    {
        connectionData = malloc(sizeof(ARDISCOVERY_Connection_ConnectionData_t));
        if (connectionData != NULL)
        {
            /* initialize connectionData */
            connectionData->txData.buffer = NULL;
            connectionData->txData.size = 0;
            connectionData->rxData.buffer = NULL;
            connectionData->rxData.size = 0;
            connectionData->isClosing = 0;
            connectionData->sendJsoncallback = sendJsonCallback;
            connectionData->receiveJsoncallback = receiveJsonCallback;
            connectionData->customData = customData;
            connectionData->socket = -1;
            memset(&(connectionData->address), 0, sizeof(connectionData->address));
        }
        else
        {
            localError = ARDISCOVERY_ERROR_ALLOC;
        }
    }
    
    /* Allocate reception buffer */
    if (localError == ARDISCOVERY_OK)
    {
        connectionData->rxData.buffer = (uint8_t *) malloc (sizeof(uint8_t) * ARDISCOVERY_CONNECTION_RX_BUFFER_SIZE);
        if (connectionData->rxData.buffer != NULL)
        {
            connectionData->rxData.size = 0;
        }
        else
        {
            localError = ARDISCOVERY_ERROR_ALLOC;
        }
    }
    
    /* Allocate transmission buffer */
    if (localError == ARDISCOVERY_OK)
    {
        connectionData->txData.buffer = (uint8_t *) malloc(sizeof(uint8_t) * ARDISCOVERY_CONNECTION_TX_BUFFER_SIZE);
        if (connectionData->txData.buffer != NULL)
        {
            connectionData->txData.size = 0;
        }
        else
        {
            localError = ARDISCOVERY_ERROR_ALLOC;
        }
    }
    
    /* Delete connection data if an error occurred */
    if (localError != ARDISCOVERY_OK)
    {
        ERR("error: %s", ARDISCOVERY_Error_ToString (localError));
        ARDISCOVERY_Connection_Delete (&connectionData);
    }

    /* return error */
    if (error != NULL)
    {
        *error = localError;
    }

    return connectionData;
}

void ARDISCOVERY_Connection_Delete (ARDISCOVERY_Connection_ConnectionData_t **connectionData)
{
    /*
     * Free connection data
     */
     
    if (connectionData != NULL)
    {
        if ((*connectionData) != NULL)
        {
            if ((*connectionData)->txData.buffer)
            {
                free((*connectionData)->txData.buffer);
                (*connectionData)->txData.buffer = NULL;
                (*connectionData)->txData.size = 0;
            }
            if ((*connectionData)->rxData.buffer)
            {
                free((*connectionData)->rxData.buffer);
                (*connectionData)->rxData.buffer = NULL;
                (*connectionData)->rxData.size = 0;
            }

            free (*connectionData);
            (*connectionData) = NULL;
        }
    }
}

eARDISCOVERY_ERROR ARDISCOVERY_Connection_OpenAsDevice (ARDISCOVERY_Connection_ConnectionData_t *connectionData, int port)
{
    /*
     * Initialize connection
     */
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;
    
    int deviceSocket = 0;
    socklen_t clientLen = sizeof (connectionData->address);

    /* Check parameter */
    if (connectionData == NULL)
    {
        error = ARDISCOVERY_ERROR_BAD_PARAMETER;
    }

    if (error == ARDISCOVERY_OK)
    {
        /* initilize the server socket */
        error = ARDISCOVERY_Connection_InitDeviceSocket (&deviceSocket, port);
    }
    
    if (error == ARDISCOVERY_OK)
    {
        /* while the connection is not closed */
        while (connectionData->isClosing != 1)
        {
            /* reinitilize error for all connection */
            error = ARDISCOVERY_OK;

            ARSAL_PRINT(ARSAL_PRINT_DEBUG, ARDISCOVERY_CONNECTION_TAG, "Device waits to accept a socket");
            
            /* Wait for any incoming connection from controller */
            connectionData->socket = ARSAL_Socket_Accept (deviceSocket, (struct sockaddr*) &(connectionData->address), &clientLen);
            
            if (connectionData->socket < 0)
            {
                ERR("accept() failed: %s", strerror(errno));
                error = ARDISCOVERY_ERROR_ACCEPT;
            }
        
            if (error == ARDISCOVERY_OK)
            {
                ARSAL_PRINT(ARSAL_PRINT_DEBUG, ARDISCOVERY_CONNECTION_TAG, "Device accepts a socket");
                
                /* receiption  */
                error = ARDISCOVERY_Connection_RxPending (connectionData);
            }
            
            if (error == ARDISCOVERY_OK)
            {
                /* sending */
                error = ARDISCOVERY_Connection_TxPending (connectionData);
            }
            
            
            if (error != ARDISCOVERY_OK)
            {
                /* print the error occurred */
                ERR("error: %s", ARDISCOVERY_Error_ToString (error));
            }
        }
    }
    
    /* close deviceSocket */
    ARSAL_Socket_Close (deviceSocket);

    return error;
}

eARDISCOVERY_ERROR ARDISCOVERY_Connection_OpenAsController (ARDISCOVERY_Connection_ConnectionData_t *connectionData, int port, const char *ip)
{
    /*
     * Initialize connection
     */
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    /* Check parameter */
    if (connectionData == NULL)
    {
        error = ARDISCOVERY_ERROR_BAD_PARAMETER;
    }

    if (error == ARDISCOVERY_OK)
    {
        /* Start by contacting the device we're interested in */
        error = ARDISCOVERY_Connection_InitControllerSocket (connectionData, port, ip);
    }
    
    if (error == ARDISCOVERY_OK)
    {
        /* sending */
        error = ARDISCOVERY_Connection_TxPending (connectionData);
    }
    
    if (error == ARDISCOVERY_OK)
    {
        /* receiption  */
        error = ARDISCOVERY_Connection_RxPending (connectionData);
    }
    
    return error;
}

void ARDISCOVERY_Connection_Close (ARDISCOVERY_Connection_ConnectionData_t *connectionData)
{
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;
    
    /* Check parameter */
    if (connectionData == NULL)
    {
        error = ARDISCOVERY_ERROR_BAD_PARAMETER;
    }
    
    if (error == ARDISCOVERY_OK)
    {
        /* Stop reception */
        connectionData->isClosing = 1;
        
        ARSAL_Socket_Close (connectionData->socket);
    }
}

/*************************
 * local Implementation
 *************************/

static eARDISCOVERY_ERROR ARDISCOVERY_Connection_CreateSocket (int *socket)
{
    /*
     * Create TCP socket
     */
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;
    int optval = 1;
    
    *socket = ARSAL_Socket_Create(AF_INET, SOCK_STREAM, 0);
    if (*socket < 0)
    {
        error = ARDISCOVERY_ERROR_SOCKET_CREATION;
    }
    else
    {
        if (setsockopt (*socket, SOL_SOCKET, SO_REUSEADDR, &optval, sizeof optval) != 0)
        {
            error = ARDISCOVERY_ERROR_SOCKET_PERMISSION_DENIED;
        }
    }
    
    return error;
}

static eARDISCOVERY_ERROR ARDISCOVERY_Connection_InitDeviceSocket (int *deviceSocket, int port)
{
    /*
     * Bind TCP socket for receiving
     * On device, port is self known and listening is done first.
     */
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;
    struct sockaddr_in recvSin;
    int errorBind, errorListen = 0;

    /* Create TCP socket */
    if (error == ARDISCOVERY_OK)
    {
        error = ARDISCOVERY_Connection_CreateSocket (deviceSocket);
    }

    /* Init socket */
    if (error == ARDISCOVERY_OK)
    {
        /* Server side (device) : listen to anyone */
        recvSin.sin_addr.s_addr = htonl(INADDR_ANY);
        
        recvSin.sin_family = AF_INET;
        recvSin.sin_port = htons(port);

        errorBind = ARSAL_Socket_Bind (*deviceSocket, (struct sockaddr*) &recvSin, sizeof (recvSin));

        if (errorBind != 0)
        {
            ERR("bind() failed: %s", strerror(errno));

            switch (errno)
            {
            case EACCES:
                error = ARDISCOVERY_ERROR_SOCKET_PERMISSION_DENIED;
                break;

            default:
                error = ARDISCOVERY_ERROR;
                break;
            }
        }

        errorListen = ARSAL_Socket_Listen (*deviceSocket, 10);

        if (errorListen != 0)
        {
            ERR("listen() failed: %s", strerror(errno));

            switch (errno)
            {
            case EINVAL:
                error = ARDISCOVERY_ERROR_SOCKET_ALREADY_CONNECTED;
                break;

            default:
                error = ARDISCOVERY_ERROR;
                break;
            }
        }
    }

    return error;
}

static eARDISCOVERY_ERROR ARDISCOVERY_Connection_InitControllerSocket (ARDISCOVERY_Connection_ConnectionData_t *connectionData, int port, const char *ip)
{
    /*
     * On controller, port is known once received from device.
     */
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;
    int connectError;

    /* Create TCP socket */
    if (error == ARDISCOVERY_OK)
    {
        error = ARDISCOVERY_Connection_CreateSocket (&(connectionData->socket));
    }

    /* Init socket */
    if (error == ARDISCOVERY_OK)
    {
        /* Client side (controller) : listen to the device we chose */
        connectionData->address.sin_addr.s_addr = inet_addr (ip);   
        connectionData->address.sin_family = AF_INET;
        connectionData->address.sin_port = htons (port);
        
        ARSAL_PRINT(ARSAL_PRINT_DEBUG, ARDISCOVERY_CONNECTION_TAG, "contoller try to connect ip:%s port:%d", ip, port);
        
        connectError = ARSAL_Socket_Connect (connectionData->socket, (struct sockaddr*) &(connectionData->address), sizeof (connectionData->address));
        
        if (connectError != 0)
        {
            ERR("connect() failed: %s", strerror(errno));
            
            switch (errno)
            {
            case EACCES:
                error = ARDISCOVERY_ERROR_SOCKET_PERMISSION_DENIED;
                break;

            default:
                error = ARDISCOVERY_ERROR;
                break;
            }
        }
    }
    
    return error;
}

static eARDISCOVERY_ERROR ARDISCOVERY_Connection_RxPending (ARDISCOVERY_Connection_ConnectionData_t *connectionData)
{
    /* - read connection data - */
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;
    
    /* Read content from incoming connection */
    int readSize = ARSAL_Socket_Recv (connectionData->socket, connectionData->rxData.buffer, ARDISCOVERY_CONNECTION_RX_BUFFER_SIZE, 0);
    
    ARSAL_PRINT(ARSAL_PRINT_DEBUG, ARDISCOVERY_CONNECTION_TAG, "data read size: %d", readSize);
    
    if (readSize > 0)
    {
        /* set the rxdata size */
        connectionData->rxData.size = readSize;
        
        ARSAL_PRINT(ARSAL_PRINT_DEBUG, ARDISCOVERY_CONNECTION_TAG, "data read: %s", connectionData->rxData.buffer);
        
        /* receive callback */
        error = connectionData->receiveJsoncallback (connectionData->rxData.buffer, connectionData->rxData.size, inet_ntoa(connectionData->address.sin_addr), connectionData->customData);
    }
    
    return error;
}

static eARDISCOVERY_ERROR ARDISCOVERY_Connection_TxPending (ARDISCOVERY_Connection_ConnectionData_t *connectionData)
{
    /* - send connection data - */
    
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;
    ssize_t sendSize = 0;
    
    /* sending callback */
    error = connectionData->sendJsoncallback (connectionData->txData.buffer, &(connectionData->txData.size), connectionData->customData);
    
    ARSAL_PRINT(ARSAL_PRINT_DEBUG, ARDISCOVERY_CONNECTION_TAG, "data send size: %d", connectionData->txData.size);
    
    /* check the txData size */
    if ((error == ARDISCOVERY_OK) && (connectionData->txData.size > 0) && (connectionData->txData.size <= ARDISCOVERY_CONNECTION_TX_BUFFER_SIZE))
    {
        ARSAL_PRINT(ARSAL_PRINT_DEBUG, ARDISCOVERY_CONNECTION_TAG, "data send: %s", connectionData->txData.buffer);
        
        /* Send txData */
        sendSize = ARSAL_Socket_Send(connectionData->socket, connectionData->txData.buffer, connectionData->txData.size, 0);
        if (sendSize < 0)
        {
            error = ARDISCOVERY_ERROR_SEND;
        }
    }
    
    return error;
}


