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
    private int port;
    
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
        port = 0;
    }
    
    public ARDiscoveryDeviceNetService (String name, String type, String ip, int port)
    {
        this.name = name;
        this.type = type;
        this.ip = ip;
        this.port = port;
    }
    
    /* Parcelling part */
    public ARDiscoveryDeviceNetService(Parcel in)
    {
        this.name = in.readString();
        this.type = in.readString();
        this.ip = in.readString();
        this.port = in.readInt();
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
    
    public int getPort()
    {
        return port;
    }
    
    public void setport (int port)
    {
        this.port = port;
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
        dest.writeInt(this.port);
    }

};

