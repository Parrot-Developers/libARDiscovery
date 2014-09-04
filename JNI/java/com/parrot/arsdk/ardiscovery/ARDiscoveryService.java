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

public class ARDiscoveryService extends Service
{
    private static String TAG = "ARDiscoveryService";
    
    /* Native Functions */
    private static native int nativeGetProductID (int product);
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
    
    /**
     * Constant for devices services list updates notification
     */
    public static final String kARDiscoveryServiceNotificationServicesDevicesListUpdated = "kARDiscoveryServiceNotificationServicesDevicesListUpdated";
    
    public static String ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT;
    public static String ARDISCOVERY_SERVICE_NET_DEVICE_DOMAIN;
    
    private static native String nativeGetDefineNetDeviceDomain ();
    private static native String nativeGetDefineNetDeviceFormat ();
    
    private HashMap<String, Intent> intentCache;
    
    private JmDNS mDNSManager;
    private ServiceListener mDNSListener;
    private List<String> devicesServiceArray;
    
    private ServiceInfo publishedService;
    
    private AsyncTask<Object, Object, Object> jmdnsCreatorAsyncTask;
    private Boolean isNetDiscovering = false;
    private Boolean isLeDiscovering = false;
    private Boolean askForNetDiscovering = false;
    private Boolean askForLeDiscovering = false;
    
    private String hostIp;
    private InetAddress hostAddress;
    static private InetAddress nullAddress;
    
    private IntentFilter networkStateChangedFilter;
    private BroadcastReceiver networkStateIntentReceiver;
    
    private HashMap<String, ARDiscoveryDeviceService> netDeviceServicesHmap;
    
    /* BLE */
    private static final int ARDISCOVERY_BT_VENDOR_ID = 0x0043; /* Parrot Company ID registered by Bluetooth SIG (Bluetooth Specification v4.0 Requirement) */
    private static final int ARDISCOVERY_USB_VENDOR_ID = 0x19cf; /* official Parrot USB Vendor ID */

    private Boolean bleIsAvalaible;
    private BluetoothAdapter bluetoothAdapter;
    private BLEScanner bleScanner;
    private HashMap<String, ARDiscoveryDeviceService> bleDeviceServicesHmap;
    private Object leScanCallback;/*< Device scan callback. (BluetoothAdapter.LeScanCallback) */
    
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
        ARSALPrint.d(TAG,"onCreate");
        mHandler = new Handler();

        try 
        {
            nullAddress = InetAddress.getByName("0.0.0.0");
        } 
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        hostAddress = nullAddress;
        
        /**
         * devicesServiceArray init
         */
        devicesServiceArray = new ArrayList<String>();
        for (int i = ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_NSNETSERVICE.getValue() ; i < ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_BLESERVICE.getValue(); ++i)
        {
            String devicesService = String.format (ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT, nativeGetProductID(i));
            
            devicesService += ARDISCOVERY_SERVICE_NET_DEVICE_DOMAIN;
            
            devicesServiceArray.add(devicesService);
        }
        
        netDeviceServicesHmap = new HashMap<String, ARDiscoveryDeviceService> ();
        bleDeviceServicesHmap = new HashMap<String, ARDiscoveryDeviceService> ();
        initIntents();
        
        networkStateChangedFilter = new IntentFilter();
        networkStateChangedFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        networkStateChangedFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        networkStateIntentReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                ARSALPrint.d(TAG,"BroadcastReceiver onReceive");
                
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
                {
                    /* if the wifi is connected get its hostAddress */
                    ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                    NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (wifiInfo.isConnected())
                    {
                        if(isNetDiscovering)
                        {
                            mdnsConnect();
                            isNetDiscovering = false;
                        }
                    }
                    else
                    {
                        if(isNetDiscovering)
                        {
                            askForNetDiscovering = true;
                        }
                        
                        mdnsDisconnect();
                        
                        ArrayList<ARDiscoveryDeviceService> netDeviceServicesArray = new ArrayList<ARDiscoveryDeviceService> (netDeviceServicesHmap.values());
                        
                        /* remove all net services */
                        for ( ARDiscoveryDeviceService s : netDeviceServicesArray)
                        {
                            notificationNetServiceDeviceRemoved (s);
                        }
                    }
                }
                
                if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED))
                {
                    
                    ARSALPrint.d(TAG,"ACTION_STATE_CHANGED");
                    
                    if (bleIsAvalaible)
                    {
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        
                        switch (state)
                        {
                            case BluetoothAdapter.STATE_ON:
                                if (askForLeDiscovering)
                                {
                                    bleConnect();
                                    askForLeDiscovering = false;
                                }
                                break;
                            case BluetoothAdapter.STATE_TURNING_OFF:
                                
                                /* remove all BLE services */
                                bleDeviceServicesHmap.clear();
                                
                                /* broadcast the new deviceServiceList */
                                broadcastDeviceServiceArrayUpdated ();
                                
                                if(isLeDiscovering)
                                {
                                    askForLeDiscovering = true;
                                }
                                bleDisconnect();
                                
                                /* remove all BLE services */
                                bleDeviceServicesHmap.clear();
                                
                                /* broadcast the new deviceServiceList */
                                broadcastDeviceServiceArrayUpdated ();
                                
                                break;
                        }
                    }
                }
            }
        };
        
        bleIsAvalaible = false;
        getBLEAvailability();

        if (bleIsAvalaible)
        {
            initBLE();
        }
        
        registerReceiver(networkStateIntentReceiver, networkStateChangedFilter);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        
        ARSALPrint.d(TAG,"onDestroy");
        mHandler.removeCallbacksAndMessages(null);
        
        unregisterReceiver(networkStateIntentReceiver);
        
        mdnsDisconnect ();
        bleDisconnect();
    }
    
    public void mdnsDestroy()
    {
        ARSALPrint.d(TAG,"mdnsDestroy");

        /* if jmnds is running */
        if (mDNSManager != null)
        {
            if (mDNSListener != null)
            {
                /* remove the net service listeners */
                for (String devicesService : devicesServiceArray)
                {
                    ARSALPrint.d(TAG,"removeServiceListener:" + devicesService);
                    mDNSManager.removeServiceListener(devicesService, mDNSListener);
                }

                mDNSListener = null;
            }
            
            try
            {
                mDNSManager.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            mDNSManager = null;
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
    
    private void getBLEAvailability()
    {
        /* check whether BLE is supported on the device  */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            ARSALPrint.d(TAG,"BLE Is NOT Avalaible");
            bleIsAvalaible = false;
        }
        else
        {
            ARSALPrint.d(TAG,"BLE Is Avalaible");
            bleIsAvalaible = true;
        }
    }
    
    @TargetApi(18)
    private void initBLE()
    {
        ARSALPrint.d(TAG,"initBLE");
        
        /* Initializes Bluetooth adapter. */
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        
        bleScanner = new BLEScanner();
        
        leScanCallback = new BluetoothAdapter.LeScanCallback()
        {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
            {
                ARSALPrint.d(TAG,"onLeScan");
                
                bleScanner.bleCallback(device, rssi, scanRecord);
            }
        };
    }
    
    public void start()
    {
        ARSALPrint.d(TAG,"start");
        
        if (!isNetDiscovering)
        {
            /* if the wifi is connected get its hostAddress */
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWifi.isConnected())
            {
                mdnsConnect();
            }
            else
            {
                askForNetDiscovering = true;
            }
        }
        
        if (!isLeDiscovering)
        {
            if ((bleIsAvalaible == true) && bluetoothAdapter.isEnabled())
            {
                bleConnect();
                isLeDiscovering = true;
            }
            else
            {
                askForLeDiscovering = true;
            }
        }
    }

    public void stop()
    {
        ARSALPrint.d(TAG,"stop");
        
        if (isNetDiscovering)
        {
            /* Stop net scan */
            mdnsDisconnect();
        }
        
        if (isLeDiscovering)
        {
            /* Stop BLE scan */
            bleDisconnect();
            isLeDiscovering = false;
        }
    }

    public void startBLEDiscovering()
    {
        ARSALPrint.d(TAG,"startBLEDiscovering");
        
        if (!isLeDiscovering)
        {
            if ((bleIsAvalaible == true) && bluetoothAdapter.isEnabled())
            {
                bleConnect();
                isLeDiscovering = true;
            }
            else
            {
                askForLeDiscovering = true;
            }
        }
    }
    
    public void stopBLEDiscovering()
    {
        ARSALPrint.d(TAG,"stopBLEDiscovering");
        
        if (isLeDiscovering)
        {
            /* Stop BLE scan */
            bleDisconnect();
            isLeDiscovering = false;
        }
    }
    
    private void mdnsConnect()
    {
        ARSALPrint.d(TAG,"connect");
        
        /* if jmdns is not running yet */
        if (mDNSManager == null)
        {
            /* get the host address */
            WifiManager wifi =   (WifiManager)getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
        
            if(wifi != null)
            {
                try
                {
                    hostIp = Formatter.formatIpAddress(wifi.getConnectionInfo().getIpAddress());
                    hostAddress = InetAddress.getByName(hostIp);
                }
                catch (UnknownHostException e)
                {
                    e.printStackTrace();
                }
                
                ARSALPrint.d(TAG,"hostIp: " + hostIp);
                ARSALPrint.d(TAG,"hostAddress: " + hostAddress);
                
            }

            if ((!hostAddress.equals (nullAddress)) && (isNetDiscovering == false))
            {
                isNetDiscovering = true;
                
                jmdnsCreatorAsyncTask = new JmdnsCreatorAsyncTask();
                jmdnsCreatorAsyncTask.execute();
            }
        }
    }
    
    private void mdnsDisconnect()
    {
        ARSALPrint.d(TAG,"disconnect");
        
        /* reset */
        hostAddress = nullAddress;
        unpublishServices();
        mdnsDestroy ();
    }
    
    private void notificationNetServiceDeviceAdd( ServiceEvent serviceEvent )
    {
        ARSALPrint.d(TAG,"notificationServiceDeviceAdd");
        
        int productID = 0;
        
        /* get ip address */
        String ip = getServiceIP (serviceEvent);
        int port = getServicePort (serviceEvent);
        
        if (ip != null)
        {
            /* new ARDiscoveryDeviceNetService */
            ARDiscoveryDeviceNetService deviceNetService = new ARDiscoveryDeviceNetService(serviceEvent.getName(), serviceEvent.getType(), ip, port);
            
            /* find device type */
            for (int i = ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_NSNETSERVICE.getValue(); i < ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_BLESERVICE.getValue(); ++i)
            {
                if (deviceNetService.getType().equals(devicesServiceArray.get(i)))
                {
                    productID = nativeGetProductID(i);
                    break;
                }
            }
            
            if (productID != 0)
            {
                /* add the service in the array */
                ARDiscoveryDeviceService deviceService = new ARDiscoveryDeviceService (serviceEvent.getName(), deviceNetService, productID);
                netDeviceServicesHmap.put(deviceService.getName(), deviceService);
                
                /* broadcast the new deviceServiceList */
                broadcastDeviceServiceArrayUpdated ();
            }
            else
            {
                ARSALPrint.e(TAG,"Found an unknown service : " + deviceNetService);
            }
        }
    }
    
    private void notificationNetServicesDevicesResolved( ServiceEvent serviceEvent )
    {
        ARSALPrint.d(TAG,"notificationServicesDevicesResolved");
        
        /* check if the service is known */
        Boolean known = netDeviceServicesHmap.containsKey(serviceEvent.getName());
        
        /* add the service if it not known yet*/
        if(!known)
        {
            ARSALPrint.d(TAG,"service Resolved not know : "+ serviceEvent);
            notificationNetServiceDeviceAdd(serviceEvent);
        }
    }
    
    private void notificationNetServiceDeviceRemoved( ServiceEvent serviceEvent )
    {
        ARSALPrint.d(TAG,"notificationServiceDeviceRemoved");
        
        /* remove from the deviceServicesHmap */
        ARDiscoveryDeviceService deviceServiceRemoved = netDeviceServicesHmap.remove(serviceEvent.getName());

        if(deviceServiceRemoved != null)
        {
            /* broadcast the new deviceServiceList */
            broadcastDeviceServiceArrayUpdated ();
        }
        else
        {
            ARSALPrint.w(TAG, "service: "+ serviceEvent.getInfo().getName() + " not known");
        }
    }
    
    private void notificationNetServiceDeviceRemoved( ARDiscoveryDeviceService deviceService )
    {
        ARSALPrint.d(TAG,"notificationServiceDeviceRemoved");
        
        /* remove from the deviceServicesHmap */
        ARDiscoveryDeviceService deviceServiceRemoved = netDeviceServicesHmap.remove(deviceService.getName());

        if(deviceServiceRemoved != null)
        {
            /* broadcast the new deviceServiceList */
            broadcastDeviceServiceArrayUpdated ();
        }
        else
        {
            ARSALPrint.w(TAG, "service: "+ deviceService.getName() + " not known");
        }
    }
    
    
    /* broadcast the deviceServicelist Updated */
    private void broadcastDeviceServiceArrayUpdated ()
    {    
        ARSALPrint.d(TAG,"broadcastDeviceServiceArrayUpdated");
        /* broadcast the service add*/
        Intent intent = intentCache.get(kARDiscoveryServiceNotificationServicesDevicesListUpdated);
        
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
    
    private String getServiceIP(ServiceEvent serviceEvent)
    {
        ARSALPrint.d(TAG,"getServiceIP serviceEvent: " + serviceEvent);
        
        String serviceIP = null;
        
        ServiceInfo info = serviceEvent.getDNS().getServiceInfo(serviceEvent.getType(), serviceEvent.getName());
        if( (info != null) && (info.getInet4Addresses().length > 0) )
        {
            serviceIP = info.getInet4Addresses()[0].getHostAddress();
        }
        
        return serviceIP;
    }
    
    private int getServicePort(ServiceEvent serviceEvent)
    {
        ARSALPrint.d(TAG,"getServicePort serviceEvent: " + serviceEvent);
        
        int servicePort = 0;
        
        ServiceInfo info = serviceEvent.getDNS().getServiceInfo(serviceEvent.getType(), serviceEvent.getName());
        if(info != null)
        {
            servicePort = info.getPort();
        }
        
        return servicePort;
    }
    
    public ArrayList<ARDiscoveryDeviceService> getDeviceServicesArray()
    {
        ArrayList<ARDiscoveryDeviceService> deviceServicesArray = new ArrayList<ARDiscoveryDeviceService> ( netDeviceServicesHmap.values()  ); 
        deviceServicesArray.addAll(bleDeviceServicesHmap.values());
        
        ARSALPrint.d(TAG,"getDeviceServicesArray: " + deviceServicesArray);
        return new ArrayList<ARDiscoveryDeviceService> (deviceServicesArray);
    }
    
    // TODO: only one ...
    private class JmdnsCreatorAsyncTask extends AsyncTask<Object, Object, Object> { 
        
        @Override
        protected Object doInBackground(Object... params) 
        {
            ARSALPrint.d(TAG, "doInBackground");
            
            try 
            {
                mDNSManager = null;
                if ( (hostAddress != null) && (! hostAddress.equals( nullAddress )) )
                {
                    mDNSManager = JmDNS.create(hostAddress);
                }
                else
                {
                    mDNSManager = JmDNS.create();
                }
                                
                ARSALPrint.d(TAG,"JmDNS.createed");
                
                mDNSListener = new ServiceListener() 
                {
                    
                    @Override
                    public void serviceAdded(ServiceEvent event) 
                    {
                        ARSALPrint.d(TAG,"Service Added: " + event.getName());
                        
                        Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS> dataProgress = new Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS>(event,eARDISCOVERY_SERVICE_EVENT_STATUS.ARDISCOVERY_SERVICE_EVENT_STATUS_ADD);
                        publishProgress(dataProgress);
                    }
                
                    @Override
                    public void serviceRemoved(ServiceEvent event) 
                    {
                        ARSALPrint.d(TAG,"Service removed: " + event.getName());
                        
                        Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS> dataProgress = new Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS>(event,eARDISCOVERY_SERVICE_EVENT_STATUS.ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED);
                        publishProgress(dataProgress);
                    }
                
                    @Override
                    public void serviceResolved(ServiceEvent event) 
                    {
                        ARSALPrint.d(TAG, "Service resolved: " + event.getName());
                        
                        Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS> dataProgress = new Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS>(event,eARDISCOVERY_SERVICE_EVENT_STATUS.ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED);
                        publishProgress(dataProgress);
                    }
                };
                
            }
            catch (IOException e) 
            {
                ARSALPrint.e(TAG, "mDNSManager creation failed.");
                e.printStackTrace();
            }
            
            isNetDiscovering = false;
            
            return null;
        }
        
        protected void onProgressUpdate(Object... progress) 
        {
            ARSALPrint.d(TAG,"onProgressUpdate");
            
            Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS> dataProgress = (Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS>) progress[0];
            
            switch(dataProgress.second)
            {
            
                case ARDISCOVERY_SERVICE_EVENT_STATUS_ADD :
                    ARSALPrint.d(TAG,"ARDISCOVERY_SERVICE_EVENT_STATUS_ADD");
                    notificationNetServiceDeviceAdd( dataProgress.first);
                    break;
                    
                case ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED :    
                    ARSALPrint.d(TAG,"ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED");
                    notificationNetServicesDevicesResolved( dataProgress.first);
                    break;
                    
                case ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED :
                    ARSALPrint.d(TAG,"ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED");
                    notificationNetServiceDeviceRemoved( dataProgress.first);
                    break;
                    
                default:
                    ARSALPrint.d(TAG, "error service event status " + dataProgress.second +" not known");
                    break;
            }
            
        }
        
        protected void onPostExecute(Object result)
        {
            /* add the net service listeners */
            for (String devicesService : devicesServiceArray)
            {
                ARSALPrint.d(TAG,"addServiceListener:" + devicesService);
                
                if (mDNSManager != null)
                {
                    mDNSManager.addServiceListener(devicesService, mDNSListener);
                }
                else
                {
                    ARSALPrint.w(TAG,"mDNSManager is null");
                }
            }
        }
        
    }
    
    /* BLE */
    private void bleConnect()
    {
        ARSALPrint.d(TAG,"bleConnect");
        
        if (bleIsAvalaible)
        {
            bleScanner.start();
        }
    }
    
    private void bleDisconnect()
    {
        ARSALPrint.d(TAG,"bleDisconnect");
        
        if (bleIsAvalaible)
        {
            bleScanner.stop();
        }
    }
    
    
    @TargetApi(18)
    private class BLEScanner
    {
        private static final long ARDISCOVERY_BLE_SCAN_PERIOD = 10000;
        private static final long ARDISCOVERY_BLE_SCAN_DURATION = 4000;
        public static final long ARDISCOVERY_BLE_TIMEOUT_DURATION = ARDISCOVERY_BLE_SCAN_PERIOD + ARDISCOVERY_BLE_SCAN_DURATION+6000;
        private boolean isStart;
        private boolean scanning;
        private Handler startBLEHandler;
        private Handler stopBLEHandler;
        private Runnable startScanningRunnable;
        private Runnable stopScanningRunnable;
        private HashMap<String, ARDiscoveryDeviceService> newBLEDeviceServicesHmap;
        
        private static final int ARDISCOVERY_BLE_MANUFACTURER_DATA_LENGTH_OFFSET = 3;
        private static final int ARDISCOVERY_BLE_MANUFACTURER_DATA_ADTYPE_OFFSET = 4;
        private static final int ARDISCOVERY_BLE_MANUFACTURER_DATA_LENGTH_WITH_ADTYPE = 9;
        private static final int ARDISCOVERY_BLE_MANUFACTURER_DATA_ADTYPE = 0xFF; 
        
        public BLEScanner()
        {
            ARSALPrint.d(TAG,"BLEScanningTask constructor");
            
            startBLEHandler = new Handler() ;
            stopBLEHandler = new Handler() ;
            
            startScanningRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                    startScanLeDevice();
                }
            };
            
            stopScanningRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                    periodScanLeDeviceEnd();
                }
            };
        }
        
        public void start()
        {
            ARSALPrint.d(TAG,"BLEScanningTask start");
            
            if (! isStart)
            {
                isStart = true;
                startScanningRunnable.run();
            }
        }
        
        private void startScanLeDevice()
        {
            ARSALPrint.d(TAG,"start scanLeDevice: bluetoothAdapter: " + bluetoothAdapter);

            /* reset newDeviceServicesHmap */
            newBLEDeviceServicesHmap = new HashMap<String, ARDiscoveryDeviceService>();
            
            /* Stops scanning after a pre-defined scan duration. */
            stopBLEHandler.postDelayed( stopScanningRunnable , ARDISCOVERY_BLE_SCAN_DURATION);

            scanning = true;
            bluetoothAdapter.startLeScan((BluetoothAdapter.LeScanCallback)leScanCallback);
            
            /* restart scanning after a pre-defined scan period. */
            startBLEHandler.postDelayed (startScanningRunnable, ARDISCOVERY_BLE_SCAN_PERIOD);
        }
        
        public void bleCallback (BluetoothDevice bleService, int rssi ,byte[] scanRecord)
        {
            ARSALPrint.d(TAG,"bleCallback");
            
            int productID = getParrotProductID (scanRecord);
            
            if (productID != 0)
             {
                ARDiscoveryDeviceBLEService deviceBLEService = new ARDiscoveryDeviceBLEService(bleService);
                deviceBLEService.setSignal(rssi);

                /* add the service in the array*/
                ARDiscoveryDeviceService deviceService = new ARDiscoveryDeviceService (bleService.getName(), deviceBLEService, productID);
                
                newBLEDeviceServicesHmap.put(deviceService.getName(), deviceService);
            }
        }
        
        /**
         * @brief get the parrot product id from the BLE scanRecord
         * @param scanRecord BLE scanRecord
         * @return the product ID of the parrot BLE device. return "0" if it is not a parrot device
         */
        private int getParrotProductID (byte[] scanRecord)
        {
            /* read the scanRecord  to check if it is a PARROT Delos device with the good version */
            
            /* scanRecord :
            * <---------------- 31 oct ------------------> 
            * | AD Struct 1    | AD Struct 2 |  AD Struct n | 
            * |                 \_____________________
            * | length (1 oct)    | data (length otc) |
            *                     |                     \_______________________
            *                     |AD type (n oct) | AD Data ( length - n oct) |
            * 
            * for Delos:
            * AD Struct 1 : (Flags)
            * - length = 0x02
            * - AD Type = 0x01
            * - AD data : 
            * 
            * AD Struct 2 : (manufacturerData)
            * - length = 0x09
            * - AD Type = 0xFF
            * - AD data : | BTVendorID (2 oct) | USBVendorID (2 oct) | USBProductID (2 oct) | VersionID (2 oct) |
            */
            
            int parrotProductID = 0;
            
            final int MASK = 0xFF;
            
            /* get the length of the manufacturerData */
            byte[] data = (byte[]) Arrays.copyOfRange(scanRecord, ARDISCOVERY_BLE_MANUFACTURER_DATA_LENGTH_OFFSET, ARDISCOVERY_BLE_MANUFACTURER_DATA_LENGTH_OFFSET + 1);
            int manufacturerDataLenght = (MASK & data[0]);
            
            /* check if it is the length expected */
            if (manufacturerDataLenght == ARDISCOVERY_BLE_MANUFACTURER_DATA_LENGTH_WITH_ADTYPE)
            {
                /* get the manufacturerData */
                data = (byte[]) Arrays.copyOfRange(scanRecord, ARDISCOVERY_BLE_MANUFACTURER_DATA_ADTYPE_OFFSET , ARDISCOVERY_BLE_MANUFACTURER_DATA_ADTYPE_OFFSET + manufacturerDataLenght);
                int adType = (MASK & data[0]);
                
                /* check if it is the AD Type expected */
                if (adType == ARDISCOVERY_BLE_MANUFACTURER_DATA_ADTYPE)
                {
                    int btVendorID = (data[1] & MASK) + ((data[2] & MASK) << 8);
                    int usbVendorID = (data[3] & MASK) + ((data[4] & MASK) << 8);
                    int usbProductID = (data[5] & MASK) + ((data[6] & MASK) << 8);
                    
                    /* check the vendorID, the usbVendorID end the productID */
                    if ((btVendorID == ARDISCOVERY_BT_VENDOR_ID) && 
                        (usbVendorID == ARDISCOVERY_USB_VENDOR_ID) && 
                        (usbProductID == getProductID(ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_MINIDRONE)) )
                    {
                        parrotProductID = usbProductID;
                    }
                }
            }
            
            return parrotProductID;
        }
        
        private void periodScanLeDeviceEnd()
        {
            ARSALPrint.d(TAG,"periodScanLeDeviceEnd");
            notificationBLEServiceDeviceUpDate (newBLEDeviceServicesHmap);
            stopScanLeDevice();
        }
        
        private void stopScanLeDevice()
        {
            ARSALPrint.d(TAG,"ScanLeDeviceAsyncTask stopLeScan");
            scanning = false;
            bluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback)leScanCallback);
        }

        public void stop()
        {
            ARSALPrint.d(TAG,"BLEScanningTask stop");
            
            if (leScanCallback != null)
            {
                bluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback)leScanCallback);
            }
            startBLEHandler.removeCallbacks(startScanningRunnable);
            stopBLEHandler.removeCallbacks(stopScanningRunnable);
            scanning = false;
            isStart = false;
        }
        
        public Boolean IsScanning()
        {
            return scanning;
        }
        
        public Boolean IsStart()
        {
            return isStart;
        }
        
    };
    
    @TargetApi(18)
    private void notificationBLEServiceDeviceUpDate( HashMap<String, ARDiscoveryDeviceService> newBLEDeviceServicesHmap )
    {
        mHandler.removeCallbacksAndMessages(null);
        ARSALPrint.d(TAG,"notificationBLEServiceDeviceAdd");
        
        /* if the BLEDeviceServices List has changed */
        if (bleServicesListHasChanged(newBLEDeviceServicesHmap))
        {
            /* get the new BLE Device Services list */
            bleDeviceServicesHmap = newBLEDeviceServicesHmap;
            
            /* broadcast the new deviceServiceList */
            broadcastDeviceServiceArrayUpdated ();
        }

        mHandler.postDelayed(new Runnable(){

            @Override
            public void run()
            {
                ARSALPrint.d(TAG,"BLE scan timeout ! clear BLE devices");
                bleDeviceServicesHmap.clear();
                /* broadcast the new deviceServiceList */
                broadcastDeviceServiceArrayUpdated();
            }
        }, BLEScanner.ARDISCOVERY_BLE_TIMEOUT_DURATION);
    }
    
    private boolean bleServicesListHasChanged ( HashMap<String, ARDiscoveryDeviceService> newBLEDeviceServicesHmap )
    {
        /* check is the list of BLE devices has changed */
        ARSALPrint.d(TAG,"bleServicesListHasChanged");
        
        boolean res = false;
        
        if (bleDeviceServicesHmap.size() != newBLEDeviceServicesHmap.size())
        {
            /* if the number of devices has changed */
            res = true;
        }
        else if (!bleDeviceServicesHmap.keySet().equals(newBLEDeviceServicesHmap.keySet()))
        {
            /* if the names of devices has changed */
            res = true;
        }
        else
        {
            for (ARDiscoveryDeviceService bleDevice : bleDeviceServicesHmap.values())
            {
                /* check from the MAC address */
                if (!newBLEDeviceServicesHmap.containsValue(bleDevice))
                {
                    /* if one of the old devices is not present is the new list */
                    res = true;
                }
            }
        }
        
        return res;
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
        return publishService(getProductID(product), name, port);
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
        unpublishServices();

        Thread t = new Thread(new Runnable()
            {
                public void run()
                {
                    String type = String.format("_arsdk-%04x._udp.local.", product_id);
                    ARSALPrint.e(TAG, "Publish service <" + type + " | " + name + " | " + port + " >");
                    publishedService = ServiceInfo.create(type, name, port, 1, 1, ServiceInfo.NO_VALUE);
                    boolean success = false;
                    try
                    {
                        mDNSManager.registerService(publishedService);
                        success = true;
                    }
                    catch (IOException ioe)
                    {
                        ARSALPrint.e(TAG, "Error publishing service");
                        ioe.printStackTrace();
                    }
                    catch (Throwable t)
                    {
                        ARSALPrint.e(TAG, "Oops ...");
                        t.printStackTrace();
                    }
                }
            });
        t.start();
        // return success;
        return true;
    }

    /**
     * @brief Unpublishes any published service
     */
    public void unpublishServices()
    {
        if (mDNSManager != null)
        {
            mDNSManager.unregisterAllServices();
        }
        publishedService = null;
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

