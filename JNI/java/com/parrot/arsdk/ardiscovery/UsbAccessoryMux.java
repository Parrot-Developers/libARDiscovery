/*
    Copyright (C) 2016 Parrot SA

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


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.parrot.mux.Mux;

import java.io.IOException;

/**
 * Singleton that hold usb accessory mux instance
 */
public class UsbAccessoryMux {

    public static final String ACTION_USB_ACCESSORY_ATTACHED = "com.parrot.arsdk.USB_ACCESSORY_ATTACHED";
    private static final String ACTION_USB_PERMISSION = "com.parrot.arsdk.USB_ACCESSORY_PERMISSION";

    private static final String MANUFACTURER_ID = "Parrot";
    private static final String SKYCONTROLLER2_MODEL_ID = "Skycontroller 2";

    private static final String TAG = "UsbAccessoryMux";
    private static UsbAccessoryMux sInstance;

    private final Context context;
    // mux
    private final UsbManager usbManager;
    private Mux usbMux;
    private ParcelFileDescriptor muxFileDescriptor;
    private Thread muxThread;
    // discovery
    private ARDiscoveryMux discoveryChannel;
    private ARDiscoveryMux.Listener mDiscoveryListener;


    public static UsbAccessoryMux get(Context appContext) {
        synchronized (UsbAccessoryMux.class) {
            if (sInstance == null) {
                sInstance = new UsbAccessoryMux(appContext);
            }
            return sInstance;
        }
    }

    private UsbAccessoryMux(Context appContext) {
        Log.i(TAG, "create UsbAccessoryMux");
        this.context = appContext;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        IntentFilter filter = new IntentFilter(ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbAccessoryReceiver, filter);

        // check for connected accessory
        UsbAccessory[] accessoryList = usbManager.getAccessoryList();
        if (accessoryList != null) {
            for (UsbAccessory accessory : accessoryList) {
                if (MANUFACTURER_ID.equals(accessory.getManufacturer()) && SKYCONTROLLER2_MODEL_ID.equals(accessory.getModel())) {
                    usbManager.requestPermission(accessory, PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0));
                }
            }
        }
    }

    public void setDiscoveryListener(ARDiscoveryMux.Listener listener) {
        mDiscoveryListener = listener;
        if (discoveryChannel != null) {
            discoveryChannel.setListener(mDiscoveryListener);
        }
    }

    public int connect(String device, String model, String id, String json, ARDiscoveryMux.ConnectCallback callback) {
        if (discoveryChannel != null) {
            return discoveryChannel.connect(device, model, id, json, callback);
        }
        return -1;
    }

    public void cancelConnect() {
        if (discoveryChannel != null) {
            discoveryChannel.cancelConnect();
        }
    }

    private void startMux(UsbAccessory accessory) {
        Log.i(TAG, "Accessory connected " + accessory);
        synchronized (this) {
            if (usbMux == null) {
                muxFileDescriptor = usbManager.openAccessory(accessory);
                if (muxFileDescriptor != null) {
                    Log.i(TAG, "Opening mux, fd=" + muxFileDescriptor.getFd());
                    usbMux = new Mux(muxFileDescriptor, onCloseListener);
                    if (usbMux.isValid()) {
                        // start mux thread
                        muxThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                usbMux.runReader();
                            }
                        }, "muxThread");
                        muxThread.start();
                        discoveryChannel = new ARDiscoveryMux(usbMux);
                        if (mDiscoveryListener != null) {
                            discoveryChannel.setListener(mDiscoveryListener);
                        }
                    } else {
                        Log.i(TAG, "Error opening usb mux");
                        usbMux = null;
                        try {
                            muxFileDescriptor.close();
                        } catch (IOException e) {
                        }
                    }
                } else {
                    Log.e(TAG, "Error opening USB Accessory");
                }
            }
        }
    }

    private void closeMux() {
        synchronized (this) {
            if (discoveryChannel != null) {
                discoveryChannel.destroy();
                discoveryChannel = null;
            }
            if (usbMux != null) {
                usbMux.stop();
                usbMux.destroy();
                usbMux = null;
            }
            if (muxFileDescriptor != null) {
                try {
                    muxFileDescriptor.close();
                } catch (IOException e) {
                }
                muxFileDescriptor = null;
            }
        }
    }

    private final Mux.IOnClosedListener onCloseListener = new Mux.IOnClosedListener() {
        @Override
        public void onClosed() {
            closeMux();
        }
    };

    private BroadcastReceiver mUsbAccessoryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, true);
            UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
            Log.i(TAG, "mUsbAccessoryReceiver " + intent.getAction());
            if (usbMux == null && permissionGranted && accessory != null && MANUFACTURER_ID.equals(accessory
                    .getManufacturer()) && SKYCONTROLLER2_MODEL_ID.equals(accessory.getModel())) {
                startMux(accessory);
            }
        }
    };

    public Mux getMux() {
        return usbMux;
    }
}



