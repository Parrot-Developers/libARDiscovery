/*
 * DroneControlService
 *
 *  Created on: May 5, 2011
 *      Author: Dmytro Baryskyy
 */

package com.parrot.arsdk.ardiscovery;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import com.parrot.arsdk.arsal.ARSALPrint;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Formatter;
import android.util.Pair;

public class ARDiscoveryService extends Service
{
	private static String TAG = "ARDiscoveryService";
	
	public enum eARDISCOVERY_SERVICE_EVENT_STATUS
	{
		ARDISCOVERY_SERVICE_EVENT_STATUS_ADD, 
		ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED,
		ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED 
	}
	
	/**
	 * Constant for devices services list updates notification
	 * userInfo is a NSDictionnary with the following content:
	 *  - key   : kARDiscoveryManagerServicesList
	 *  - value : NSArray of NSNetService
	 */
	public static final String kARDiscoveryManagerNotificationServicesDevicesListUpdated = "kARDiscoveryManagerNotificationServicesDevicesListUpdated";
	public static final String kARDiscoveryManagerService = "kARDiscoveryManagerService";
	public static final String kARDiscoveryManagerServiceStatus = "kARDiscoveryManagerServiceStatus";
	
	
	/**
	 * Constant for controller services list updates notification
	 * userInfo is a NSDictionnary with the following content:
	 *  - key   : kARDiscoveryManagerServicesList
	 *  - value : NSArray of NSNetService
	 */
	public static final String kARDiscoveryManagerNotificationServicesControllersListUpdated = "kARDiscoveryManagerNotificationServicesControllersListUpdated";
	
	/**
	 * Constant for publication notifications
	 * userInfo is a NSDictionnary with the following content:
	 *  - key   : kARDiscoveryManagerServiceName
	 *  - value : NSString with the name of the published service
	 *            or @"" if no service is published
	 */
	public static final String kARDiscoveryManagerNotificationServicePublished = "kARDiscoveryManagerNotificationServicePublished";
	public static final String kARDiscoveryManagerServiceName = "kARDiscoveryManagerServiceName";

	/**
	 * Constant for service resolution notifications
	 * userInfo is a NSDictionnary with the following content:
	 *  - key   : kARDiscoveryManagerServiceResolved
	 *  - value : NSNetService which was resolved
	 *  - key   : kARDiscoveryManagerServiceIP
	 *  - value : NSString with the resolved IP of the service
	 */
	public static final String kARDiscoveryManagerNotificationServiceResolved = "kARDiscoveryManagerNotificationServiceResolved";
	public static final String kARDiscoveryManagerServiceResolved = "kARDiscoveryManagerServiceResolved";
	public static final String kARDiscoveryManagerServiceIP = "kARDiscoveryManagerServiceIP";
	
	public static final String ARDRONE_SERVICE_TYPE = "_arsdk-mk3._udp.local.";
	//public static final String ARDRONE_SERVICE_NAME = "AR.Drone";
	
	private HashMap<String, Intent> intentCache;
	
	private JmDNS mDNSManager;
	private ServiceListener mDNSListener;
	private AsyncTask<Object, Object, Object> jmdnsCreatorAsyncTask;
	
	private String hostIp;
	private InetAddress hostAddress;
	
	private ArrayList<ServiceEvent> deviceServicesArray;
	
	private final IBinder binder = new LocalBinder();
	
	@Override
	public IBinder onBind(Intent intent)
	{
		// TODO Auto-generated method stub
		
		ARSALPrint.d(TAG,"onBind");
		connect();
		return binder;
	}  
	
	@Override
	public void onCreate() 
	{
		ARSALPrint.d(TAG,"onCreate");
		
		/* get the host address */
		WifiManager wifi =   (WifiManager)getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
		if(wifi != null)
		{
			try {
				hostIp = Formatter.formatIpAddress(wifi.getConnectionInfo().getIpAddress());
				hostAddress = InetAddress.getByName(hostIp);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			ARSALPrint.d(TAG,"hostIp: " + hostIp);
			ARSALPrint.d(TAG,"hostAddress: " + hostAddress);
		}
		
		
		jmdnsCreatorAsyncTask = new JmdnsCreatorAsyncTask(); 
		deviceServicesArray = new ArrayList<ServiceEvent>();
		initIntents();
	}

	@Override
	public void onDestroy()
    {
        super.onDestroy();
        
        ARSALPrint.d(TAG,"onDestroy");
        
        if (mDNSManager != null)
        {
            if (mDNSListener != null)
            {
            	ARSALPrint.d(TAG,"removeServiceListener");
            	mDNSManager.removeServiceListener(ARDRONE_SERVICE_TYPE, mDNSListener);
            	mDNSListener = null;
            }
            
            try
            {
            	ARSALPrint.d(TAG,"mDNSManager.close");
            	mDNSManager.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mDNSManager = null;
        }
    }

	protected void connect()
	{
		ARSALPrint.d(TAG,"connect");
		jmdnsCreatorAsyncTask.execute();
	}
	
	public class LocalBinder extends Binder
	{
		public ARDiscoveryService getService() 
		{
			ARSALPrint.d(TAG,"getService");
			return ARDiscoveryService.this;
		}
	}
	
	private void initIntents()
	{
		ARSALPrint.d(TAG,"initIntents");
		
		intentCache = new HashMap<String, Intent>();
		intentCache.put(kARDiscoveryManagerNotificationServicesDevicesListUpdated, new Intent(kARDiscoveryManagerNotificationServicesDevicesListUpdated));
		intentCache.put(kARDiscoveryManagerNotificationServiceResolved, new Intent(kARDiscoveryManagerNotificationServiceResolved));
	}
	
	private void notificationServiceDeviceAdd( ServiceEvent serviceEvent )
	{
		
		ARSALPrint.d(TAG,"notificationServiceDeviceAdd");
		
		/* add the service in the array*/
		deviceServicesArray.add( serviceEvent );
		
		/* broadcast the service add*/
		Intent intent = intentCache.get(kARDiscoveryManagerNotificationServicesDevicesListUpdated);
		
		intent.putExtra( kARDiscoveryManagerService, (Serializable) serviceEvent);
		intent.putExtra( kARDiscoveryManagerServiceStatus, (Serializable) eARDISCOVERY_SERVICE_EVENT_STATUS.ARDISCOVERY_SERVICE_EVENT_STATUS_ADD );
		
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

	}
	
	private void notificationServicesDevicesResolved( ServiceEvent serviceEvent )
	{
		ARSALPrint.d(TAG,"notificationServicesDevicesResolved");
		
		/* check if the service is known */
		Boolean known = false;
		for(ServiceEvent s : deviceServicesArray)
		{
			if( s.getName().equals(serviceEvent.getName()) )
			{
				known = true;
			}
		}
		/* add the service if it not known yet*/
		if(!known)
		{
			ARSALPrint.d(TAG,"service Resolved not know : "+ serviceEvent);
			notificationServiceDeviceAdd(serviceEvent);
		}
		
		
		/* broadcast the service resolved*/
		Intent intent = intentCache.get(kARDiscoveryManagerNotificationServiceResolved);
		intent.putExtra( kARDiscoveryManagerServiceResolved, (Serializable) serviceEvent);
		
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
	}
	
	private void notificationServiceDeviceRemoved( ServiceEvent serviceEvent )
	{
		ARSALPrint.d(TAG,"notificationServiceDeviceRemoved");
		
		ServiceEvent serviceEventToremoved = null;
		
		/* look for the service removed in the array */
		for(ServiceEvent s : deviceServicesArray)
		{
			if( s.getName().equals(serviceEvent.getName()) )
			{
				ARSALPrint.d(TAG,"found");
				serviceEventToremoved = s;
			}
		}
		
		if(serviceEventToremoved != null)
		{
			/* send intent */
			Intent intent = intentCache.get(kARDiscoveryManagerNotificationServicesDevicesListUpdated);
			intent.putExtra( kARDiscoveryManagerService, (Serializable) serviceEventToremoved);
			intent.putExtra( kARDiscoveryManagerServiceStatus, (Serializable) eARDISCOVERY_SERVICE_EVENT_STATUS.ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED );
			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
			
			/* remove from the ServicesArray */
			Boolean res = deviceServicesArray.remove(serviceEventToremoved);
			if(res == false)
			{
				ARSALPrint.e(TAG, "remove error");
			}
			
			ARSALPrint.d(TAG,"ok");
		}
		else
		{
			ARSALPrint.w(TAG, "service: "+ serviceEvent.getInfo().getName() + " not known");
		}

	}
	
	/**
	 * Try to resolve the given ServiceEvent
	 * Resolution is queued until all previous resolutions
	 * are complete, or failed
	 */
	public void resolveService(ServiceEvent service)
	{
		ARSALPrint.d(TAG, "resolveService");

		mDNSManager.requestServiceInfo(service.getType(), service.getName());
	}
	
	public String getServiceIP(ServiceEvent service)
	{
		String serviceIP = null;
		
		ServiceInfo info = service.getDNS().getServiceInfo(service.getType(), service.getName());
		if( (info != null) && (info.getInet4Addresses().length > 0) ){
			serviceIP = info.getInet4Addresses()[0].getHostAddress();
		}
		return serviceIP;
	}
	
	public ArrayList<ServiceEvent> getDeviceServicesArray()
	{
		ARSALPrint.d(TAG,"getDeviceServicesArray: " + deviceServicesArray);
		return deviceServicesArray;
	}
	
	// TODO: only one ...
	private class JmdnsCreatorAsyncTask extends AsyncTask<Object, Object, Object> { 
		
		@Override
		protected Object doInBackground(Object... params) 
		{
			
			try 
			{
				if(hostAddress != null)
				{
					mDNSManager = JmDNS.create(hostAddress);
				}
				else
				{
					mDNSManager = JmDNS.create();
				}
					        	
				ARSALPrint.d(TAG,"JmDNS.createed");
				
				mDNSListener = new ServiceListener() 
				{
					
					@Override
					public void serviceAdded(ServiceEvent event) 
					{
						// Required to force serviceResolved to be called again (after the first search)
				    	//mDNSManager.requestServiceInfo(event.getType(), event.getName(), true);
				    	ARSALPrint.d(TAG,"Service Added: " + event.getName());
				    	
				    	Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS> dataProgress = new Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS>(event,eARDISCOVERY_SERVICE_EVENT_STATUS.ARDISCOVERY_SERVICE_EVENT_STATUS_ADD);
				    	publishProgress(dataProgress);
					}
				
					@Override
					public void serviceRemoved(ServiceEvent event) 
					{
						ARSALPrint.d(TAG,"Service removed: " + event.getName());
						
						Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS> dataProgress = new Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS>(event,eARDISCOVERY_SERVICE_EVENT_STATUS.ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED);
				    	publishProgress(dataProgress);
					}
				
					@Override
					public void serviceResolved(ServiceEvent event) 
					{
						ARSALPrint.d(TAG, "Service resolved: " + event.getName());
						
						Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS> dataProgress = new Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS>(event,eARDISCOVERY_SERVICE_EVENT_STATUS.ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED);
						publishProgress(dataProgress);
					}
				};
				
	        }
			catch (IOException e) 
			{
	            e.printStackTrace();
	            return null;
	        }
			
			return null;
		}
		
		protected void onProgressUpdate(Object... progress) 
		{
			ARSALPrint.d(TAG,"onProgressUpdate");
			
			Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS> dataProgress = (Pair<ServiceEvent,eARDISCOVERY_SERVICE_EVENT_STATUS>) progress[0];
			
			switch(dataProgress.second)
			{
			
				case ARDISCOVERY_SERVICE_EVENT_STATUS_ADD :
					ARSALPrint.d(TAG,"ARDISCOVERY_SERVICE_EVENT_STATUS_ADD");
					notificationServiceDeviceAdd( dataProgress.first);
					break;
					
				case ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED :	
					ARSALPrint.d(TAG,"ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED");
					notificationServicesDevicesResolved( dataProgress.first);
					break;
					
				case ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED :
					ARSALPrint.d(TAG,"ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED");
					notificationServiceDeviceRemoved( dataProgress.first);
					break;
					
				default:
					ARSALPrint.d(TAG, "error service event status " + dataProgress.second +" not known");
					break;
			}
			
	    }
		
		protected void onPostExecute(Object result)
		{
			ARSALPrint.d(TAG,"addServiceListener: ARDRONE_SERVICE_TYPE=" + ARDRONE_SERVICE_TYPE);
			mDNSManager.addServiceListener(ARDRONE_SERVICE_TYPE, mDNSListener);
		}
		
	}
	
};

