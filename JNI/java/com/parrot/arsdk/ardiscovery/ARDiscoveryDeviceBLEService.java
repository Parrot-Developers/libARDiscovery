
package com.parrot.arsdk.ardiscovery;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import com.parrot.arsdk.arsal.ARSALPrint;

public class ARDiscoveryDeviceBLEService implements Parcelable
{
    /**
     * 
     */

    private static String TAG = "ARDiscoveryDeviceBLEService";
    
    private int test;
    private BluetoothDevice bluetoothDevice;
    
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

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {    
        dest.writeParcelable( this.bluetoothDevice, flags);
    }

};

