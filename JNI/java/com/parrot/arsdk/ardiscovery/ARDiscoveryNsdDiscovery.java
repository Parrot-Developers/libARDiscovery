package com.parrot.arsdk.ardiscovery;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.net.InetAddress;

import com.parrot.arsdk.arsal.ARSALPrint;

import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.content.Context;

/**
 * Android Nsd implementation of the wifi part of ARDiscoveryService
 */
public class ARDiscoveryNsdDiscovery implements ARDiscoveryWifiDiscovery
{
    private static final String TAG = ARDiscoveryNsdDiscovery.class.getSimpleName();

    private List<String> devicesServiceArray;

    private HashMap<String, ARDiscoveryDeviceService> netDeviceServicesHmap;

    // Listeners
    private NsdManager.RegistrationListener mRegistrationListener;
    private HashMap<String, NsdManager.DiscoveryListener> mDiscoveryListeners;
    private NsdManager.ResolveListener mResolveListener;

    // NsdManager
    private NsdManager mNsdManager;

    // Published service info
    private String mServiceName;
    private boolean published;

    private boolean opened;
    private ARDiscoveryService broadcaster;
    private Context context;

    public ARDiscoveryNsdDiscovery()
    {
        opened = false;

        /**
         * devicesServiceArray init
         */
        devicesServiceArray = new ArrayList<String>();
        //TODO: workaround for the skyController
        String devicesService = String.format (ARDiscoveryService.ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT, ARDiscoveryService.nativeGetProductID(ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE.getValue()));
        devicesServiceArray.add(devicesService);
        /*
        for (int i = ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_NSNETSERVICE.getValue() ; i < ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_BLESERVICE.getValue(); ++i)
        {
            String devicesService = String.format (ARDiscoveryService.ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT, ARDiscoveryService.nativeGetProductID(i));
            
            devicesServiceArray.add(devicesService);
        }*/

        netDeviceServicesHmap = new HashMap<String, ARDiscoveryDeviceService> ();

        initializeRegistrationListener();
        initializeDiscoveryListeners();
        initializeResolveListener();
    }

    public synchronized void open(ARDiscoveryService broadcaster, Context c)
    {
        this.broadcaster = broadcaster;
        this.context = c;
        if (opened)
        {
            return;
        }

        netDeviceServicesHmap.clear();
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        opened = true;

        for (String type : devicesServiceArray)
        {
            ARSALPrint.i(TAG, "Will start searching for devices of type <" + type + ">");
            
            ARSALPrint.i(TAG, "NsdManager.PROTOCOL_DNS_SD:" + NsdManager.PROTOCOL_DNS_SD +" mDiscoveryListeners.get(type):" + mDiscoveryListeners.get(type));
            
            mNsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListeners.get(type));
        }
        
    }

    public synchronized void close()
    {
        if (! opened)
        {
            return;
        }

        for (String type : devicesServiceArray)
        {
            ARSALPrint.i(TAG, "Will stop searching for devices of type <" + type + ">");
            mNsdManager.stopServiceDiscovery(mDiscoveryListeners.get(type));
        }

        this.broadcaster = null;
        this.context = null;

        opened = false;
    }


    public void update()
    {
        // Nothing here, we always are discovering, regardless of the network type
    }
    
    public void start()
    {
        ARSALPrint.d(TAG, "start ... not implemented");
    }

    public void restart()
    {
        ARSALPrint.d(TAG, "restarting discovery ...");
        if (devicesServiceArray != null && mNsdManager != null && mDiscoveryListeners != null)
        {
            for (String type : devicesServiceArray)
            {
                ARSALPrint.i(TAG, "Will start searching for devices of type <" + type + ">");
            
                ARSALPrint.i(TAG, "NsdManager.PROTOCOL_DNS_SD:" + NsdManager.PROTOCOL_DNS_SD +" mDiscoveryListeners.get(type):" + mDiscoveryListeners.get(type));
            
                mNsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListeners.get(type));
            }
        }
    }
    
    public void stop()
    {
        ARSALPrint.d(TAG, "stop ... not implemented");
        if (devicesServiceArray != null && mNsdManager != null && mDiscoveryListeners != null)
        {
            for (String type : devicesServiceArray)
            {
                ARSALPrint.i(TAG, "Will stop searching for devices of type <" + type + ">");
                mNsdManager.stopServiceDiscovery(mDiscoveryListeners.get(type));
            }
        }
        
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
        return publishService(ARDiscoveryService.getProductID(product), name, port);
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
        if (opened)
        {
            unpublishService();

            NsdServiceInfo serviceInfo = new NsdServiceInfo();

            serviceInfo.setServiceName(name);
            serviceInfo.setPort(port);
            String type = String.format(ARDiscoveryService.ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT, product_id);
            serviceInfo.setServiceType(type);

            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
            
            published = true;
        }

        return published;
    }

    /**
     * @brief Unpublishes any published service
     */
    public void unpublishService()
    {
        if (published)
        {
            mNsdManager.unregisterService(mRegistrationListener);
            mServiceName = null;
            published = false;
        }
    }

    private void initializeRegistrationListener()
    {
        mRegistrationListener = new NsdManager.RegistrationListener()
            {

                @Override
                public void onServiceRegistered(NsdServiceInfo NsdServiceInfo)
                {
                    // Save the service name.  Android may have changed it in order to
                    // resolve a conflict, so update the name you initially requested
                    // with the name Android actually used.
                    mServiceName = NsdServiceInfo.getServiceName();
                }

                @Override
                public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
                {
                    // Registration failed!  Put debugging code here to determine why.
                }

                @Override
                public void onServiceUnregistered(NsdServiceInfo arg0)
                {
                    // Service has been unregistered.  This only happens when you call
                    // NsdManager.unregisterService() and pass in this listener.
                }

                @Override
                public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
                {
                    // Unregistration failed.  Put debugging code here to determine why.
                }
            };
    }

    private void initializeDiscoveryListeners()
    {
        mDiscoveryListeners = new HashMap<String, NsdManager.DiscoveryListener> ();
        for (String type : devicesServiceArray)
        {
            // Instantiate a new DiscoveryListener
            NsdManager.DiscoveryListener dl = new NsdManager.DiscoveryListener()
                {

                    //  Called as soon as service discovery begins.
                    @Override
                    public void onDiscoveryStarted(String regType)
                    {
                        ARSALPrint.i(TAG, "Service discovery started");
                    }

                    @Override
                    public void onServiceFound(NsdServiceInfo service)
                    {
                        // A service was found!  Do something with it.
                        ARSALPrint.i(TAG, "Service discovery success" + service);
                        boolean validType = false;
                        boolean shouldBeAdded = true;
                        for (String type : devicesServiceArray)
                        {
                            if (service.getServiceType().equals(type))
                            {
                                validType = true;
                                break;
                            }
                        }

                        if (validType)
                        {
                            if (service.getServiceName().equals(mServiceName))
                            {
                                // Do not add our own service to the list
                                shouldBeAdded = false;
                            }
                        }

                        if (shouldBeAdded)
                        {
                            mNsdManager.resolveService(service, mResolveListener);
                        }
                    }

                    @Override
                    public void onServiceLost(NsdServiceInfo service)
                    {
                        // When the network service is no longer available.
                        // Internal bookkeeping code goes here.
                        ARSALPrint.i(TAG, "service lost" + service);

                        /* remove from the deviceServicesHmap */
                        ARDiscoveryDeviceService deviceServiceRemoved = netDeviceServicesHmap.remove(service.getServiceName());

                        if(deviceServiceRemoved != null)
                        {
                            /* broadcast the new deviceServiceList */
                            broadcaster.broadcastDeviceServiceArrayUpdated ();
                        }
                        else
                        {
                            ARSALPrint.e(TAG, "service: "+ service.getServiceName() + " not known");
                        }
                    }

                    @Override
                    public void onDiscoveryStopped(String serviceType)
                    {
                        ARSALPrint.i(TAG, "Discovery stopped: " + serviceType);
                    }

                    @Override
                    public void onStartDiscoveryFailed(String serviceType, int errorCode)
                    {
                        ARSALPrint.e(TAG, "onStartDiscoveryFailed ... Discovery failed: Error code:" + errorCode);
                    }

                    @Override
                    public void onStopDiscoveryFailed(String serviceType, int errorCode)
                    {
                        ARSALPrint.e(TAG, "onStopDiscoveryFailed ... Discovery failed: Error code:" + errorCode);
                    }
                };
            mDiscoveryListeners.put(type, dl);
        }
    }

    private void initializeResolveListener()
    {
        mResolveListener = new NsdManager.ResolveListener()
        {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                // Called when the resolve fails.  Use the error code to debug.
                ARSALPrint.e(TAG, "Resolve failed " + errorCode);
                // if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE)
                // {
                //     onServiceResolved(serviceInfo);
                // }
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo)
            {
                ARSALPrint.i(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName))
                {
                    ARSALPrint.w(TAG, "Same IP.");
                    return;
                }
                int port = serviceInfo.getPort();
                InetAddress host = serviceInfo.getHost();
                String ip = host.getHostAddress();

                boolean known = netDeviceServicesHmap.containsKey(serviceInfo.getServiceName());

                ARSALPrint.i (TAG, "IP = " + ip + ", Port = " + port + ", Known ? " + known);

                /* add the service if it not known yet*/
                if ((ip != null) && (!known))
                {
                    String serviceInfoType = serviceInfo.getServiceType().substring(1, serviceInfo.getServiceType().length()) + ".";
                    
                    ARDiscoveryDeviceNetService deviceNetService = new ARDiscoveryDeviceNetService(serviceInfo.getServiceName(), serviceInfoType, ip, port);
                    int productID = 0;

                    /* find device type */
                    for (int i = ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_NSNETSERVICE.getValue(); i < ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_BLESERVICE.getValue(); ++i)
                    {
                        String type = devicesServiceArray.get(i);
                        ARSALPrint.d(TAG, "Checking <" + deviceNetService.getType() + "> against <" + type + ">");
                        if (deviceNetService.getType().equals(type))
                        {
                            productID = ARDiscoveryService.nativeGetProductID(i);
                            ARSALPrint.d(TAG, "Match ! Productid = " + productID);
                            break;
                        }
                    }

                    if (productID != 0)
                    {
                        /* add the service in the array */
                        ARDiscoveryDeviceService deviceService = new ARDiscoveryDeviceService (serviceInfo.getServiceName(), deviceNetService, productID);
                        netDeviceServicesHmap.put(deviceService.getName(), deviceService);

                        /* broadcast the new deviceServiceList */
                        broadcaster.broadcastDeviceServiceArrayUpdated ();
                    }
                    else
                    {
                        ARSALPrint.e(TAG,"Found an unknown service : " + deviceNetService);
                    }
                }
            }
        };
    }

    public List<ARDiscoveryDeviceService> getDeviceServicesArray()
    {
        return new ArrayList<ARDiscoveryDeviceService> (netDeviceServicesHmap.values());
    }


}
