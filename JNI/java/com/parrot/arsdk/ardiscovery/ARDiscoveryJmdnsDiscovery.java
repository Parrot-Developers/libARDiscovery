/*
    Copyright (C) 2014 Parrot SA

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in
      the documentation and/or other materials provided with the 
      distribution.
    * Neither the name of Parrot nor the names
      of its contributors may be used to endorse or promote products
      derived from this software without specific prior written
      permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
    "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
    LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
    FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
    COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
    BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
    OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED 
    AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
    OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
    SUCH DAMAGE.
*/
package com.parrot.arsdk.ardiscovery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import com.parrot.arsdk.arsal.ARSALPrint;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Pair;
import android.os.AsyncTask;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;


/**
 * Jmdns implementation of the wifi part of ARDiscoveryService
 */
public class ARDiscoveryJmdnsDiscovery implements ARDiscoveryWifiDiscovery
{
    private static final String TAG = ARDiscoveryJmdnsDiscovery.class.getSimpleName();

    private JmDNS mDNSManager;
    private ServiceListener mDNSListener;
    private List<String> devicesServiceArray;

    private ServiceInfo publishedService;

    private AsyncTask<Object, Object, Object> jmdnsCreatorAsyncTask;
    
    private Boolean isNetDiscovering = false;
    private Boolean askForNetDiscovering = false;

    private IntentFilter networkStateChangedFilter;
    private BroadcastReceiver networkStateIntentReceiver;

    private HashMap<String, ARDiscoveryDeviceService> netDeviceServicesHmap;

    private boolean opened;

    private ARDiscoveryService broadcaster;
    private Context context;
    
    private String hostIp;
    private InetAddress hostAddress;
    static private InetAddress nullAddress;

    public ARDiscoveryJmdnsDiscovery()
    {
        opened = false;

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
            String devicesService = String.format (ARDiscoveryService.ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT, ARDiscoveryService.nativeGetProductID(i));

            devicesService += ARDiscoveryService.ARDISCOVERY_SERVICE_NET_DEVICE_DOMAIN;

            devicesServiceArray.add(devicesService);
        }

        netDeviceServicesHmap = new HashMap<String, ARDiscoveryDeviceService> ();

        networkStateChangedFilter = new IntentFilter();
        networkStateChangedFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        networkStateIntentReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
                {
                    update();
                }
            }
        };
    }

    public synchronized void open(ARDiscoveryService broadcaster, Context c)
    {
        this.broadcaster = broadcaster;
        this.context = c;
        if (opened)
        {
            return;
        }

        netDeviceServicesHmap = new HashMap<String, ARDiscoveryDeviceService> ();

        context.registerReceiver(networkStateIntentReceiver, networkStateChangedFilter);

        opened = true;
    }

    public synchronized void close()
    {
        if (! opened)
        {
            return;
        }

        context.unregisterReceiver(networkStateIntentReceiver);

        mdnsDisconnect();
        //mdnsDestroy();

        this.broadcaster = null;
        this.context = null;

        opened = false;
    }

    private void mdnsDestroy()
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


    public void update()
    {
        /* If we have wifi or ethernet connected, connect discovery */
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mEth = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        
        if (((mWifi != null) && (mWifi.isConnected())) ||
            ((mEth  != null) && (mEth.isConnected())))
        {
            if(askForNetDiscovering)
            {
                mdnsConnect();
                askForNetDiscovering = false;
            }
        }
        else
        {
            if (isNetDiscovering)
            {
                askForNetDiscovering = true;
            }
            
            mdnsDisconnect();
            
            ArrayList<ARDiscoveryDeviceService> netDeviceServicesArray = new ArrayList<ARDiscoveryDeviceService> (netDeviceServicesHmap.values());
            
            /* remove all net services */
            for (ARDiscoveryDeviceService s : netDeviceServicesArray)
            {
                notificationNetServiceDeviceRemoved (s);
            }
        }
    }
    
    public void start()
    {
        if (!isNetDiscovering)
        {
            /* if the wifi is connected get its hostAddress */
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
    }
    
    public void stop()
    {
        if (isNetDiscovering)
        {
            /* Stop net scan */
            mdnsDisconnect();
        }
    }

    private void mdnsConnect()
    {
        /* if jmdns is not running yet */
        if (mDNSManager == null)
        {
            
             /* get the host address */
            WifiManager wifi =   (WifiManager) context.getSystemService(android.content.Context.WIFI_SERVICE);

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
            
            if (isNetDiscovering == false)
            {
                isNetDiscovering = true;

                jmdnsCreatorAsyncTask = new JmdnsCreatorAsyncTask();
                jmdnsCreatorAsyncTask.execute();
            }
        }
    }

    private void mdnsDisconnect()
    {
        /* reset */
        hostAddress = nullAddress;
        unpublishService();
        mdnsDestroy();
        jmdnsCreatorAsyncTask = null;
        isNetDiscovering = false;
    }

    private void notificationNetServiceDeviceAdd(ServiceEvent serviceEvent)
    {
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
                    productID = ARDiscoveryService.nativeGetProductID(i);
                    break;
                }
            }

            if (productID != 0)
            {
                /* add the service in the array */
                ARDiscoveryDeviceService deviceService = new ARDiscoveryDeviceService (serviceEvent.getName(), deviceNetService, productID);
                netDeviceServicesHmap.put(deviceService.getName(), deviceService);

                if (broadcaster != null)
                {
                    /* broadcast the new deviceServiceList */
                    broadcaster.broadcastDeviceServiceArrayUpdated ();
                }
            }
            else
            {
                ARSALPrint.d(TAG,"Found an unknown service : " + deviceNetService);
            }

        }
    }

    private void notificationNetServicesDevicesResolved(ServiceEvent serviceEvent)
    {
        ARSALPrint.d(TAG,"notificationServicesDevicesResolved");

        /* check if the service is known */
        boolean known = netDeviceServicesHmap.containsKey(serviceEvent.getName());

        /* add the service if it not known yet*/
        if(!known)
        {
            ARSALPrint.d(TAG,"service Resolved not know : "+ serviceEvent);
            notificationNetServiceDeviceAdd(serviceEvent);
        }

    }

    private void notificationNetServiceDeviceRemoved(ServiceEvent serviceEvent)
    {
        ARSALPrint.d(TAG,"notificationServiceDeviceRemoved");

        /* remove from the deviceServicesHmap */
        ARDiscoveryDeviceService deviceServiceRemoved = netDeviceServicesHmap.remove(serviceEvent.getName());

        if ((deviceServiceRemoved != null) && (broadcaster != null))
        {
            /* broadcast the new deviceServiceList */
            broadcaster.broadcastDeviceServiceArrayUpdated ();
        }
        else
        {
            ARSALPrint.w(TAG, "service: "+ serviceEvent.getInfo().getName() + " not known");
        }
    }

    private void notificationNetServiceDeviceRemoved(ARDiscoveryDeviceService deviceService)
    {
        ARSALPrint.d(TAG,"notificationServiceDeviceRemoved");

        /* remove from the deviceServicesHmap */
        ARDiscoveryDeviceService deviceServiceRemoved = netDeviceServicesHmap.remove(deviceService.getName());

        if ((deviceServiceRemoved != null) && (broadcaster != null))
        {
            /* broadcast the new deviceServiceList */
            broadcaster.broadcastDeviceServiceArrayUpdated ();
        }
        else
        {
            ARSALPrint.w(TAG, "service: "+ deviceService.getName() + " not known");
        }
    }

    private String getServiceIP(ServiceEvent serviceEvent)
    {
        ARSALPrint.d(TAG,"getServiceIP serviceEvent: " + serviceEvent);

        String serviceIP = null;

        ServiceInfo info = serviceEvent.getDNS().getServiceInfo(serviceEvent.getType(), serviceEvent.getName());
        if((info != null) && (info.getInet4Addresses().length > 0))
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

    private class JmdnsCreatorAsyncTask extends AsyncTask<Object, Object, Object>
    {

        @Override
        protected Object doInBackground(Object... params)
        {
            try
            {
                if ((hostAddress != null) && (!hostAddress.equals( nullAddress )) )
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

                        Pair<ServiceEvent,ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS> dataProgress = new Pair<ServiceEvent,ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS>(event,ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS.ARDISCOVERY_SERVICE_EVENT_STATUS_ADD);
                        publishProgress(dataProgress);
                    }

                    @Override
                    public void serviceRemoved(ServiceEvent event)
                    {
                        ARSALPrint.d(TAG,"Service removed: " + event.getName());

                        Pair<ServiceEvent,ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS> dataProgress = new Pair<ServiceEvent,ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS>(event,ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS.ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED);
                        publishProgress(dataProgress);
                    }

                    @Override
                    public void serviceResolved(ServiceEvent event)
                    {
                        ARSALPrint.d(TAG, "Service resolved: " + event.getName());

                        Pair<ServiceEvent,ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS> dataProgress = new Pair<ServiceEvent,ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS>(event,ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS.ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED);
                        publishProgress(dataProgress);
                    }
                };
            }
            catch (IOException e)
            {
                ARSALPrint.e(TAG, "mDNSManager creation failed.");
                e.printStackTrace();
                askForNetDiscovering = true;
            }
            
            //the doInBackground is ended but the AsyncTask is even
            //all mdns callbacks will in the AsyncTask

            return null;
        }

        protected void onProgressUpdate(Object... progress)
        {
            ARSALPrint.d(TAG,"onProgressUpdate");

            Pair<ServiceEvent,ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS> dataProgress = (Pair<ServiceEvent,ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS>) progress[0];

            switch(dataProgress.second)
            {

            case ARDISCOVERY_SERVICE_EVENT_STATUS_ADD :
                ARSALPrint.d(TAG,"ARDISCOVERY_SERVICE_EVENT_STATUS_ADD");
                notificationNetServiceDeviceAdd(dataProgress.first);
                break;

            case ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED :
                ARSALPrint.d(TAG,"ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED");
                notificationNetServicesDevicesResolved(dataProgress.first);
                break;

            case ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED :
                ARSALPrint.d(TAG,"ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED");
                notificationNetServiceDeviceRemoved(dataProgress.first);
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
        unpublishService();

        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                String type = String.format("_arsdk-%04x._udp.local.", product_id);
                ARSALPrint.d(TAG, "Publish service <" + type + " | " + name + " | " + port + " >");
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
    public void unpublishService()
    {
        if (mDNSManager != null)
        {
            mDNSManager.unregisterAllServices();
        }
        publishedService = null;
    }


    public List<ARDiscoveryDeviceService> getDeviceServicesArray()
    {
        return new ArrayList<ARDiscoveryDeviceService> (netDeviceServicesHmap.values());
    }
}
