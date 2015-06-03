package com.parrot.arsdk.ardiscovery;

import com.parrot.arsdk.arsal.ARSALPrint;

public class ARDiscoveryDevice
{
    private static String TAG = "ARDiscoveryDevice";
    
    private native long nativeNew() throws ARDiscoveryException;
    private native void nativeDelete(long jARDiscoveryDevice);
    
    private native int nativeInitWifi(long jARDiscoveryDevice, int product, String name, String address, int port);
    
    private long nativeARDiscoveryDevice;
    private boolean initOk;

    /**
     * Constructor
     */
    public ARDiscoveryDevice() throws ARDiscoveryException
    {
        ARSALPrint.d(TAG,"ARDiscoveryDevice ...");
        
        initOk = false;
        nativeARDiscoveryDevice = nativeNew();
        if (nativeARDiscoveryDevice != 0)
        {
            initOk = true;
        }
    }

    /**
     * Dispose
     */
    public void dispose()
    {
        ARSALPrint.d(TAG,"dispose ...");
        
        ARDISCOVERY_ERROR_ENUM error = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK;
        synchronized (this)
        {
            if(initOk == true)
            {
                nativeDelete(nativeARDiscoveryDevice);
                nativeARDiscoveryDevice = 0;
                initOk = false;
            }
        }
    }

    /**
     * Destructor
     */
    public void finalize () throws Throwable
    {
        try
        {
            dispose ();
        }
        finally
        {
            super.finalize ();
        }
    }
    
    
    public ARDISCOVERY_ERROR_ENUM initWifi (ARDISCOVERY_PRODUCT_ENUM product, String name, String address, int port)
    {
        ARSALPrint.d(TAG,"initWifi ...");
        
        ARDISCOVERY_ERROR_ENUM error = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK;
        synchronized (this)
        {
            if (initOk == true)
            {
                if (product != null)
                {
                    int nativeError = nativeInitWifi(nativeARDiscoveryDevice, product.getValue() , name, address, port);
                    error = ARDISCOVERY_ERROR_ENUM.getFromValue(nativeError);
                }
                else
                {
                    error = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_ERROR_BAD_PARAMETER;
                }
            }
        }

        return error;
    }
    
    public long getNativeDevice ()
    {
        return nativeARDiscoveryDevice;
    }
}
