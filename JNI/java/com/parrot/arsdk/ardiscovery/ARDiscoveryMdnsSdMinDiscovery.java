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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.parrot.arsdk.ardiscovery.mdnssdmin.MdnsSdMin;
import com.parrot.arsdk.arsal.ARSALPrint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

/**
 * Custom mdns-sd implementation of the wifi part of ARDiscoveryService
 */
public class ARDiscoveryMdnsSdMinDiscovery implements ARDiscoveryWifiDiscovery
{

    private static final String TAG = ARDiscoveryMdnsSdMinDiscovery.class.getSimpleName();
    private final MdnsSdMin mdnsSd;
    private final IntentFilter networkStateChangedFilter;
    private final Map<String, ARDiscoveryDeviceService> netDeviceServicesHmap;
    private final List<String> devicesServiceArray;
    private ARDiscoveryService broadcaster;
    private Context context;
    private WifiManager.MulticastLock multicastLock;
    private boolean started;

    public ARDiscoveryMdnsSdMinDiscovery()
    {
        ARSALPrint.v(TAG, "Creating MdsnSd based ARDiscovery");
        // build the list of services to look for
        devicesServiceArray = new ArrayList<String>();
        for (int i = ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_NSNETSERVICE.getValue(); i < ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_BLESERVICE.getValue(); ++i)
        {
            String devicesService = String.format(ARDiscoveryService.ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT, ARDiscoveryService.nativeGetProductID(i));
            devicesService += ARDiscoveryService.ARDISCOVERY_SERVICE_NET_DEVICE_DOMAIN;
            devicesServiceArray.add(devicesService);
        }

        netDeviceServicesHmap = new HashMap<String, ARDiscoveryDeviceService>();
        mdnsSd = new MdnsSdMin(devicesServiceArray.toArray(new String[devicesServiceArray.size()]), mdsnSdListener);

        // create the connectivity change receiver
        networkStateChangedFilter = new IntentFilter();
        networkStateChangedFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    public synchronized void open(ARDiscoveryService broadcaster, Context c)
    {
        ARSALPrint.v(TAG, "Opening MdsnSd based ARDiscovery");
        this.broadcaster = broadcaster;
        this.context = c;
        // create a multicast lock
        WifiManager wifi =  (WifiManager) context.getSystemService(android.content.Context.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("ARDiscovery");
    }

    public synchronized void close()
    {
        ARSALPrint.v(TAG, "Closing MdsnSd based ARDiscovery");
        if (started)
        {
            stop();
        }
        mdnsSd.stop();
        this.broadcaster = null;
        this.context = null;
    }

    public synchronized void start()
    {
        ARSALPrint.v(TAG, "Starting MdsnSd based ARDiscovery");
        if (!multicastLock.isHeld())
        {
            multicastLock.acquire();
        }
        // this is sticky intent, receiver will be called asap
        context.registerReceiver(networkStateIntentReceiver, networkStateChangedFilter);
        started = true;
    }

    public synchronized void stop()
    {
        ARSALPrint.v(TAG, "Stopping MdsnSd based ARDiscovery");
        started = false;
        if (multicastLock.isHeld())
        {
            multicastLock.release();
        }
        context.unregisterReceiver(networkStateIntentReceiver);
        mdnsSd.stop();
        netDeviceServicesHmap.clear();
        broadcaster.broadcastDeviceServiceArrayUpdated();
    }

    @Override
    public void update()
    {
        // noting to do here
    }

    @Override
    public List<ARDiscoveryDeviceService> getDeviceServicesArray()
    {
        return new ArrayList<ARDiscoveryDeviceService>(netDeviceServicesHmap.values());
    }

    @Override
    public boolean publishService(ARDISCOVERY_PRODUCT_ENUM product, String name, int port)
    {
        // not supported yet
        return false;
    }

    @Override
    public boolean publishService(int product_id, String name, int port)
    {
        // not supported yet
        return false;
    }

    @Override
    public void unpublishService()
    {
        // not supported yet
    }

    private final BroadcastReceiver networkStateIntentReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
            {
                mdnsSd.stop();

                ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                NetworkInfo mEth = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

                if (((mWifi != null) && (mWifi.isConnected())) || ((mEth != null) && (mEth.isConnected())))
                {
                    ARSALPrint.v(TAG, "Restaring MdsnSd");
                    mdnsSd.start();
                }
                else
                {
                    netDeviceServicesHmap.clear();
                    broadcaster.broadcastDeviceServiceArrayUpdated();
                }
            }
        }
    };

    private final MdnsSdMin.Listener mdsnSdListener = new MdnsSdMin.Listener()
    {
        @Override
        public void onServiceAdded(String name, String serviceType, String ipAddress, int port, String[] txts)
        {
            String serialNumber = null;
            if (txts != null && txts.length >=1)
            {
                serialNumber = txts[0];
            }
            ARDiscoveryDeviceNetService deviceNetService = new ARDiscoveryDeviceNetService(name, serviceType, ipAddress,
                    port, serialNumber);

            /* find device type */
            int productID = 0;
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
                ARDiscoveryDeviceService deviceService = new ARDiscoveryDeviceService(name, deviceNetService, productID);
                netDeviceServicesHmap.put(deviceService.getName(), deviceService);
                if (broadcaster != null)
                {
                    // got an answer, stop sending queries
                    mdnsSd.cancelSendQueries();
                    // broadcast the new deviceServiceList
                    broadcaster.broadcastDeviceServiceArrayUpdated();
                }
            }
        }

        @Override
        public void onServiceRemoved(String name, String serviceType)
        {
            ARDiscoveryDeviceService deviceServiceRemoved = netDeviceServicesHmap.remove(name);
            if (deviceServiceRemoved != null)
            {
                broadcaster.broadcastDeviceServiceArrayUpdated();
            }
        }
    };

}
