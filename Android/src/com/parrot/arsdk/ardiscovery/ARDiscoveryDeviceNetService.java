
package com.parrot.arsdk.ardiscovery;

import android.os.Parcel;
import android.os.Parcelable;

import com.parrot.arsdk.arsal.ARSALPrint;

public class ARDiscoveryDeviceNetService implements  Parcelable
{
	/**
	 * 
	 */

	private static String TAG = "ARDiscoveryDevice";
	
	private String name;
	private String ip;
	
	public static final Parcelable.Creator<ARDiscoveryDeviceNetService> CREATOR = new Parcelable.Creator<ARDiscoveryDeviceNetService>()
	{
	    @Override
	    public ARDiscoveryDeviceNetService createFromParcel(Parcel source)
	    {
	        return new ARDiscoveryDeviceNetService(source);
	    }

	    @Override
	    public ARDiscoveryDeviceNetService[] newArray(int size)
	    {
		return new ARDiscoveryDeviceNetService[size];
	    }
	};
	
	public ARDiscoveryDeviceNetService ()
	{
		name = "";
		ip = "";
	}
	
	public ARDiscoveryDeviceNetService (String name, String ip)
	{
		this.name = name;
		this.ip = ip;
	}
	
	/* Parcelling part */
    public ARDiscoveryDeviceNetService(Parcel in)
    {
        
        this.name = in.readString();
        this.ip = in.readString();

    }
    
	
	public boolean equals(Object other) 
    {
		boolean isEqual = true;
	        
        if ( (other == null) || !(other instanceof ARDiscoveryDeviceNetService) )
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
        	ARDiscoveryDeviceNetService otherDevice = (ARDiscoveryDeviceNetService) other;
            if (!this.name.equals(otherDevice))
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
	
	public String getIp()
	{
		return ip;
	}
	
	public void setIp (String ip)
	{
		this.ip = ip;
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
		dest.writeString(this.ip);
	}

};

