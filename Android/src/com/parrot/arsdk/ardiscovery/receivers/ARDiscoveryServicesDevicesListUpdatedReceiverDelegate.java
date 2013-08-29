package com.parrot.arsdk.ardiscovery.receivers;

import javax.jmdns.ServiceEvent;

import com.parrot.arsdk.ardiscovery.ARDiscoveryService.eARDISCOVERY_SERVICE_EVENT_STATUS;

public interface ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {

	public void onServicesDevicesListUpdated(ServiceEvent deviceService, eARDISCOVERY_SERVICE_EVENT_STATUS status);
}
