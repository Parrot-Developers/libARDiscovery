
package com.parrot.arsdk.ardiscovery;

import android.os.Parcel;
import android.os.Parcelable;

import com.parrot.arsdk.arsal.ARSALPrint;

public class ARDiscoveryDeviceNetService implements  Parcelable
{
    /**
     * 
     */

    private static String TAG = "ARDiscoveryDeviceNetService";
    
    private String name;
    private String type;
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
        type = "";
        ip = "";
    }
    
    public ARDiscoveryDeviceNetService (String name, String type, String ip)
    {
        this.name = name;
        this.type = type;
        this.ip = ip;
    }
    
    /* Parcelling part */
    public ARDiscoveryDeviceNetService(Parcel in)
    {
        this.name = in.readString();
        this.type = in.readString();
        this.ip = in.readString();
    }
    
    @Override
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

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
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
        dest.writeString(this.type);
        dest.writeString(this.ip);
    }

};

