#ifndef _ARDISCOVERY_ERROR_H_
#define _ARDISCOVERY_ERROR_H_

/**
 * @brief libARDiscovery errors known.
 */
typedef enum
{
    ARDISCOVERY_OK = 0, /**< No error */
    ARDISCOVERY_ERROR = -1, /**< Unknown generic error */

    ARDISCOVERY_ERROR_SIMPLE_POLL = -1000, /**< Avahi failed to create simple poll object */
    ARDISCOVERY_ERROR_BUILD_NAME, /**< Avahi failed to create simple poll object */
    ARDISCOVERY_ERROR_CLIENT, /**< Avahi failed to create client */
    ARDISCOVERY_ERROR_ENTRY_GROUP, /**< Avahi failed to create entry group */
    ARDISCOVERY_ERROR_ADD_SERVICE, /**< Avahi failed to add service */
    ARDISCOVERY_ERROR_GROUP_COMMIT, /**< Avahi failed to commit group */

    ARDISCOVERY_ERROR_ALLOC = -2000, /**< Failed to allocate connection resources */
    ARDISCOVERY_ERROR_INIT, /**< Wrong type to connect as */
    ARDISCOVERY_ERROR_SOCKET_CREATION, /**< Socket creation error */
    ARDISCOVERY_ERROR_SOCKET_PERMISSION_DENIED, /**< Socket access permission denied */
    ARDISCOVERY_ERROR_SOCKET_ALREADY_CONNECTED, /**< Socket is already connected */
    ARDISCOVERY_ERROR_ACCEPT, /**< Socket accept failed */
    ARDISCOVERY_ERROR_SEND, /**< Failed to write frame to socket */

} eARDISCOVERY_ERROR;

/**
 * @brief Gets the error string associated with an eARDISCOVERY_ERROR
 * @param error The error to describe
 * @return A static string describing the error
 *
 * @note User should NEVER try to modify a returned string
 */
char* ARDISCOVERY_Error_ToString (eARDISCOVERY_ERROR error);

#endif /* _ARDISCOVERY_ERROR_H_ */
