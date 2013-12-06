#include <errno.h>
#include <stdlib.h>

#include <libARSAL/ARSAL_Print.h>
#include <libARSAL/ARSAL_Socket.h>

#include <libARDiscovery/ARDISCOVERY_Connection.h>
#include "ARDISCOVERY_Connection.h"

#define __ARDISCOVERY_CONNECTION_TAG__ "ARDISCOVERY_Connection"

#define ERR(...)    ARSAL_PRINT(ARSAL_PRINT_ERROR, __ARDISCOVERY_CONNECTION_TAG__, __VA_ARGS__)
#define SAY(...)    ARSAL_PRINT(ARSAL_PRINT_WARNING, __ARDISCOVERY_CONNECTION_TAG__, __VA_ARGS__)

/*
 * Private header
 */

/**
 * @brief Bind TCP socket for receiving.
 * On device, port is self known and listening is done first.
 * On controller, port is known once received from device.
 * @param[in] TxData Transmission data
 * @param[in] ipAddr IP to connect to
 * @return error during execution
 */
static eARDISCOVERY_ERROR ARDISCOVERY_Connection_InitTx(ARDISCOVERY_Connection_ComData_t* TxData, uint8_t* ipAddr);

/**
 * @brief Connect TCP socket for sending
 * On controller, port is self known and connecting is done first.
 * On device, port is known once received from controller.
 * @param[in] RxData Reception data
 * @param[in] ipAddr IP to connect to
 * @return error during execution
 */
static eARDISCOVERY_ERROR ARDISCOVERY_Connection_InitRx(ARDISCOVERY_Connection_ComData_t* RxData, uint8_t* ipAddr);

/**
 * @brief Manage action regarding current connection state
 * @param[in] connectionData connection data
 * @param[in] ipAddr IP address of current negociation
 * @return error during execution
 */
static eARDISCOVERY_ERROR ARDISCOVERY_Connection_StateMachine(ARDISCOVERY_Connection_ConnectionData_t* connectionData, uint8_t* ipAddr);

/**
 * @brief Send a connection frame
 * @param[in] TxData Data to send
 * @param[in] ipAddr IP address to connect to
 * @return error during execution
 */
static eARDISCOVERY_ERROR ARDISCOVERY_Connection_Sending(ARDISCOVERY_Connection_ComData_t* TxData, uint8_t* ipAddr);

/*
 * Implementation
 */

ARDISCOVERY_Connection_ConnectionData_t* ARDISCOVERY_Connection_New(ARDISCOVERY_Connection_Callback_t callback, void* customData, eARDISCOVERY_ERROR* errorPtr)
{
    /*
     * Create and initialize connection data
     */
    ARDISCOVERY_Connection_ConnectionData_t *connectionData = NULL;
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    connectionData = malloc(sizeof(ARDISCOVERY_Connection_ConnectionData_t));
    if (connectionData != NULL)
    {
        connectionData->callback = callback;
        connectionData->customData = customData;
        connectionData->state = ARDISCOVERY_STATE_NONE;

        /* Allocate reception buffer */
        if (error == ARDISCOVERY_OK)
        {
            connectionData->RxData.buffer = (uint8_t *) malloc(sizeof(uint8_t) * ARDISCOVERY_CONNECTION_RX_BUFFER_SIZE);
            if (connectionData->RxData.buffer != NULL)
            {
                connectionData->RxData.size = 0;
            }
            else
            {
                error = ARDISCOVERY_ERROR_ALLOC;
            }
        }

        /* Allocate transmission buffer */
        if (error == ARDISCOVERY_OK)
        {
            connectionData->TxData.buffer = (uint8_t *) malloc(sizeof(uint8_t) * ARDISCOVERY_CONNECTION_TX_BUFFER_SIZE);
            if (connectionData->TxData.buffer != NULL)
            {
                connectionData->TxData.size = 0;
            }
            else
            {
                error = ARDISCOVERY_ERROR_ALLOC;
            }
        }
    }
    else
    {
        error = ARDISCOVERY_ERROR_ALLOC;
    }

    /* Delete connection data if an error occurred */
    if (error != ARDISCOVERY_OK)
    {
        ERR("error: %s", ARDISCOVERY_Error_ToString (error));
        ARDISCOVERY_Connection_Delete (&connectionData);
    }

    if (errorPtr != NULL)
    {
        *errorPtr = error;
    }

    return connectionData;
}

eARDISCOVERY_ERROR ARDISCOVERY_Connection_Open(ARDISCOVERY_Connection_ConnectionData_t* connectionData, uint32_t initAs, uint8_t* ipAddr)
{
    /*
     * Initialize connection
     */
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    /* Check parameter */
    if (connectionData == NULL)
    {
        ERR("Null parameter");
        error = ARDISCOVERY_ERROR;
    }

    /* Connect As */
    if (error == ARDISCOVERY_OK)
    {

        switch (initAs)
        {
            case ARDISCOVERY_CONNECTION_INIT_AS_DEVICE:
            {
                connectionData->RxData.port = ARDISCOVERY_CONNECTION_TCP_C2D_PORT;
                connectionData->TxData.port = ARDISCOVERY_CONNECTION_TCP_D2C_PORT;
                /* Wait for any controller to contact us */
                /* ipAddr should be null */
                error = ARDISCOVERY_Connection_InitRx(&connectionData->RxData, ipAddr);
                if (error == ARDISCOVERY_OK)
                {
                    connectionData->state = ARDISCOVERY_STATE_RX_PENDING;
                }
                break;
            }
            case ARDISCOVERY_CONNECTION_INIT_AS_CONTROLLER:
            {
                connectionData->TxData.port = ARDISCOVERY_CONNECTION_TCP_C2D_PORT;
                connectionData->RxData.port = ARDISCOVERY_CONNECTION_TCP_D2C_PORT;
                /* Start by contacting the device we're interested in */
                error = ARDISCOVERY_Connection_InitTx(&connectionData->TxData, ipAddr);
                if (error == ARDISCOVERY_OK)
                {
                    /* Send request */
                    connectionData->state = ARDISCOVERY_STATE_TX_PENDING;
                    error = ARDISCOVERY_Connection_StateMachine(connectionData, ipAddr);
                }
                break;
            }
            default:
            {
                error = ARDISCOVERY_ERROR_INIT;
                break;
            }
        }
    }

    if (error != ARDISCOVERY_OK)
    {
        ERR("error: %s", ARDISCOVERY_Error_ToString (error));
    }

    return error;
}

static eARDISCOVERY_ERROR ARDISCOVERY_Connection_InitRx(ARDISCOVERY_Connection_ComData_t* RxData, uint8_t* ipAddr)
{
    /*
     * Bind TCP socket for receiving
     * On device, port is self known and listening is done first.
     * On controller, port is known once received from device.
     */
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;
    struct sockaddr_in recvSin;
    int errorBind, errorListen = 0;

    /* Check parameters */
    if (RxData == NULL)
    {
        ERR("Null parameter");
        error = ARDISCOVERY_ERROR;
    }

    /* Create TCP socket */
    if (error == ARDISCOVERY_OK)
    {
        /* Close socket if already existing */
        if (RxData->socket)
        {
            ARSAL_Socket_Close(RxData->socket);
        }

        RxData->socket = ARSAL_Socket_Create(AF_INET, SOCK_STREAM, 0);
        if (RxData->socket < 0)
        {
            error = ARDISCOVERY_ERROR_SOCKET_CREATION;
        }
    }

    /* Init socket */
    if (error == ARDISCOVERY_OK)
    {
        /* Server side (device) : listen to anyone */
        /* Client side (controller) : listen to the device we chose */
        if (ipAddr == NULL)
        {
            recvSin.sin_addr.s_addr = htonl(INADDR_ANY);
        }
        else
        {
            recvSin.sin_addr.s_addr = inet_addr((const char *)ipAddr);
        }
        recvSin.sin_family = AF_INET;
        recvSin.sin_port = htons(RxData->port);

        errorBind = ARSAL_Socket_Bind(RxData->socket, (struct sockaddr*) &recvSin, sizeof(recvSin));

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

        errorListen = ARSAL_Socket_Listen(RxData->socket, 10);

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

static eARDISCOVERY_ERROR ARDISCOVERY_Connection_InitTx(ARDISCOVERY_Connection_ComData_t* TxData, uint8_t* ipAddr)
{
    /*
     * Connect TCP socket for sending
     * On controller, port is self known and connecting is done first.
     * On device, port is known once received from controller.
     */
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;
    struct sockaddr_in sendSin;
    int connectError;

    /* Check parameters */
    if (TxData == NULL || ipAddr == NULL)
    {
        ERR("Null parameter");
        error = ARDISCOVERY_ERROR;
    }

    /* Close socket if already existing */
    if (TxData->socket)
    {
        ARSAL_Socket_Close(TxData->socket);
    }

    /* Create TCP socket */
    if (error == ARDISCOVERY_OK)
    {
        TxData->socket = ARSAL_Socket_Create(AF_INET, SOCK_STREAM, 0);
        if (TxData->socket < 0)
        {
            error = ARDISCOVERY_ERROR_SOCKET_CREATION;
        }
    }

    /* Init socket */
    if (error == ARDISCOVERY_OK)
    {
        /* Server side (device) : connect to the controller we chose to respond */
        /* Client side (controller) : connect to the device we chose into list */
        sendSin.sin_addr.s_addr = inet_addr((const char *)ipAddr);
        sendSin.sin_family = AF_INET;
        sendSin.sin_port = htons(TxData->port);

        connectError = ARSAL_Socket_Connect(TxData->socket, (struct sockaddr*) &sendSin, sizeof(sendSin));

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

void ARDISCOVERY_Connection_ReceptionHandler(void* data)
{
    /*
     * Frame reception thread
     */
    ARDISCOVERY_Connection_ConnectionData_t* connectionData = (ARDISCOVERY_Connection_ConnectionData_t*) data;
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    int clientSocket = 0;
    struct sockaddr_in clientAddr;
    socklen_t clientLen = sizeof(clientAddr);

    if (connectionData->callback == NULL)
    {
        ERR("No callback defined for ARDiscovery use");
        return;
    }

    if (connectionData->RxData.socket < 0)
    {
        ERR("No socket defined for reception");
        return;
    }

    while (connectionData->state != ARDISCOVERY_STATE_STOP)
    {
        /* Wait for any incoming connection from controller */
        clientSocket = ARSAL_Socket_Accept(connectionData->RxData.socket, (struct sockaddr*) &clientAddr, &clientLen);

        if (clientSocket < 0)
        {
            ERR("accept() failed: %s", strerror(errno));
            error = ARDISCOVERY_ERROR_ACCEPT;
            return;
        }

        if (error == ARDISCOVERY_OK)
        {
            /* Read content from incoming connection */
            connectionData->RxData.size = read(clientSocket, connectionData->RxData.buffer, ARDISCOVERY_CONNECTION_RX_BUFFER_SIZE);

            if (connectionData->RxData.size > 0)
            {
                SAY("Received \"%s\" (%d) from %s", connectionData->RxData.buffer, connectionData->RxData.size, inet_ntoa(clientAddr.sin_addr));

                /* Manage received data */
                error = ARDISCOVERY_Connection_StateMachine(connectionData, (uint8_t *)inet_ntoa(clientAddr.sin_addr));
            }
        }
    }
}

static eARDISCOVERY_ERROR ARDISCOVERY_Connection_StateMachine(ARDISCOVERY_Connection_ConnectionData_t* connectionData, uint8_t* ipAddr)
{
    /*
     * Manage action regarding current connection state
     */
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    if (connectionData->callback == NULL)
    {
        ERR("Null parameter");
        error = ARDISCOVERY_ERROR;
    }

    if (error == ARDISCOVERY_OK)
    {
        switch (connectionData->state)
        {
            case ARDISCOVERY_STATE_RX_PENDING:
            {
                /* Controller : Wainting for Device response
                 * Device : Listening to anyone */

                /* Manage received data, may get some Tx data */
                error = connectionData->callback(connectionData->customData,
                        connectionData->RxData.buffer, &connectionData->RxData.size,
                        connectionData->TxData.buffer, &connectionData->TxData.size,
                        ipAddr);

                if (error == ARDISCOVERY_OK)
                {
                    /* We may have something to send (case of device responding) */
                    if (connectionData->TxData.size != 0)
                    {
                        error = ARDISCOVERY_Connection_Sending(&connectionData->TxData, ipAddr);
                    }
                    /* Or not (case of controller connecting) */
                }
                break;
            }
            case ARDISCOVERY_STATE_TX_PENDING:
            {
                /* Controller only */

                /* Generate Tx data to connect to a device */
                error = connectionData->callback(connectionData->customData,
                        NULL, NULL,
                        connectionData->TxData.buffer, &connectionData->TxData.size,
                        NULL);

                if (error == ARDISCOVERY_OK)
                {
                    if (connectionData->TxData.size != 0)
                    {
                        error = ARDISCOVERY_Connection_Sending(&connectionData->TxData, ipAddr);
                    }
                    /* Now wait for device response */
                    if (error == ARDISCOVERY_OK)
                    {
                        connectionData->state = ARDISCOVERY_STATE_RX_PENDING;
                    }
                }
                break;
            }
            default:
            {
                /* This should not happen */
                ERR("Received data in state %d", connectionData->state);
                break;
            }
        }
    }

    if (error != ARDISCOVERY_OK)
    {
        ERR("error: %s", ARDISCOVERY_Error_ToString (error));
    }

    return error;
}

static eARDISCOVERY_ERROR ARDISCOVERY_Connection_Sending(ARDISCOVERY_Connection_ComData_t* TxData, uint8_t* ipAddr)
{
    /*
     * Send data
     */
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    /* Check parameters */
    if (TxData == NULL || ipAddr == NULL)
    {
        ERR("Null parameter");
        error = ARDISCOVERY_ERROR;
    }

    /* Open Tx connection */
    if (error == ARDISCOVERY_OK)
    {
        error = ARDISCOVERY_Connection_InitTx(TxData, ipAddr);
    }

    if (error == ARDISCOVERY_OK)
    {
        /* Send content */
        if (write(TxData->socket, TxData->buffer, TxData->size) < 0)
        {
            ERR("Could not write data");
            error = ARDISCOVERY_ERROR_SEND;
        }
    }
    else
    {
        ERR("Could not initiate Tx connection");
    }

    return error;
}

void ARDISCOVERY_Connection_Close(ARDISCOVERY_Connection_ConnectionData_t* connectionData)
{
    /*
     * Stop reception
     */
    connectionData->state = ARDISCOVERY_STATE_STOP;

    ARSAL_Socket_Close(connectionData->TxData.socket);
    ARSAL_Socket_Close(connectionData->RxData.socket);
}

void ARDISCOVERY_Connection_Delete(ARDISCOVERY_Connection_ConnectionData_t** connectionDataPtrAddr)
{
    /*
     * Free connection data
     */
    ARDISCOVERY_Connection_ConnectionData_t *connectionDataPtr = NULL;

    if (connectionDataPtrAddr)
    {
        connectionDataPtr = *connectionDataPtrAddr;

        if (connectionDataPtr)
        {
            if (connectionDataPtr->TxData.buffer)
            {
                free(connectionDataPtr->TxData.buffer);
                connectionDataPtr->TxData.buffer = NULL;
            }
            if (connectionDataPtr->RxData.buffer)
            {
                free(connectionDataPtr->RxData.buffer);
                connectionDataPtr->RxData.buffer = NULL;
            }

            free (connectionDataPtr);
            connectionDataPtr = NULL;
        }

        *connectionDataPtrAddr = NULL;
    }
}
