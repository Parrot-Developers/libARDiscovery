#ifndef _ARDISCOVERY_CONNECTION_H_
#define _ARDISCOVERY_CONNECTION_H_

#include <libARDiscovery/ARDISCOVERY_Error.h>

/**
 * @brief Init type
 */
#define ARDISCOVERY_CONNECTION_INIT_AS_DEVICE       1
#define ARDISCOVERY_CONNECTION_INIT_AS_CONTROLLER   2

/**
 * @brief JSON strings for UDP ports extraction
 */
#define ARDISCOVERY_CONNECTION_JSON_C2DPORT_STRING  "c2d_port"
#define ARDISCOVERY_CONNECTION_JSON_D2CPORT_STRING  "d2c_port"

/**
 * @brief callback use when
 * @param[in] dataRxPtr Reception buffer
 * @param[in] dataRxSize Reception data size
 * @param[in] dataTxPtr Transmission buffer
 * @param[in] dataTxSize Transmission data size
 * @param[in] customData custom data
 * @return error during callback execution
 */
typedef eARDISCOVERY_ERROR (*ARDISCOVERY_Connection_Callback_t) (void *customData, uint8_t* dataRxPtr, uint32_t* dataRxSize, uint8_t* dataTxPtr, uint32_t* dataTxSize, uint8_t* ipAddr);

/**
 * @brief Structures to allow data sharing across connection process
 */
typedef struct ARDISCOVERY_Connection_ConnectionData_t ARDISCOVERY_Connection_ConnectionData_t;

/*
 * @brief Create and initialize connection data
 * @param[in] callback Connection data management callback
 * @param[in] customData custom data
 * @param[in] errorPtr Error code
 * @return new connection data object
 */
ARDISCOVERY_Connection_ConnectionData_t* ARDISCOVERY_Connection_New(ARDISCOVERY_Connection_Callback_t callback, void* customData, eARDISCOVERY_ERROR* errorPtr);

/*
 * @brief Initialize connection
 * @param[in] connectionData Connection data
 * @param[in] initAs Device/Controller init flag
 * @param[in] ipAddr device IP address
 * @return error during execution
 */
eARDISCOVERY_ERROR ARDISCOVERY_Connection_Open(ARDISCOVERY_Connection_ConnectionData_t* connectionData, uint32_t initAs, uint8_t* ipAddr);

/*
 * @brief Receive frames on TCP socket and handle them
 * @param[in] connectionData Connection data
 */
void ARDISCOVERY_Connection_ReceptionHandler(void* connectionData);

/*
 * @brief Close connection
 * @param[in] connectionData Connection data
 */
void ARDISCOVERY_Connection_Close(ARDISCOVERY_Connection_ConnectionData_t* connectionData);

/*
 * @brief Free data structures
 * @param[in] connectionData Connection data
 */
void ARDISCOVERY_Connection_Delete(ARDISCOVERY_Connection_ConnectionData_t** connectionDataPtrAddr);

#endif /* _ARDISCOVERY_CONNECTION_H_ */
