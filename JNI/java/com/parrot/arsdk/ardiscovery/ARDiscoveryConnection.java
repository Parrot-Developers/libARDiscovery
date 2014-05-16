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

    public static String ARDISCOVERY_CONNECTION_JSON_C2DPORT_KEY = "";
    public static String ARDISCOVERY_CONNECTION_JSON_D2CPORT_KEY = "";
    public static String ARDISCOVERY_CONNECTION_JSON_ARSTREAM_FRAGMENT_SIZE_KEY = "";
    public static String ARDISCOVERY_CONNECTION_JSON_ARSTREAM_FRAGMENT_MAXIMUM_NUMBER_KEY = "";
    public static String ARDISCOVERY_CONNECTION_JSON_CONTROLLER_TYPE_KEY = "";
    public static String ARDISCOVERY_CONNECTION_JSON_CONTROLLER_NAME_KEY = "";
    public static String ARDISCOVERY_CONNECTION_JSON_C2D_UPDATE_PORT_KEY = "";
    public static String ARDISCOVERY_CONNECTION_JSON_C2D_USER_PORT_KEY = "";

    public static int ARDISCOVERY_CONNECTION_SEND_JSON_SIZE = 0;

    private static native void nativeStaticInit ();
    private static native String nativeGetDefineJsonC2DPortKey();
    private static native String nativeGetDefineJsonD2CPortKey();
    private static native String nativeGetDefineJsonARStreamFragmentSizeKey();
    private static native String nativeGetDefineJsonARStreamFragmentMaximumNumberKey();
    private static native String nativeGetDefineJsonControllerTypeKey();
    private static native String nativeGetDefineJsonControllerNameKey();
    private static native String nativeGetDefineJsonC2DUpdatePortKey ();
    private static native String nativeGetDefineJsonC2DUserPortKey ();

    private static native int nativeGetDefineTxBufferSize ();

    private native long nativeNew();
    private native int nativeDelete(long jARDiscoveryConnection);

    // Controller Connection
    private native int nativeControllerConnection (long jConnectionData, int port, String javaIP);
    private native void nativeControllerConnectionAbort (long jConnectionData);
    // Device Connection
    private native int nativeDeviceListeningLoop (long jConnectionData, int port);
    private native int nativeDeviceStopListening (long jConnectionData);

    private long nativeARDiscoveryConnection;
    private boolean initOk;

    static
    {
        nativeStaticInit();
        ARDISCOVERY_CONNECTION_JSON_C2DPORT_KEY = nativeGetDefineJsonC2DPortKey();
        ARDISCOVERY_CONNECTION_JSON_D2CPORT_KEY = nativeGetDefineJsonD2CPortKey();
        ARDISCOVERY_CONNECTION_JSON_ARSTREAM_FRAGMENT_SIZE_KEY = nativeGetDefineJsonARStreamFragmentSizeKey();
        ARDISCOVERY_CONNECTION_JSON_ARSTREAM_FRAGMENT_MAXIMUM_NUMBER_KEY = nativeGetDefineJsonARStreamFragmentMaximumNumberKey();
        ARDISCOVERY_CONNECTION_JSON_CONTROLLER_TYPE_KEY = nativeGetDefineJsonControllerTypeKey();
        ARDISCOVERY_CONNECTION_JSON_CONTROLLER_NAME_KEY = nativeGetDefineJsonControllerNameKey();
        ARDISCOVERY_CONNECTION_JSON_C2D_UPDATE_PORT_KEY = nativeGetDefineJsonC2DUpdatePortKey();
        ARDISCOVERY_CONNECTION_JSON_C2D_USER_PORT_KEY = nativeGetDefineJsonC2DUserPortKey();

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
        if(initOk == true)
        {
            nativeControllerConnectionAbort (nativeARDiscoveryConnection);
        }
    }

    /**
     * @brief Initialize connection as a Device
     * @warning Must be called in its own thread
     * @param[in] port port use to the discovery
     * @return error during execution
     */
    public ARDISCOVERY_ERROR_ENUM DeviceListeningLoop (int port)
    {
        int nativeError = 0;
        
        if(initOk == true)
        {
            nativeDeviceListeningLoop (nativeARDiscoveryConnection, port);
        }
        
        return ARDISCOVERY_ERROR_ENUM.getFromValue(nativeError);
    }

    /**
     * @brief Close connection
     * @see openAsController()
     */
    public void DeviceStopListening ()
    {
        if(initOk == true)
        {
            nativeDeviceStopListening (nativeARDiscoveryConnection);
        }
    }

    /**
     * @brief callback use to send json information of the connection
     * @warning the json must not exceed ARDISCOVERY_CONNECTION_SEND_JSON_SIZE
     * @return json information of the connection
     */
    protected abstract String onSendJson ();

    /**
     * @brief callback use to receive json information of the connection
     * @param[in] dataRx json information of the connection
     * @param[in] ip ip address of the sender
     * @return error during callback execution
     */
    protected abstract ARDISCOVERY_ERROR_ENUM onReceiveJson (String dataRx, String ip);

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
