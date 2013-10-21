package com.parrot.arsdk.ardiscovery.receivers;

import com.parrot.arsdk.arsal.ARSALPrint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class ARDiscoveryServicesDevicesListUpdatedReceiver extends BroadcastReceiver
{

	private static String TAG = "ARDiscoveryServicesDevicesListUpdatedReceiver";
	
	private ARDiscoveryServicesDevicesListUpdatedReceiverDelegate delegate;
			
	
	public ARDiscoveryServicesDevicesListUpdatedReceiver(ARDiscoveryServicesDevicesListUpdatedReceiverDelegate delegate)
	{
		ARSALPrint.d(TAG,"ARDiscoveryServicesDevicesListUpdatedReceiver constructor");
		
		this.delegate = delegate;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		ARSALPrint.d(TAG,"onReceive");
		
		if (delegate != null)
		{
			delegate.onServicesDevicesListUpdated();
		}
	}

}
