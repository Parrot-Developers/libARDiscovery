package com.parrot.arsdk.ardiscovery.receivers;

import javax.jmdns.ServiceEvent;

public interface ARDiscoveryServiceResolvedReceiverDelegate{

	public void onServiceResolved(ServiceEvent service);
}
