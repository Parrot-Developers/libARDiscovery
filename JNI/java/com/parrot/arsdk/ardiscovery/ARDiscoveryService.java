/*
 * DroneControlService
 *
 *  Created on: May 5, 2011
 *  Author:
 */

package com.parrot.arsdk.ardiscovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import com.parrot.arsdk.arsal.ARSALPrint;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
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
	 */
	public static final String kARDiscoveryServiceNotificationServicesDevicesListUpdated = "kARDiscoveryServiceNotificationServicesDevicesListUpdated";
	
	private static final String ARDISCOVERY_ARDRONE_SERVICE_TYPE = "_arsdk-mk3._udp.local.";
	
	private HashMap<String, Intent> intentCache;
	
	private JmDNS mDNSManager;
	private ServiceListener mDNSListener;
	
	private AsyncTask<Object, Object, Object> jmdnsCreatorAsyncTask;
	private Boolean inWifiConnection = false;
	
	private String hostIp;
	private InetAddress hostAddress;
	static private InetAddress nullAddress;
	
	private IntentFilter networkStateChangedFilter;
	private BroadcastReceiver networkStateIntentReceiver;
	
	private HashMap<String, ARDiscoveryDeviceService> netDeviceServicesHmap;
	
	/* BLE */
	private static final int ARDISCOVERY_BLE_VENDOR_ID = 0x0043;
	private static final int ARDISCOVERY_BLE_PRODUCT_ID = 0x2000;
	private static final int ARDISCOVERY_BLE_VERSION_ID = 0x0001;

	private Boolean bleIsAvalaible;
	private BluetoothAdapter bluetoothAdapter;
	private BLEScanner bleScanner;
	private HashMap<String, ARDiscoveryDeviceService> bleDeviceServicesHmap;
	private Object leScanCallback;/*< Device scan callback. (BluetoothAdapter.LeScanCallback) */
	
	private final IBinder binder = new LocalBinder();
	
	@Override
	public IBinder onBind(Intent intent)
	{
		ARSALPrint.d(TAG,"onBind");
		
		manageConnections();

		return binder;
	}  
	
	@Override
	public void onCreate() 
	{
		ARSALPrint.d(TAG,"onCreate");
		
		try 
		{
			nullAddress = InetAddress.getByName("0.0.0.0");
		} 
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		hostAddress = nullAddress;
		
		netDeviceServicesHmap = new HashMap<String, ARDiscoveryDeviceService> ();
		bleDeviceServicesHmap = new HashMap<String, ARDiscoveryDeviceService> ();
		initIntents();
		
		networkStateChangedFilter = new IntentFilter();
		networkStateChangedFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		networkStateChangedFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		networkStateIntentReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				ARSALPrint.d(TAG,"BroadcastReceiver onReceive");
				
				if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
				{
					/* if the wifi is connected get its hostAddress */
					ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
					NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					if (wifiInfo.isConnected())
					{
						mdnsConnect();
					}
					else
					{
						mdnsDisconnect();
					}
				}
				
				if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED))
				{
					
					ARSALPrint.d(TAG,"ACTION_STATE_CHANGED");
					
					if (bleIsAvalaible)
					{
						int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
						
						switch (state)
						{
							case BluetoothAdapter.STATE_ON:
								bleConnect();
								break;
							case BluetoothAdapter.STATE_TURNING_OFF:
								bleDisconnect();
								break;
						}
					}
				}
			}
		};
		
		bleIsAvalaible = false;
		getBLEAvailability();

		if (bleIsAvalaible)
		{
			initBLE();
		}
		
		registerReceiver(networkStateIntentReceiver, networkStateChangedFilter);
		
	}

	@Override
	public void onDestroy()
    {
        super.onDestroy();
        
        ARSALPrint.d(TAG,"onDestroy");
        
        unregisterReceiver(networkStateIntentReceiver);
        
        mdnsDisconnect ();
        bleDisconnect();
    }
	
	public void mdnsDestroy()
    {
        ARSALPrint.d(TAG,"mdnsDestroy");
        
        /* if jmnds is running */
        if (mDNSManager != null)
        {
            if (mDNSListener != null)
            {
            	ARSALPrint.d(TAG, "removeServiceListener");
            	mDNSManager.removeServiceListener(ARDISCOVERY_ARDRONE_SERVICE_TYPE, mDNSListener);
            	mDNSListener = null;
            }
            
            try
            {
            	mDNSManager.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            mDNSManager = null;
        }
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
		intentCache.put(kARDiscoveryServiceNotificationServicesDevicesListUpdated, new Intent(kARDiscoveryServiceNotificationServicesDevicesListUpdated));
	}
	
	@Override
	public boolean onUnbind(Intent intent)
	{
		ARSALPrint.d(TAG,"onUnbind");
        
        return true; /* ensures onRebind is called */
	}
	
	@Override
	public void onRebind(Intent intent)
	{
		ARSALPrint.d(TAG,"onRebind");
	    
		manageConnections();
	}
	
	private void getBLEAvailability()
	{
		/* check whether BLE is supported on the device  */
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
		{
			ARSALPrint.d(TAG,"BLE Is NOT Avalaible");
			bleIsAvalaible = false;
		}
		else
		{
			ARSALPrint.d(TAG,"BLE Is Avalaible");
			bleIsAvalaible = true;
		}
	}
	
	@TargetApi(18)
	private void initBLE()
	{
		ARSALPrint.d(TAG,"initBLE");
		
		/* Initializes Bluetooth adapter. */
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();
	
		bleScanner = new BLEScanner();
		
		leScanCallback = new BluetoothAdapter.LeScanCallback()
	 	{
	 	    @Override
	 	    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
	 	    {
	 	    	ARSALPrint.d(TAG,"onLeScan");
	 	    	
	 	    	bleScanner.bleCallback(device, scanRecord);
			}
	 	};
	}
	
	public void manageConnections()
	{
		ARSALPrint.d(TAG,"manageConnections");
		
		/* if the wifi is connected get its hostAddress */
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (mWifi.isConnected())
		{
			mdnsConnect();
		}
		else
		{
			mdnsDisconnect();
		}

		if ( (bleIsAvalaible == true) && bluetoothAdapter.isEnabled())
		{
			bleConnect();
		}
		else
		{
			bleDisconnect();
		}
	} 
	
	private void mdnsConnect()
	{
		ARSALPrint.d(TAG,"connect");
		
		/* if jmdns is not running yet */
		if (mDNSManager == null)
		{
			/* get the host address */
			WifiManager wifi =   (WifiManager)getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
		
			if(wifi != null)
			{
				try
				{
					hostIp = Formatter.formatIpAddress(wifi.getConnectionInfo().getIpAddress());
					hostAddress = InetAddress.getByName(hostIp);
				}
				catch (UnknownHostException e)
				{
					e.printStackTrace();
				}
				
				ARSALPrint.d(TAG,"hostIp: " + hostIp);
				ARSALPrint.d(TAG,"hostAddress: " + hostAddress);
				
			}
				
			if (! hostAddress.equals (nullAddress) && inWifiConnection == false)
			{
				inWifiConnection = true;
				
				jmdnsCreatorAsyncTask = new JmdnsCreatorAsyncTask();
				jmdnsCreatorAsyncTask.execute();
			}
		}
	}
	
	private void mdnsDisconnect()
	{
		ARSALPrint.d(TAG,"disconnect");
		
		/* reset */
		hostAddress = nullAddress;
		mdnsDestroy ();
		
		ArrayList<ARDiscoveryDeviceService> netDeviceServicesArray = new ArrayList<ARDiscoveryDeviceService> (netDeviceServicesHmap.values());
		
		/* remove all net services */
		for ( ARDiscoveryDeviceService s : netDeviceServicesArray)
		{
			notificationNetServiceDeviceRemoved (s);
		}
	}
	
	private void notificationNetServiceDeviceAdd( ServiceEvent serviceEvent )
	{
		ARSALPrint.d(TAG,"notificationServiceDeviceAdd");
		
		/* get ip address */
		String ip = getServiceIP(serviceEvent);
		
		if (ip != null)
		{
			/* new ARDiscoveryDeviceNetService */
			ARDiscoveryDeviceNetService deviceNetService = new ARDiscoveryDeviceNetService(serviceEvent.getName(), ip);
			
			/* add the service in the array*/
			ARDiscoveryDeviceService deviceService = new ARDiscoveryDeviceService (serviceEvent.getName(), deviceNetService);
			netDeviceServicesHmap.put(deviceService.getName(), deviceService);
			
			/* broadcast the new deviceServiceList */
			broadcastDeviceServiceArrayUpdated ();
		}
	}
	
	private void notificationNetServicesDevicesResolved( ServiceEvent serviceEvent )
	{
		ARSALPrint.d(TAG,"notificationServicesDevicesResolved");
		
		/* check if the service is known */
		Boolean known = netDeviceServicesHmap.containsKey(serviceEvent.getName());
		
		/* add the service if it not known yet*/
		if(!known)
		{
			ARSALPrint.d(TAG,"service Resolved not know : "+ serviceEvent);
			notificationNetServiceDeviceAdd(serviceEvent);
		}

	}
	
	private void notificationNetServiceDeviceRemoved( ServiceEvent serviceEvent )
	{
		ARSALPrint.d(TAG,"notificationServiceDeviceRemoved");
		
		/* remove from the deviceServicesHmap */
		ARDiscoveryDeviceService deviceServiceRemoved = netDeviceServicesHmap.remove(serviceEvent.getName());

		if(deviceServiceRemoved != null)
		{
			/* broadcast the new deviceServiceList */
			broadcastDeviceServiceArrayUpdated ();
		}
		else
		{
			ARSALPrint.w(TAG, "service: "+ serviceEvent.getInfo().getName() + " not known");
		}
	}
	
	private void notificationNetServiceDeviceRemoved( ARDiscoveryDeviceService deviceService )
	{
		ARSALPrint.d(TAG,"notificationServiceDeviceRemoved");
		
		/* remove from the deviceServicesHmap */
		ARDiscoveryDeviceService deviceServiceRemoved = netDeviceServicesHmap.remove(deviceService.getName());

		if(deviceServiceRemoved != null)
		{
			/* broadcast the new deviceServiceList */
			broadcastDeviceServiceArrayUpdated ();
		}
		else
		{
			ARSALPrint.w(TAG, "service: "+ deviceService.getName() + " not known");
		}
	}
	
	
	/* broadcast the deviceServicelist Updated */
	private void broadcastDeviceServiceArrayUpdated ()
	{	
		ARSALPrint.d(TAG,"broadcastDeviceServiceArrayUpdated");
		/* broadcast the service add*/
		Intent intent = intentCache.get(kARDiscoveryServiceNotificationServicesDevicesListUpdated);
		
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
	}
	
	private String getServiceIP(ServiceEvent serviceEvent)
	{
		ARSALPrint.d(TAG,"getServiceIP serviceEvent: " + serviceEvent);
		
		String serviceIP = null;
		
		ServiceInfo info = serviceEvent.getDNS().getServiceInfo(serviceEvent.getType(), serviceEvent.getName());
		if( (info != null) && (info.getInet4Addresses().length > 0) )
		{
			serviceIP = info.getInet4Addresses()[0].getHostAddress();
		}
		
		return serviceIP;
	}
	
	public ArrayList<ARDiscoveryDeviceService> getDeviceServicesArray()
	{
		ArrayList<ARDiscoveryDeviceService> deviceServicesArray = new ArrayList<ARDiscoveryDeviceService> ( netDeviceServicesHmap.values()  ); 
		deviceServicesArray.addAll(bleDeviceServicesHmap.values());
		
		ARSALPrint.d(TAG,"getDeviceServicesArray: " + deviceServicesArray);
		return new ArrayList<ARDiscoveryDeviceService> (deviceServicesArray);
	}
	
	// TODO: only one ...
	private class JmdnsCreatorAsyncTask extends AsyncTask<Object, Object, Object> { 
		
		@Override
		protected Object doInBackground(Object... params) 
		{
			ARSALPrint.d(TAG, "doInBackground");
			
			try 
			{
				if ( (hostAddress != null) && (! hostAddress.equals( nullAddress )) )
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
			
			inWifiConnection = false;
			
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
					notificationNetServiceDeviceAdd( dataProgress.first);
					break;
					
				case ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED :	
					ARSALPrint.d(TAG,"ARDISCOVERY_SERVICE_EVENT_STATUS_RESOLVED");
					notificationNetServicesDevicesResolved( dataProgress.first);
					break;
					
				case ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED :
					ARSALPrint.d(TAG,"ARDISCOVERY_SERVICE_EVENT_STATUS_REMOVED");
					notificationNetServiceDeviceRemoved( dataProgress.first);
					break;
					
				default:
					ARSALPrint.d(TAG, "error service event status " + dataProgress.second +" not known");
					break;
			}
			
	    }
		
		protected void onPostExecute(Object result)
		{
			ARSALPrint.d(TAG,"addServiceListener: ARDRONE_SERVICE_TYPE=" + ARDISCOVERY_ARDRONE_SERVICE_TYPE);
			mDNSManager.addServiceListener(ARDISCOVERY_ARDRONE_SERVICE_TYPE, mDNSListener);
		}
		
	}
	
	/* BLE */
	private void bleConnect()
	{
		ARSALPrint.d(TAG,"bleConnect");
		
		if (bleIsAvalaible)
		{
			bleScanner.start();
		}
	}
	
	private void bleDisconnect()
	{
		ARSALPrint.d(TAG,"bleDisconnect");
		
		if (bleIsAvalaible)
		{
			bleScanner.stop();
			
			/* remove all BLE services */
			bleDeviceServicesHmap.clear();
			
			/* broadcast the new deviceServiceList */
			broadcastDeviceServiceArrayUpdated ();
		}
	}
	
	
	@TargetApi(18)
	private class BLEScanner
	{
		private static final long ARDISCOVERY_BLE_SCAN_PERIOD = 20000;
		private static final long ARDISCOVERY_BLE_SCAN_DURATION = 10000;
		private boolean isStart;
		private boolean scanning;
		private Handler startBLEHandler;
		private Handler stopBLEHandler;
		private Runnable startScanningRunnable;
		private Runnable stopScanningRunnable;
		private HashMap<String, ARDiscoveryDeviceService> newBLEDeviceServicesHmap;
		
		private static final int ARDISCOVERY_BLE_MANUFACTURER_DATA_LENGTH_OFFSET = 3;
		private static final int ARDISCOVERY_BLE_MANUFACTURER_DATA_ADTYPE_OFFSET = 4;
		private static final int ARDISCOVERY_BLE_MANUFACTURER_DATA_LENGTH_WITH_ADTYPE = 7;
		private static final int ARDISCOVERY_BLE_MANUFACTURER_DATA_ADTYPE = 0xFF; 
		
		public BLEScanner()
		{
			ARSALPrint.d(TAG,"BLEScanningTask constructor");
			
			startBLEHandler = new Handler() ;
			stopBLEHandler = new Handler() ;
			
			startScanningRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                	startScanLeDevice();
                }
            };
			
			stopScanningRunnable = new Runnable()
            {
                @Override
                public void run()
                {
                	periodScanLeDeviceEnd();
                }
            };
		}
		
		public void start()
		{
			ARSALPrint.d(TAG,"BLEScanningTask start");
			
			if (! isStart)
			{
				isStart = true;
				startScanningRunnable.run();
			}

		}
			
		private void startScanLeDevice()
		{
			ARSALPrint.w(TAG,"start scanLeDevice: bluetoothAdapter: " + bluetoothAdapter);

			/* reset newDeviceServicesHmap */
			newBLEDeviceServicesHmap = new HashMap<String, ARDiscoveryDeviceService>();
			
            /* Stops scanning after a pre-defined scan duration. */
        	stopBLEHandler.postDelayed( stopScanningRunnable , ARDISCOVERY_BLE_SCAN_DURATION);

            scanning = true;
            bluetoothAdapter.startLeScan((BluetoothAdapter.LeScanCallback)leScanCallback);
            
            /* restart scanning after a pre-defined scan period. */
            startBLEHandler.postDelayed (startScanningRunnable, ARDISCOVERY_BLE_SCAN_PERIOD);
	    }
		
		public void bleCallback( BluetoothDevice bleService, byte[] scanRecord)
		{
			ARSALPrint.d(TAG,"bleCallback");
			
			if (isParrotBLEDevice (scanRecord))
 	    	{
	    		ARSALPrint.d(TAG,"add ble device name:" + bleService.getName());
	    		
	    		ARDiscoveryDeviceBLEService deviceBLEService = new ARDiscoveryDeviceBLEService(bleService);
	    		
	    		/* add the service in the array*/
				ARDiscoveryDeviceService deviceService = new ARDiscoveryDeviceService (bleService.getName(), deviceBLEService);
				
				newBLEDeviceServicesHmap.put(deviceService.getName(), deviceService);
	    	}
	    }
		
		private boolean isParrotBLEDevice(byte[] scanRecord)
		{
			/* read the scanRecord  to check if it is a PARROT Delos device with the good version */
			
			/* scanRecord :
			* <---------------- 31 oct ------------------> 
			* | AD Struct 1	| AD Struct 2 |  AD Struct n | 
			* |				 \_____________________
			* | length (1 oct)	| data (length otc) |
			* 					|					 \_______________________
			* 					|AD type (n oct) | AD Data ( length - n oct) |
			* 
			* for Delos:
			* AD Struct 1 : (Flags)
			* - length = 0x02
			* - AD Type = 0x01
			* - AD data : 
			* 
			* AD Struct 2 : (manufacturerData)
			* - length = 0x07
			* - AD Type = 0xFF
			* - AD data : | vendorID (2 oct) | productID (2 oct) | versionID (2 oct) |
			*/
			
			ARSALPrint.d(TAG,"isParrotBLEDevice");
			
			boolean res = false;
			
		    final int MASK = 0xFF;
		    
		    /* get the length of the manufacturerData */
		    byte[] data = (byte[]) Arrays.copyOfRange(scanRecord, ARDISCOVERY_BLE_MANUFACTURER_DATA_LENGTH_OFFSET, ARDISCOVERY_BLE_MANUFACTURER_DATA_LENGTH_OFFSET + 1);
		    int manufacturerDataLenght = (MASK & data[0]);
		    
		    /* check if it is the length expected */
		    if (manufacturerDataLenght == ARDISCOVERY_BLE_MANUFACTURER_DATA_LENGTH_WITH_ADTYPE)
		    {
		    	/* get the manufacturerData */
		    	data = (byte[]) Arrays.copyOfRange(scanRecord, ARDISCOVERY_BLE_MANUFACTURER_DATA_ADTYPE_OFFSET , ARDISCOVERY_BLE_MANUFACTURER_DATA_ADTYPE_OFFSET + manufacturerDataLenght);
		    	int adType = (MASK & data[0]);
		    	
		    	/* check if it is the AD Type expected */
		    	if (adType == ARDISCOVERY_BLE_MANUFACTURER_DATA_ADTYPE)
		    	{
			    	int vendorID = (data[1] & MASK) + ((data[2] & MASK) << 8);
			    	int productID = (data[3] & MASK) + ((data[4] & MASK) << 8);
			    	int versionID = (data[5] & MASK) + ((data[6] & MASK) << 8);
			    	
			    	/* check the vendorID, the productID and and the version */
			    	if ((vendorID == ARDISCOVERY_BLE_VENDOR_ID) && (productID == ARDISCOVERY_BLE_PRODUCT_ID) && (versionID >= ARDISCOVERY_BLE_VERSION_ID) )
			    	{
			    		res = true;
			    	}
		    	}
		    }
		    
		    return res;
		}
		
		private void periodScanLeDeviceEnd()
		{
			ARSALPrint.d(TAG,"periodScanLeDeviceEnd");
			notificationBLEServiceDeviceUpDate (newBLEDeviceServicesHmap);
			stopScanLeDevice();
	    }
		
		private void stopScanLeDevice()
		{
			ARSALPrint.d(TAG,"ScanLeDeviceAsyncTask stopLeScan");
            scanning = false;
            bluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback)leScanCallback);
	    }

	    public void stop()
	    {
	    	ARSALPrint.d(TAG,"BLEScanningTask stop");
	    	
	    	bluetoothAdapter.stopLeScan((BluetoothAdapter.LeScanCallback)leScanCallback);
            startBLEHandler.removeCallbacks(startScanningRunnable);
            stopBLEHandler.removeCallbacks(stopScanningRunnable);
            scanning = false;
            isStart = false;
	    }
	    
	    public Boolean IsScanning()
	    {
	    	return scanning;
	    }
	    
	    public Boolean IsStart()
	    {
	    	return isStart;
	    }
		
	};
	
	@TargetApi(18)
	private void notificationBLEServiceDeviceUpDate( HashMap<String, ARDiscoveryDeviceService> newBLEDeviceServicesHmap )
	{
		ARSALPrint.d(TAG,"notificationBLEServiceDeviceAdd");
		
		/* if the BLEDeviceServices List has changed */
		if (bleServicesListHasChanged(newBLEDeviceServicesHmap))
    	{
    		/* get the new BLE Device Services list */
    		bleDeviceServicesHmap = newBLEDeviceServicesHmap;
			
			/* broadcast the new deviceServiceList */
			broadcastDeviceServiceArrayUpdated ();
    	}
	}
	
	private boolean bleServicesListHasChanged ( HashMap<String, ARDiscoveryDeviceService> newBLEDeviceServicesHmap )
	{
		/* check is the list of BLE devices has changed */
		ARSALPrint.d(TAG,"bleServicesListHasChanged");
		
		boolean res = false;
		
		if (bleDeviceServicesHmap.size() != newBLEDeviceServicesHmap.size())
		{
			/* if the number of devices has changed */
			res = true;
		}
		else if (!bleDeviceServicesHmap.keySet().equals(newBLEDeviceServicesHmap.keySet()))
		{
			/* if the names of devices has changed */
			res = true;
		}
		else
		{
			for (ARDiscoveryDeviceService bleDevice : bleDeviceServicesHmap.values())
			{
				/* check from the MAC address */
				if (!newBLEDeviceServicesHmap.containsValue(bleDevice))
				{
					/* if one of the old devices is not present is the new list */
					res = true;
				}
			}
		}
		
		return res;
	}
	
};

