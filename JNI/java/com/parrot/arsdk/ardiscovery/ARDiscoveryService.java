/*
 * DroneControlService
 *
 *  Created on: 
 *  Author:
 */

package com.parrot.arsdk.ardiscovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import com.parrot.arsdk.arsal.ARSALPrint;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Formatter;
import android.util.Pair;
import android.os.Build;

public class ARDiscoveryService extends Service
{
    private static final String TAG = ARDiscoveryService.class.getSimpleName();
    
    /* Native Functions */
    public static native int nativeGetProductID (int product);
    private static native String nativeGetProductName(int product);
    private static native String nativeGetProductPathName(int product);
    private static native int nativeGetProductFromName(String name);
    private static native int nativeGetProductFromProductID(int productID);
    
    public enum eARDISCOVERY_SERVICE_EVENT_STATUS
    {
        ARDISCOVERY_SERVICE_EVENT_STATUS_ADD, 
        ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED,
        ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED 
    }
    
    public enum ARDISCOVERYSERVICE_WIFI_DISCOVERY_TYPE_ENUM
    {
        ARDISCOVERYSERVICE_WIFI_DISCOVERY_TYPE_JMDNS,
        ARDISCOVERYSERVICE_WIFI_DISCOVERY_TYPE_NSD;
    }
    
    /**
     * Constant for devices services list updates notification
     */
    public static final String kARDiscoveryServiceNotificationServicesDevicesListUpdated = "kARDiscoveryServiceNotificationServicesDevicesListUpdated";
    
    public static final String kARDiscoveryServiceWifiDiscoveryType = "kARDiscoveryServiceWifiDiscoveryType";
    
    public static String ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT;
    public static String ARDISCOVERY_SERVICE_NET_DEVICE_DOMAIN;
    
    private static native String nativeGetDefineNetDeviceDomain ();
    private static native String nativeGetDefineNetDeviceFormat ();
    
    private HashMap<String, Intent> intentCache;
    
    private ARDiscoveryBLEDiscovery bleDiscovery;
    private ARDiscoveryWifiDiscovery wifiDiscovery;
    private ARDiscoveryNsdPublisher wifiPublisher;
    
    private ARDISCOVERYSERVICE_WIFI_DISCOVERY_TYPE_ENUM wifiDiscoveryType = null;
    
    private final IBinder binder = new LocalBinder();
    private Handler mHandler;
    
    static
    {
        ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT = nativeGetDefineNetDeviceFormat () + ".";
        ARDISCOVERY_SERVICE_NET_DEVICE_DOMAIN = nativeGetDefineNetDeviceDomain () + ".";
    }
    
    @Override
    public IBinder onBind(Intent intent)
    {
        ARSALPrint.d(TAG,"onBind");

        return binder;
    }  
    
    @Override
    public void onCreate() 
    {
        initIntents();
        
        bleDiscovery = new ARDiscoveryBLEDiscoveryImpl();
        bleDiscovery.open(this, this);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            wifiPublisher = new ARDiscoveryNsdPublisher();
            wifiPublisher.open(this, this);
        }
        else
        {
            ARSALPrint.w(TAG, "no wifiPublisher !");
        }
        
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        
        if(bleDiscovery != null)
        {
            bleDiscovery.close();
        }
        
        if(wifiDiscovery != null)
        {
            wifiDiscovery.close();
        }
        
        if(wifiPublisher != null)
        {
            wifiPublisher.close();
        }
        
    }
    
    public class LocalBinder extends Binder
    {
        public ARDiscoveryService getService()
        {
            ARSALPrint.d(TAG,"getService");
            return ARDiscoveryService.this;
        }
    }
    
    private void initIntents()
    {
        ARSALPrint.d(TAG,"initIntents");
        
        intentCache = new HashMap<String, Intent>();
        intentCache.put(kARDiscoveryServiceNotificationServicesDevicesListUpdated, new Intent(kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }
    
    @Override
    public boolean onUnbind(Intent intent)
    {
        ARSALPrint.d(TAG,"onUnbind");
        
        return true; /* ensures onRebind is called */
    }
    
    @Override
    public void onRebind(Intent intent)
    {
        ARSALPrint.d(TAG,"onRebind");
        
    }
    
    private synchronized void initWifiDiscovery(ARDISCOVERYSERVICE_WIFI_DISCOVERY_TYPE_ENUM newWifiDiscoveryType) 
    {
        switch (newWifiDiscoveryType)
        {
            case ARDISCOVERYSERVICE_WIFI_DISCOVERY_TYPE_NSD:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                {
                    wifiDiscoveryType = newWifiDiscoveryType;
                    wifiDiscovery = new ARDiscoveryNsdDiscovery();
                }
                else
                {
                    ARSALPrint.w(TAG, "NSD can't run on " + Build.VERSION.SDK_INT + " jmdns will be used");
                    initWifiDiscovery(ARDISCOVERYSERVICE_WIFI_DISCOVERY_TYPE_ENUM.ARDISCOVERYSERVICE_WIFI_DISCOVERY_TYPE_JMDNS);
                }
                break;
                
            case ARDISCOVERYSERVICE_WIFI_DISCOVERY_TYPE_JMDNS:
            default:
                wifiDiscoveryType = newWifiDiscoveryType;
                wifiDiscovery = new ARDiscoveryJmdnsDiscovery();
                break;
            
        }
        
        if (wifiDiscovery != null)
        {
            ARSALPrint.w(TAG, "Creation complete, open in progress");
            wifiDiscovery.open(this, this);
            ARSALPrint.w(TAG, "Open complete");
        }
    }
    
    public synchronized void start()
    {
        start(ARDISCOVERYSERVICE_WIFI_DISCOVERY_TYPE_ENUM.ARDISCOVERYSERVICE_WIFI_DISCOVERY_TYPE_JMDNS);
    }
    
    public synchronized void start(ARDISCOVERYSERVICE_WIFI_DISCOVERY_TYPE_ENUM newWifiDiscoveryType)
    {
        //manage ble
        if (bleDiscovery != null)
        {
            bleDiscovery.start();
        }

        //manage wifi
        if ((wifiDiscovery != null) && (wifiDiscoveryType != newWifiDiscoveryType))
        {
            //the wifiDiscoveryType is not the same of the previous
            wifiDiscovery.close();
            wifiDiscovery = null;
        }
        //no else ; no wifiDiscovery or wifiDiscoveryType is the same of the previous

        if (wifiDiscovery == null)
        {
            //create a new wifiDiscovery
            initWifiDiscovery(newWifiDiscoveryType);
        }
        //no else ; wifiDiscovery already exits

        if (wifiDiscovery != null)
        {
            wifiDiscovery.start();
        }
    }

    public synchronized void stop()
    {
        if (bleDiscovery != null)
        {
            bleDiscovery.stop();
        }
        
        if (wifiDiscovery != null)
        {
            wifiDiscovery.stop();
        }
    }
    
    public synchronized void startBLEDiscovering()
    {
        if (bleDiscovery != null)
        {
            bleDiscovery.start();
        }
    }
    
    public synchronized void stopBLEDiscovering()
    {
        if (bleDiscovery != null)
        {
            bleDiscovery.stop();
        }
    }
    
    /* broadcast the deviceServicelist Updated */
    public void broadcastDeviceServiceArrayUpdated ()
    {
        /* broadcast the service add */
        Intent intent = intentCache.get(kARDiscoveryServiceNotificationServicesDevicesListUpdated);
        
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
    
    public List<ARDiscoveryDeviceService> getDeviceServicesArray()
    {
        List<ARDiscoveryDeviceService> deviceServicesArray =  wifiDiscovery.getDeviceServicesArray(); 
        deviceServicesArray.addAll(bleDiscovery.getDeviceServicesArray());
        
        ARSALPrint.d(TAG,"getDeviceServicesArray: " + deviceServicesArray);

        return deviceServicesArray;
    }

    /**
     * @brief Publishes a service of the given product
     * This function unpublishes any previously published service
     * @param product The product to publish
     * @param name The name of the service
     * @param port The port of the service
     */
    public boolean publishService(ARDISCOVERY_PRODUCT_ENUM product, String name, int port)
    {
        boolean ret = false;
        
        if (wifiPublisher != null)
        {
            ret = wifiPublisher.publishService(product, name, port);
        }
        
        return ret;
    }

    /**
     * @brief Publishes a service of the given product_id
     * This function unpublishes any previously published service
     * @param product_id The product ID to publish
     * @param name The name of the service
     * @param port The port of the service
     */
    public boolean publishService(final int product_id, final String name, final int port)
    {
        boolean ret = false;
        
        if (wifiPublisher != null)
        {
            ret = wifiPublisher.publishService(product_id, name, port);
        }
        
        return ret;
    }

    /**
     * @brief Unpublishes any published service
     */
    public void unpublishServices()
    {
        if (wifiPublisher != null)
        {
            wifiPublisher.unpublishService();
        }
    }

    /**
     * @brief Converts a product enumerator in product ID
     * This function is the only one knowing the correspondance
     * between the enumerator and the products' IDs.
     * @param product The product's enumerator
     * @return The corresponding product ID
     */
    public static int getProductID (ARDISCOVERY_PRODUCT_ENUM product)
    {
        return nativeGetProductID (product.getValue());
    }

    /**
     * @brief Converts a product ID in product enumerator
     * This function is the only one knowing the correspondance
     * between the enumerator and the products' IDs.
     * @param productID The product's ID
     * @return The corresponding product enumerator
     */
    public static ARDISCOVERY_PRODUCT_ENUM getProductFromProductID (int productID)
    {
        return ARDISCOVERY_PRODUCT_ENUM.getFromValue (nativeGetProductFromProductID (productID));
    }

    /**
     * @brief Converts a product enum to a generic (network type) product enum
     * @param product The product to convert
     * @return The corresponding network type product enum
     */
    public static ARDISCOVERY_PRODUCT_ENUM getProductNetworkFromProduct (ARDISCOVERY_PRODUCT_ENUM product)
    {
        int bleOrdinal = ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_BLESERVICE.getValue();
        int productOrdinal = product.getValue();
        ARDISCOVERY_PRODUCT_ENUM retVal = ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_NSNETSERVICE;

        if (productOrdinal >= bleOrdinal)
        {
            retVal = ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_BLESERVICE;
        }

        return retVal;
    }
    
    /**
     * @brief Converts a product ID in product name
     * This function is the only one knowing the correspondance
     * between the products' IDs and the product names.
     * @param product The product ID
     * @return The corresponding product name
     */
    public static String getProductName (ARDISCOVERY_PRODUCT_ENUM product)
    {
        return nativeGetProductName (product.getValue());
    }
    
    /**
     * @brief Converts a product ID in product path name
     * This function is the only one knowing the correspondance
     * between the products' IDs and the product path names.
     * @param product The product ID
     * @return The corresponding product path name
     */
    public static String getProductPathName (ARDISCOVERY_PRODUCT_ENUM product)
    {
        return nativeGetProductPathName (product.getValue());
    }
    
    /**
     * @brief Converts a product product name in product ID
     * This function is the only one knowing the correspondance
     * between the product names and the products' IDs.
     * @param name The product's product name
     * @return The corresponding product ID
     */
    public static ARDISCOVERY_PRODUCT_ENUM getProductFromName(String name)
    {
        int product = nativeGetProductFromName(name);
        
        return ARDISCOVERY_PRODUCT_ENUM.getFromValue(product);
    }
};

