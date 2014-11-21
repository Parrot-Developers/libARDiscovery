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

import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothGattCallback;
import android.os.Parcel;
import android.os.Parcelable;
//import com.parrot.arsdk.argraphics.ARApplication;
import android.content.Intent;

import com.parrot.arsdk.arsal.ARSALPrint;

public class ARDiscoveryDeviceBLEService implements Parcelable
{
    /**
     * 
     */

    private static String TAG = "ARDiscoveryDeviceBLEService";

    private BluetoothDevice bluetoothDevice;
    private int signal;

    public static final Parcelable.Creator<ARDiscoveryDeviceBLEService> CREATOR = new Parcelable.Creator<ARDiscoveryDeviceBLEService>()
    {
        @Override
        public ARDiscoveryDeviceBLEService createFromParcel(Parcel source)
        {
            return new ARDiscoveryDeviceBLEService(source);
        }

        @Override
        public ARDiscoveryDeviceBLEService[] newArray(int size)
        {
            return new ARDiscoveryDeviceBLEService[size];
        }
    };

    public ARDiscoveryDeviceBLEService ()
    {
        bluetoothDevice = null;
        signal = 0;
    }

    public ARDiscoveryDeviceBLEService (BluetoothDevice bluetoothDevice)
    {
        this.bluetoothDevice = bluetoothDevice;
    }

    /* Parcelling part */
    public ARDiscoveryDeviceBLEService(Parcel in)
    {
        ARSALPrint.d(TAG,"ARDiscoveryDeviceBLEService");

        this.bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        this.signal = in.readInt();
    }

    @Override
    public boolean equals(Object other) 
    {
        ARSALPrint.d(TAG, "equals");

        boolean isEqual = true;


        if ( (other == null) || !(other instanceof ARDiscoveryDeviceBLEService) )
        {
            isEqual = false;
        }
        else if (other == this)
        {
            isEqual = true;
        }
        else
        {
            /* check */
            ARDiscoveryDeviceBLEService otherDevice = (ARDiscoveryDeviceBLEService) other;

            if (!this.bluetoothDevice.getAddress().equals(otherDevice.bluetoothDevice.getAddress()))
            {
                isEqual = false;
            }
        }

        return isEqual;
    }

    public BluetoothDevice getBluetoothDevice ()
    {
        return bluetoothDevice;
    }

    public void setBluetoothDevice (BluetoothDevice bluetoothDevice)
    {
        this.bluetoothDevice = bluetoothDevice;

    }

    public int getSignal()
    {
        return signal;
    }

    public void setSignal(int signal)
    {
        this.signal = signal;
    }


    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {    
        dest.writeParcelable( this.bluetoothDevice, flags);
        dest.writeInt(this.signal);
    }

};

