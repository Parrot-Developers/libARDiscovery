#include <libARSAL/ARSAL_Print.h>

#define __TAG__ "ARDiscoveryConnection"

int ARDISCOVERY_Connection_DummyFunction (int param1, int param2)
{
    ARSAL_PRINT (ARSAL_PRINT_ERROR, __TAG__, "Dummy function called with parameters %d and %d", param1, param2);
    return param1 + param2;
}
