package com.parrot.arsdk.ardiscovery.receivers;

import javax.jmdns.ServiceEvent;

import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS;
import com.parrot.arsdk.arsal.ARSALPrint;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class ARDiscoveryServicesDevicesListUpdatedReceiver extends BroadcastReceiver {

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
		
		ServiceEvent deviceService = (ServiceEvent) intent.getSerializableExtra(ARDiscoveryService.kARDiscoveryManagerService);
		eARDISCOVERY_SERVICE_EVENT_STATUS status = (eARDISCOVERY_SERVICE_EVENT_STATUS) intent.getSerializableExtra(ARDiscoveryService.kARDiscoveryManagerServiceStatus);
		if (delegate != null) {
			delegate.onServicesDevicesListUpdated(deviceService, status);
		}
	}

}
