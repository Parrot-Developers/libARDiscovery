/*
 * DroneControlService
 *
 *  Created on: May 5, 2011
 *  Author:
 */

package com.parrot.arsdk.ardiscovery;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import com.parrot.arsdk.arsal.ARSALPrint;

public class ARDiscoveryDeviceService implements Parcelable
{
	/**
	 * 
	 */

	private static String TAG = "ARDiscoveryDevice";
	
	private String name;
	private Object device; /* can by ARDiscoveryDeviceNetService or BluetoothDevice */
	
	public static final Parcelable.Creator<ARDiscoveryDeviceService> CREATOR = new Parcelable.Creator<ARDiscoveryDeviceService>()
	{
	    @Override
	    public ARDiscoveryDeviceService createFromParcel(Parcel source)
	    {
	        return new ARDiscoveryDeviceService(source);
	    }

	    @Override
	    public ARDiscoveryDeviceService[] newArray(int size)
	    {
		return new ARDiscoveryDeviceService[size];
	    }
	};
	
	public ARDiscoveryDeviceService ()
	{
		name = "";
		setDevice (null);
	}
	
	public ARDiscoveryDeviceService (String name, Object device)
	{
		this.name = name;
		this.setDevice(device);
	}
	
	// Parcelling part
    public ARDiscoveryDeviceService(Parcel in)
    {
        
        this.name = in.readString();
        eARDISCOVERY_DEVICE_SERVICE_TYPE type = in.readParcelable(eARDISCOVERY_DEVICE_SERVICE_TYPE.class.getClassLoader());
        
        switch(type)
        {
        	case ARDISCOVERY_DEVICE_SERVICE_TYPE_NET:
        		this.device = in.readParcelable(ARDiscoveryDeviceNetService.class.getClassLoader());
        		
        		break;
        		
        	case ARDISCOVERY_DEVICE_SERVICE_TYPE_BLE:
        		this.device = in.readParcelable(BluetoothDevice.class.getClassLoader());
        		break;
        		
        	default:
        		this.device = null;
        		break;
        }

    }
	
	public boolean equals(Object other) 
    {
		boolean isEqual = true;
	        
        if ( (other == null) || !(other instanceof ARDiscoveryDeviceService) )
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
        	ARDiscoveryDeviceService otherDevice = (ARDiscoveryDeviceService) other;
            if (!this.name.equals(otherDevice.name))
            {
                isEqual = false;
            }
        }
        
        return isEqual;
    }
	
	public String getName ()
	{
		return name;
	}
	
	public void setName (String name)
	{
		this.name = name;
	}

	public Object getDevice ()
	{
		return device;
	}

	public void setDevice (Object device)
	{
		this.device = device;
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{	
		dest.writeString(this.name);
		
		eARDISCOVERY_DEVICE_SERVICE_TYPE type = null;
		
		if (device instanceof ARDiscoveryDeviceNetService)
		{
			type = eARDISCOVERY_DEVICE_SERVICE_TYPE.ARDISCOVERY_DEVICE_SERVICE_TYPE_NET;
		}
		else if (device instanceof BluetoothDevice)
		{
			type = eARDISCOVERY_DEVICE_SERVICE_TYPE.ARDISCOVERY_DEVICE_SERVICE_TYPE_BLE;
		}
		
		dest.writeParcelable(type, flags);

		
		dest.writeParcelable((Parcelable) this.device, flags);
	}
	
	private enum eARDISCOVERY_DEVICE_SERVICE_TYPE implements Parcelable
	{
		ARDISCOVERY_DEVICE_SERVICE_TYPE_NET, 
		ARDISCOVERY_DEVICE_SERVICE_TYPE_BLE;
		
		@Override
	    public int describeContents()
		{
	        return 0;
	    }

	    @Override
	    public void writeToParcel(final Parcel dest, final int flags)
	    {
	        dest.writeInt(ordinal());
	    }

	    public static final Creator<eARDISCOVERY_DEVICE_SERVICE_TYPE> CREATOR = new Creator<eARDISCOVERY_DEVICE_SERVICE_TYPE>()
	    {
	        @Override
	        public eARDISCOVERY_DEVICE_SERVICE_TYPE createFromParcel(final Parcel source)
	        {
	            return eARDISCOVERY_DEVICE_SERVICE_TYPE.values()[source.readInt()];
	        }

	        @Override
	        public eARDISCOVERY_DEVICE_SERVICE_TYPE[] newArray(final int size)
	        {
	            return new eARDISCOVERY_DEVICE_SERVICE_TYPE[size];
	        }
	    };
	}
};

