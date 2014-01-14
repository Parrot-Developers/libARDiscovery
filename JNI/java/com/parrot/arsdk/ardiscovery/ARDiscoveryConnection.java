/*
 * ARDiscoveryConnection
 *
 *  Created on:
 *  Author:
 */

package com.parrot.arsdk.ardiscovery;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;

import com.parrot.arsdk.arsal.ARSALPrint;

public abstract class ARDiscoveryConnection
{
    /**
     * 
     */
    
    private static String TAG = "ARDiscoveryConnection";
    
    public static String ARDISCOVERY_CONNECTION_JSON_C2DPORT_STRING = "";
    public static String ARDISCOVERY_CONNECTION_JSON_D2CPORT_STRING = "";
    
    public static int ARDISCOVERY_CONNECTION_SEND_JSON_SIZE = 0;
    
    private static native void nativeStaticInit ();
    private static native String nativeGetDefineJsonC2DPortKey ();
    private static native String nativeGetDefineJsonD2CPortKey ();
    private static native int nativeGetDefineTxBufferSize ();
    
    private native long nativeNew();
    private native int nativeDelete(long jARDiscoveryConnection);
    
    private native int nativeControllerConnection (long jConnectionData, int port, String javaIP);
    private native void nativeControllerConnectionAbort (long jConnectionData);
    
    private long nativeARDiscoveryConnection;
    private boolean initOk;
    
    static
    {
        nativeStaticInit();
        ARDISCOVERY_CONNECTION_JSON_C2DPORT_STRING = nativeGetDefineJsonC2DPortKey();
        ARDISCOVERY_CONNECTION_JSON_D2CPORT_STRING = nativeGetDefineJsonD2CPortKey();
        ARDISCOVERY_CONNECTION_SEND_JSON_SIZE = nativeGetDefineTxBufferSize() -1; /* -1 for the null character of the native json */
    }
    
    /**
     * Constructor
     */
    public ARDiscoveryConnection()
    {
        initOk = false;
        nativeARDiscoveryConnection = nativeNew();
        
        if (nativeARDiscoveryConnection != 0)
        {
            initOk = true;
        }
    }
    
    /**
     * Dispose
     */
    public ARDISCOVERY_ERROR_ENUM dispose()
    {
        ARDISCOVERY_ERROR_ENUM error = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK;
        
        if(initOk == true)
        {
            int nativeError = nativeDelete(nativeARDiscoveryConnection);
            error = ARDISCOVERY_ERROR_ENUM.getFromValue(nativeError);
            
            if (error == ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK)
            {
                nativeARDiscoveryConnection = 0;
                initOk = false;
            }
        }
        
        return error;
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
    
    /**
     * @brief Initialize connection
     * @post close() must be called to close the connection.
     * @param[in] port port use to receive the connection data
     * @param[in] ip device IP address
     * @return error during execution
     * @see close()
     */
    public ARDISCOVERY_ERROR_ENUM ControllerConnection (int port, String ip)
    {
        int nativeError = nativeControllerConnection (nativeARDiscoveryConnection, port, ip);
        
        return ARDISCOVERY_ERROR_ENUM.getFromValue(nativeError);
    }
    
    /**
     * @brief Close connection
     * @see openAsController()
     */
    public void ControllerConnectionAbort ()
    {
        nativeControllerConnectionAbort (nativeARDiscoveryConnection);
    }
    
    /**
     * @brief callback use to send json information of the connection
     * @warning the json must not exceed ARDISCOVERY_CONNECTION_SEND_JSON_SIZE
     * @return json information of the connection 
     */
    abstract public String onSendJson (); 
    
    /**
     * @brief callback use to receive json information of the connection
     * @param[in] dataRx json information of the connection
     * @param[in] ip ip address of the sender
     * @return error during callback execution
     */
    abstract public ARDISCOVERY_ERROR_ENUM onReceiveJson (String dataRx, String ip);
    
    private ARDiscoveryConnectionCallbackReturn sendJsonCallback ()
    {
        ARDiscoveryConnectionCallbackReturn callbackReturn = new ARDiscoveryConnectionCallbackReturn();
        String dataTx = null;
        ARDISCOVERY_ERROR_ENUM callbackError = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK;
        
        /* asking the Port use for the device to controller */
        dataTx = onSendJson ();
        callbackReturn.setDataTx (dataTx);
    
        return callbackReturn;
    }
    
    private int receiveJsonCallback (byte[] dataRx, String ip)
    {
        ARDISCOVERY_ERROR_ENUM callbackError = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK;
        String dataRxString = null;
        
        if (dataRx != null)
        {
            /* connected */
            
            try
            {
                /* convert data to string */
                dataRxString = new String (dataRx, "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                callbackError = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_ERROR;
                e.printStackTrace();
            }
            
            callbackError = onReceiveJson (dataRxString, ip);
        }
        
        return callbackError.getValue();
    }
    
    private class ARDiscoveryConnectionCallbackReturn
    {
        private int error;
        private String dataTx;
        
        public ARDiscoveryConnectionCallbackReturn ()
        {
            this.error = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK.getValue();
            this.dataTx = null;
        }
        
        public ARDiscoveryConnectionCallbackReturn (String dataTx, int error)
        {
            this.error = error;
            this.dataTx = dataTx;
        }
        
        public void setError (int error)
        {
            this.error = error;
        }
        
        public void setDataTx (String dataTx)
        {
            this.dataTx = dataTx;
        }
        
        public int getError ()
        {
            return error;
        }
        
        public String getDataTx ()
        {
            return dataTx;
        }
    }
};

