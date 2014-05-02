#ifndef _ARDISCOVERY_CONNECTION_H_
#define _ARDISCOVERY_CONNECTION_H_

#include <libARDiscovery/ARDISCOVERY_Error.h>

/**
 * @brief JSON strings for UDP ports extraction
 */
#define ARDISCOVERY_CONNECTION_JSON_C2DPORT_KEY                             "c2d_port"
#define ARDISCOVERY_CONNECTION_JSON_D2CPORT_KEY                             "d2c_port"
#define ARDISCOVERY_CONNECTION_JSON_ARSTREAM_FRAGMENT_SIZE_KEY              "arstream_fragment_size"
#define ARDISCOVERY_CONNECTION_JSON_ARSTREAM_FRAGMENT_MAXIMUM_NUMBER_KEY    "arstream_fragment_maximum_number"
#define ARDISCOVERY_CONNECTION_JSON_CONTROLLER_TYPE_KEY                     "controller_type"
#define ARDISCOVERY_CONNECTION_JSON_CONTROLLER_NAME_KEY                     "controller_name"
#define ARDISCOVERY_CONNECTION_JSON_C2D_UPDATE_PORT_KEY                     "c2d_update_port"
#define ARDISCOVERY_CONNECTION_JSON_C2D_USER_PORT_KEY                       "c2d_user_port"

/**
 * @brief Read/Write buffers max size
 */
#define ARDISCOVERY_CONNECTION_TX_BUFFER_SIZE 256
#define ARDISCOVERY_CONNECTION_RX_BUFFER_SIZE 256

/**
 * @brief callback use to send json information of the connection
 * @warning dataTx must not exceed ARDISCOVERY_CONNECTION_TX_BUFFER_SIZE and must ending by a NULL character
 * @param[out] dataTx Transmission buffer ; must be filled with the json information of the connection
 * @param[out] dataTxSize Transmission data size
 * @param[in] customData custom data
 * @return error during callback execution
 */
typedef eARDISCOVERY_ERROR (*ARDISCOVERY_Connection_SendJsonCallback_t) (uint8_t *dataTx, uint32_t *dataTxSize, void *customData);

/**
 * @brief callback use to receive json information of the connection
 * @param[in] dataRx Reception buffer; containing json information of the connection
 * @param[in] dataRxSize Reception data size
 * @param[in] ip ip address of the sender
 * @param[in] customData custom data
 * @return error during callback execution
 */
typedef eARDISCOVERY_ERROR (*ARDISCOVERY_Connection_ReceiveJsonCallback_t) (uint8_t *dataRx, uint32_t dataRxSize, char *ip, void *customData);

/**
 * @brief Structures to allow data sharing across connection process
 */
typedef struct ARDISCOVERY_Connection_ConnectionData_t ARDISCOVERY_Connection_ConnectionData_t;

/**
 * @brief Create and initialize connection data
 * @param[in] callback Connection data management callback
 * @param[in] customData custom data
 * @param[out] error Error code
 * @return new connection data object
 */
ARDISCOVERY_Connection_ConnectionData_t* ARDISCOVERY_Connection_New(ARDISCOVERY_Connection_SendJsonCallback_t sendJsonCallback, ARDISCOVERY_Connection_ReceiveJsonCallback_t receiveJsonCallback, void *customData, eARDISCOVERY_ERROR *error);

/**
 * @brief Delete connection data
 * @param[in] connectionData Connection data
 * @param[out] error Error code
 */
eARDISCOVERY_ERROR ARDISCOVERY_Connection_Delete (ARDISCOVERY_Connection_ConnectionData_t **connectionData);

/**
 * @brief Initialize connection as a Device
 * @warning Must be called in its own thread
 * @post ARDISCOVERY_Connection_Device_StopListening() must be called to close the connection.
 * @param[in] connectionData Connection data
 * @param[in] port port use to the discovery
 * @return error during execution
 * @see ARDISCOVERY_Connection_Device_StopListening()
 */
eARDISCOVERY_ERROR ARDISCOVERY_Connection_DeviceListeningLoop (ARDISCOVERY_Connection_ConnectionData_t *connectionData, int port);

/**
 * @brief Close connection
 * @warning blocking function ; wait the end of the run
 * @param[in] connectionData Connection data
 * @see ARDISCOVERY_Connection_DeviceListeningLoop()
 */
void ARDISCOVERY_Connection_Device_StopListening (ARDISCOVERY_Connection_ConnectionData_t *connectionData);

/**
 * @brief Initialize connection as a Controller
 * @warning blocking function
 * @post ARDISCOVERY_Connection_Device_StopListening() must be called to close the connection.
 * @param[in] connectionData Connection data
 * @param[in] port port use to the discovery
 * @param[in] ip device IP address
 * @return error during execution
 * @see ARDISCOVERY_Connection_Device_StopListening()
 */
eARDISCOVERY_ERROR ARDISCOVERY_Connection_ControllerConnection (ARDISCOVERY_Connection_ConnectionData_t *connectionData, int port, const char *ip);

/**
 * @brief Abort connection
 * @warning blocking function ; wait the end of the run
 * @param[in] connectionData Connection data
 * @see ARDISCOVERY_Connection_ControllerConnection()
 */
static inline  void ARDISCOVERY_Connection_ControllerConnectionAbort (ARDISCOVERY_Connection_ConnectionData_t *connectionData)
{
    return ARDISCOVERY_Connection_Device_StopListening (connectionData);
}

#endif /* _ARDISCOVERY_CONNECTION_H_ */
