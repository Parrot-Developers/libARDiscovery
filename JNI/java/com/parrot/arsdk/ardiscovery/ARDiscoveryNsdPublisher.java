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
public class ARDiscoveryNsdPublisher implements ARDiscoveryWifiPublisher
{
    private static final String TAG = ARDiscoveryNsdDiscovery.class.getSimpleName();

    // NsdManager
    private NsdManager mNsdManager;
    
    // Listeners
    private NsdManager.RegistrationListener mRegistrationListener;

    // Published service info
    private String mServiceName;
    private boolean published;

    private boolean opened;
    private ARDiscoveryService broadcaster;
    private Context context;

    public ARDiscoveryNsdPublisher()
    {
        opened = false;

        initializeRegistrationListener();
    }

    public synchronized void open(ARDiscoveryService broadcaster, Context c)
    {
        this.broadcaster = broadcaster;
        this.context = c;
        if (opened)
        {
            return;
        }

        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        opened = true;
        
    }

    public synchronized void close()
    {
        if (! opened)
        {
            return;
        }

        this.broadcaster = null;
        this.context = null;

        opened = false;
    }


    public void update()
    {
        // Nothing here, we always are publishing, regardless of the network type
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
}
