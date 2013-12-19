#ifndef _ARDISCOVERY_CONNECTION_PRIVATE_H_
#define _ARDISCOVERY_CONNECTION_PRIVATE_H_

#include <libARDiscovery/ARDISCOVERY_Error.h>
#include <libARDiscovery/ARDISCOVERY_Connection.h>

/**
 * @brief TCP ports
 */
#define ARDISCOVERY_CONNECTION_TCP_C2D_PORT 44444
#define ARDISCOVERY_CONNECTION_TCP_D2C_PORT 44445

/**
 * @brief Read/Write buffers max size
 */
#define ARDISCOVERY_CONNECTION_TX_BUFFER_SIZE 128
#define ARDISCOVERY_CONNECTION_RX_BUFFER_SIZE 128

/**
 * @brief Connection state
 */
typedef enum
{
    ARDISCOVERY_STATE_NONE = 0,
    ARDISCOVERY_STATE_RX_PENDING,
    ARDISCOVERY_STATE_TX_PENDING,
    ARDISCOVERY_STATE_STOP

} eARDISCOVERY_STATE;

/**
 * @brief Low level communication related structure
 */
typedef struct ARDISCOVERY_Connection_ComData_t
{
    int32_t socket;
    uint32_t port;
    uint8_t *buffer;
    uint32_t size;

} ARDISCOVERY_Connection_ComData_t;

/**
 * @brief Global negotiation related structure (declared in public header)
 */
struct ARDISCOVERY_Connection_ConnectionData_t
{
    ARDISCOVERY_Connection_ComData_t TxData;    // Tx negociation node
    ARDISCOVERY_Connection_ComData_t RxData;    // Rx negociation node
    eARDISCOVERY_STATE state;                   // Connection state
    ARDISCOVERY_Connection_Callback_t callback; // Data management callback
    void* customData;                           // Custom data for callback use
};

#endif /* _ARDISCOVERY_CONNECTION_PRIVATE_H_ */
