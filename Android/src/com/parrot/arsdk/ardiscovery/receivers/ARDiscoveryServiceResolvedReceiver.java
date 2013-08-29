package com.parrot.arsdk.ardiscovery.receivers;

import javax.jmdns.ServiceEvent;

import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.arsal.ARSALPrint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;



public class ARDiscoveryServiceResolvedReceiver extends BroadcastReceiver {

	private static String TAG = "ARDiscoveryServiceResolvedReceiverDelegate";
	
	private ARDiscoveryServiceResolvedReceiverDelegate delegate;
			
	
	public ARDiscoveryServiceResolvedReceiver(ARDiscoveryServiceResolvedReceiverDelegate delegate)
	{
		ARSALPrint.d(TAG,"ARDiscoveryServiceResolvedReceiver constructor");
		this.delegate = delegate;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		ARSALPrint.d(TAG,"onReceive");
		
		ServiceEvent service = (ServiceEvent) intent.getSerializableExtra(ARDiscoveryService.kARDiscoveryManagerServiceResolved);
		if (delegate != null) {
			delegate.onServiceResolved(service);
		}
	}

}
