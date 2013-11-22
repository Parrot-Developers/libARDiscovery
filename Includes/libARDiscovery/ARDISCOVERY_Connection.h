#ifndef _ARDISCOVERY_CONNECTION_H_
#define _ARDISCOVERY_CONNECTION_H_

#include "libARDiscovery/ARDISCOVERY_Error.h"

#define ARDISCOVERY_CONNECTION_TCP_C2D_PORT 44444
#define ARDISCOVERY_CONNECTION_TCP_D2C_PORT 44445

#define ARDISCOVERY_CONNECTION_TX_BUFFER_SIZE 128
#define ARDISCOVERY_CONNECTION_RX_BUFFER_SIZE 128

#define ARDISCOVERY_CONNECTION_JSON_C2DPORT_STRING "c2d_port"
#define ARDISCOVERY_CONNECTION_JSON_D2CPORT_STRING "d2c_port"

typedef enum
{
    ARDISCOVERY_STATE_NONE = 0,
    ARDISCOVERY_STATE_WAITING,
    ARDISCOVERY_STATE_READY,
    ARDISCOVERY_STATE_STOP

} eARDISCOVERY_STATE;

/**
 * @brief Connection negotiation related data
 */
typedef struct ARDISCOVERY_Connection_ComData_t
{
    uint32_t socket;
    uint32_t port;
    uint8_t *buffer;
    uint32_t size;

} ARDISCOVERY_Connection_ComData_t;

/**
 * @brief Structure to allow service data sharing across connection process
 */
typedef struct ARDISCOVERY_Connection_ConnectionData_t ARDISCOVERY_Connection_ConnectionData_t;

struct ARDISCOVERY_Connection_ConnectionData_t
{
    /* Negotiation fields */
    ARDISCOVERY_Connection_ComData_t TxData;    //
    ARDISCOVERY_Connection_ComData_t RxData;    //
    eARDISCOVERY_STATE state;                   // Connection state
    uint8_t* tempIP;                            // Device IP we're currently negociating with
    eARDISCOVERY_ERROR (*dataCallback)(ARDISCOVERY_Connection_ConnectionData_t* data);

    /* Final data fields */
    uint32_t d2cPort; // inbound                           // UDP connection device -> controller (known from device config)
    uint32_t c2dPort; //outboundPort;                           // UDP connection controller -> device (got from controller)
    uint8_t* IP;                                // IP from the one we're connected to

};

eARDISCOVERY_ERROR ARDISCOVERY_Connection_Open(ARDISCOVERY_Connection_ConnectionData_t* connectionData);

void ARDISCOVERY_Connection_ReceptionHandler(void* connectionData);

eARDISCOVERY_ERROR ARDISCOVERY_Connection_Sending(ARDISCOVERY_Connection_ConnectionData_t* connectionData);

void ARDISCOVERY_Connection_Close(ARDISCOVERY_Connection_ConnectionData_t* connectionData);

#endif /* _ARDISCOVERY_CONNECTION_H_ */
