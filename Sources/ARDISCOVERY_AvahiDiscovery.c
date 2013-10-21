#include <libARDiscovery/ARDISCOVERY_AvahiDiscovery.h>
#include <libARSAL/ARSAL_Print.h>

#define __TAG__ "ARDiscoveryDiscovery"

int ARDISCOVERY_Discovery_TestFunction (int p1, int p2)
{
    ARSAL_PRINT (ARSAL_PRINT_ERROR, __TAG__, "Test function called with args %d and %d", p1, p2);
    return p1 + p2;
}
