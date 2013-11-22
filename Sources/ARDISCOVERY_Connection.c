#include <errno.h>

#include <libARSAL/ARSAL_Print.h>
#include <libARSAL/ARSAL_Socket.h>

#include "libARDiscovery/ARDISCOVERY_Connection.h"

#define __ARDISCOVERY_CONNECTION_TAG__ "ARDISCOVERY_Connection"

#define ERR(...)    ARSAL_PRINT(ARSAL_PRINT_ERROR, __ARDISCOVERY_CONNECTION_TAG__, __VA_ARGS__)
#define SAY(...)    ARSAL_PRINT(ARSAL_PRINT_WARNING, __ARDISCOVERY_CONNECTION_TAG__, __VA_ARGS__)

static eARDISCOVERY_ERROR ARDISCOVERY_Connection_InitTx(ARDISCOVERY_Connection_ComData_t* TxData, uint8_t* ipAddr);
static eARDISCOVERY_ERROR ARDISCOVERY_Connection_InitRx(ARDISCOVERY_Connection_ComData_t* RxData, uint8_t* ipAddr);

/*
 * Initialize com data, state and connections
 */
eARDISCOVERY_ERROR ARDISCOVERY_Connection_Open(ARDISCOVERY_Connection_ConnectionData_t* connectionData)
{
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    /* Allocate IP, if not already */
    if (error == ARDISCOVERY_OK)
    {
        if (connectionData->IP == NULL)
        {
            connectionData->IP = (uint8_t*) malloc(sizeof(uint8_t) * 16);
            if (connectionData->IP != NULL)
            {
                connectionData->IP[0] = '\0';
            }
            else
            {
                error = ARDISCOVERY_ERROR_ALLOC;
            }
        }
    }

    if (error == ARDISCOVERY_OK)
    {
        if (connectionData->tempIP == NULL)
        {
            connectionData->tempIP = (uint8_t*) malloc(sizeof(uint8_t) * 16);
            if (connectionData->tempIP != NULL)
            {
                connectionData->tempIP[0] = '\0';
            }
            else
            {
                error = ARDISCOVERY_ERROR_ALLOC;
            }
        }
    }

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

    /* Connect As */
    if (error == ARDISCOVERY_OK)
    {
        /* Device */
        if (connectionData->RxData.port)
        {
            SAY("Initiate connection as DEVICE");
            error = ARDISCOVERY_Connection_InitRx(&connectionData->RxData, NULL);
            if (error == ARDISCOVERY_OK)
            {
                connectionData->state = ARDISCOVERY_STATE_WAITING;
            }
            else
            {
                ERR("Initialization as DEVICE failed");
            }
        }
        /* Controller */
        else
        {
            /* IP is known from device advertisement */
            SAY("Initiate connection as CONTROLLER (DUMMY)");
            error = ARDISCOVERY_Connection_InitTx(&connectionData->TxData, connectionData->tempIP);
            if (error == ARDISCOVERY_OK)
            {
                /* Send connection request (callback knows from state) */
                error = connectionData->dataCallback(connectionData);
                /* Callback sets state to ARDISCOVERY_STATE_WAITING */
                /* Callback starts reception thread */
            }
        }
    }

    return error;

}

/*
 * Bind TCP socket for receiving
 * On device, port is self known and listening is done first.
 * On controller, port is known once received from device.
 */
static eARDISCOVERY_ERROR ARDISCOVERY_Connection_InitRx(ARDISCOVERY_Connection_ComData_t* RxData, uint8_t* ipAddr)
{
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;
    struct sockaddr_in recvSin;
    int errorBind, errorListen = 0;

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
            recvSin.sin_addr.s_addr = inet_addr(ipAddr);
        }
        recvSin.sin_family = AF_INET;
        recvSin.sin_port = htons(RxData->port);

        errorBind = ARSAL_Socket_Bind(RxData->socket, (struct sockaddr*) &recvSin, sizeof(recvSin));

        if (errorBind != 0)
        {
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

/*
 * Connect TCP socket for sending
 * On controller, port is self known and connecting is done first.
 * On device, port is known once received from controller.
 */
static eARDISCOVERY_ERROR ARDISCOVERY_Connection_InitTx(ARDISCOVERY_Connection_ComData_t* TxData, uint8_t* ipAddr)
{
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;
    struct sockaddr_in sendSin;
    int connectError;

    /* Close socket if already existing */
    if (TxData->socket)
    {
        ARSAL_Socket_Close(TxData->socket);
    }

    /* Init port */
    TxData->port = ARDISCOVERY_CONNECTION_TCP_D2C_PORT;

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
        sendSin.sin_addr.s_addr = inet_addr(ipAddr);
        sendSin.sin_family = AF_INET;
        sendSin.sin_port = htons(TxData->port);

        connectError = ARSAL_Socket_Connect(TxData->socket, (struct sockaddr*) &sendSin, sizeof(sendSin));

        if (connectError != 0)
        {
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

/*
 * Frame reception thread
 */
void ARDISCOVERY_Connection_ReceptionHandler(void* data)
{
    ARDISCOVERY_Connection_ConnectionData_t* connectionData = (ARDISCOVERY_Connection_ConnectionData_t*) data;
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    int clientSocket = 0;
    struct sockaddr_in clientAddr;
    int clientLen = sizeof(clientAddr);

    if (connectionData->dataCallback == NULL)
    {
        ERR("No callback defined for ARDiscovery use");
        error = ARDISCOVERY_ERROR;
    }

    while (connectionData->state != ARDISCOVERY_STATE_STOP)
    {
        /* Wait for any incoming connection from controller */
        clientSocket = ARSAL_Socket_Accept(connectionData->RxData.socket, (struct sockaddr*)&clientAddr, &clientLen);

        if (clientSocket < 0)
        {
            switch (errno)
            {
                case EINTR:
                    ERR("accept() operation was interrupted");
                    break;

                case EINVAL:
                    ERR("listen() has not been called on the socket descriptor");
                    break;

                default:
                    ERR("accept() failed");
                    break;
            }

            error = ARDISCOVERY_ERROR_ACCEPT;
        }

        if (error == ARDISCOVERY_OK)
        {
            /* Read content from incoming connection */
            connectionData->RxData.size = read(clientSocket, connectionData->RxData.buffer, ARDISCOVERY_CONNECTION_RX_BUFFER_SIZE);

            if (connectionData->RxData.size > 0)
            {
                SAY("Received \"%s\" (%d) from %s", connectionData->RxData.buffer, connectionData->RxData.size, inet_ntoa(clientAddr.sin_addr));

                /* This sender address is the one we negociate with */
                strcpy(connectionData->tempIP, inet_ntoa(clientAddr.sin_addr));

                /* Upper layer manages received data */
                error = connectionData->dataCallback(connectionData);
            }
        }
    }
}

/*
 * Frame sending
 */
eARDISCOVERY_ERROR ARDISCOVERY_Connection_Sending(ARDISCOVERY_Connection_ConnectionData_t* connectionData)
{
    eARDISCOVERY_ERROR error = ARDISCOVERY_OK;

    /* Open Tx connection */
    error = ARDISCOVERY_Connection_InitTx(&connectionData->TxData, connectionData->tempIP);

    if (error == ARDISCOVERY_OK)
    {
        /* Send content */
        write(connectionData->TxData.socket, connectionData->TxData.buffer, strlen(connectionData->TxData.buffer));
    }
    else
    {
        ERR("Could not initiate Tx connection");
    }

    return error;
}

/*
 * Free data and stop reception
 */
void ARDISCOVERY_Connection_Close(ARDISCOVERY_Connection_ConnectionData_t* connectionData)
{
    connectionData->state = ARDISCOVERY_STATE_STOP;

    ARSAL_Socket_Close(connectionData->TxData.socket);
    if (connectionData->TxData.buffer)
    {
        free(connectionData->TxData.buffer);
        connectionData->TxData.buffer = NULL;
    }

    ARSAL_Socket_Close(connectionData->RxData.socket);
    if (connectionData->RxData.buffer)
    {
        free(connectionData->RxData.buffer);
        connectionData->RxData.buffer = NULL;
    }

    if (connectionData->IP)
    {
        free(connectionData->IP);
        connectionData->IP = NULL;
    }
}
