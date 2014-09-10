package com.parrot.arsdk.ardiscovery;

import java.util.List;

import android.content.Context;

/**
 * Interface for the BLE parts of the ARDiscoveryService
 */
public interface ARDiscoveryBLEDiscovery
{
    public void open(ARDiscoveryService broadcaster, Context c);
    public void close();
    public void update();
    public void start();
    public void stop();

    public List<ARDiscoveryDeviceService> getDeviceServicesArray();
}
