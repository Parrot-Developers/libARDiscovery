#ifndef _ARDISCOVERY_CONNECTION_PRIVATE_H_
#define _ARDISCOVERY_CONNECTION_PRIVATE_H_

#include <libARDiscovery/ARDISCOVERY_Error.h>
#include <libARDiscovery/ARDISCOVERY_Connection.h>

/**
 * @brief Low level communication related structure
 */
typedef struct ARDISCOVERY_Connection_ComData_t
{
    uint8_t *buffer; /**< data buffer */
    uint32_t size; /**< size of the data buffer */
} ARDISCOVERY_Connection_ComData_t;

/**
 * @brief Global negotiation related structure (declared in public header)
 */
struct ARDISCOVERY_Connection_ConnectionData_t
{
    ARDISCOVERY_Connection_ComData_t txData;        /**< Tx negociation node */
    ARDISCOVERY_Connection_ComData_t rxData;        /**< Rx negociation node */
    uint8_t isAlive;                                   /**< is alive flag */
    ARSAL_Sem_t runningSem;                             /**< running Semaphore */
    ARDISCOVERY_Connection_SendJsonCallback_t sendJsoncallback; /**< callback use to send json information of the connection */
    ARDISCOVERY_Connection_ReceiveJsonCallback_t receiveJsoncallback; /**< callback use to receive json information of the connection */
    void *customData;                           /**< Custom data for callback use */
    int32_t socket;                             /**< socket used to negociate */
    struct sockaddr_in address;                 /**< address used to negociate */
    
    int abortPipe[2];
};

#endif /* _ARDISCOVERY_CONNECTION_PRIVATE_H_ */
