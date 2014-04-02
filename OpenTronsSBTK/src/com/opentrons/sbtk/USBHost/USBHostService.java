package com.opentrons.sbtk.USBHost;


import com.opentrons.sbtk.USBHost.FTDriver;

import com.opentrons.sbtk.Support.SBTKService;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

//This is only a bound service at the moment.
@TargetApi(12)
public class USBHostService extends SBTKService {
	private static final String TAG = "BetaBot-USBHost";
	private static final String ACTION_USB_PERMISSION = "com.nwags.BetaBot.USB_PERMISSION";
	private static final int USB_BUFFER_SIZE = 4*1024;

	SharedPreferences settings;
	
	// USB IDs for the TinyG hardware.
	// TODO make them parameters.
	BroadcastReceiver mUsbReceiver = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			
			if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)){
				Log.d(TAG, "UsbManager.ACTION_USB_DEVICE_ATTACHED");
			/*
				if(!mSerial.isConnected()){
					Log.d(TAG,"UsbManager->begin");
					mBaudrate = loadDefaultBaudrate();
					mSerial.begin(mBaudrate);
					loadDefaultSettingValues();
				}
				if(mListener.getStatus()!=AsyncTask.Status.RUNNING)
				{
					mListener.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					// Let everyone know we are connected
					Bundle b = new Bundle();
					b.putBoolean("connection", true);
					Intent i = new Intent(CONNECTION_STATUS);
					i.putExtras(b);
					sendBroadcast(i, null);
					refresh();
				}
				*/
				
				if(!mRunningMainListener){
					Log.d(TAG, "Device attached mainlistener");
					
					mainlistener();
				}
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
				Log.d(TAG, "Device detached");
				mSerial.usbDetached(intent);
				mSerial.end();
				disconnect(); // instead of detachedUi()
			} else if (ACTION_USB_PERMISSION.equals(action)) {
				Log.d(TAG, "Request permission");
				
				if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
					/*
					if(!mSerial.isConnected()){
						Log.d(TAG,"UsbManager->begin");
						mBaudrate = loadDefaultBaudrate();
						mSerial.begin(mBaudrate);
						loadDefaultSettingValues();
					}
					if(mListener.getStatus()!=AsyncTask.Status.RUNNING)
					{
						mListener.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						// Let everyone know we are connected
						Bundle b = new Bundle();
						b.putBoolean("connection", true);
						Intent i = new Intent(CONNECTION_STATUS);
						i.putExtras(b);
						sendBroadcast(i, null);
						refresh();
					}
					*/
					synchronized (this) {
						if(!mSerial.isConnected()) {
							Log.d(TAG, "Reuest permission begin");
							mBaudrate = loadDefaultBaudrate();
							mSerial.begin(mBaudrate);
							loadDefaultSettingValues();
						}
					}
					if(!mRunningMainListener) {
						Log.d(TAG, "Request permission mainlistener");
						mainlistener();
					}
				}else{
					Toast.makeText(USBHostService.this,
							"USB permission denied", Toast.LENGTH_SHORT).show();
					disconnect();
					return;
				}
				/*
				synchronized (this) {
					if(!mSerial.isConnected()) {
						Log.d(TAG, "Reuest permission begin");
						mBaudrate = loadDefaultBaudrate();
						mSerial.begin(mBaudrate);
						loadDefaultSettingValues();
					}
				}
				if(!mRunningMainListener) {
					Log.d(TAG, "Request permission mainlistener");
					mainlistener();
				}
				*/
			}
			
		}
		
	};
	
	
	private UsbDevice deviceFTDI;
	private UsbManager mUsbManager;
	private UsbDeviceConnection conn;
	private UsbEndpoint epIN;
	private UsbEndpoint epOUT;
	protected ListenerTask mListener;
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "USB service onCreate()");
		
		// get service
		mSerial = new FTDriver((UsbManager) getSystemService(Context.USB_SERVICE));
		
		Log.d(TAG, "New instance : " + mSerial);
		// listen for new devices
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		
		Context mContext = getApplicationContext();
		settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		mListener = new ListenerTask();
		mBaudrate = loadDefaultBaudrate();
	}

	@Override
	public void onDestroy() {
		mSerial.end();
		unregisterReceiver(mUsbReceiver);
		Log.d(TAG, "onDestroy()");
		super.onDestroy();
	}

	

	public void disconnect() {
		super.disconnect();
		if (deviceFTDI != null && conn != null) {
			conn.releaseInterface(deviceFTDI.getInterface(0));
			conn.close();
		}
		deviceFTDI = null;
		if(mSerial!=null){
			mSerial.end();
		}
		stopSelf();	
	}

	public void write(String s) {
		mSerial.write(s.getBytes(), s.length());
	}
	
	protected void write(byte[] b) {
		mSerial.write(b);
	}
	
	public void write(byte b[], int length) {
		mSerial.write(b, length);
		
	}

	// Connect actually invokes a permissions check. The actual connection
	// work is done in the BroadcastReceiver above.
	@Override
	public void connect() {
		super.connect();

		PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		mSerial.setPermissionIntent(permissionIntent);
		
		
		if(mSerial.begin(mBaudrate))
		{
			Log.d(TAG, "FTDriver began");
			loadDefaultSettingValues();
			
			Log.d(TAG, "FTDriver beginning");
			/*
			if(mListener.getStatus()!=AsyncTask.Status.RUNNING)
			{
				
				mListener.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				// Let everyone know we are connected
				Bundle b = new Bundle();
				b.putBoolean("connection", true);
				Intent i = new Intent(CONNECTION_STATUS);
				i.putExtras(b);
				sendBroadcast(i, null);
				refresh();
			}*/
			if(!mRunningMainListener)
				mainlistener();
		} else {
			Log.d(TAG, "FTDriver no connection");
			Toast.makeText(this, "no connection", Toast.LENGTH_SHORT).show();
		}
		
		
		/*
		if ((deviceFTDI = USBHostSupport.loadFTDI(mUsbManager)) == null) {
				Toast.makeText(this, "No TinyG USB host devices attached!",
					Toast.LENGTH_SHORT).show();
		} else {
			PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this,
					0, new Intent(ACTION_USB_PERMISSION), 0);
			mUsbManager.requestPermission(deviceFTDI, mPermissionIntent);
		}
		*/
		//super.connect();
	}

	// Handle data from the USB endpoint in a separate thread.
	protected class ListenerTask extends AsyncTask<Integer, String, Void> {
		
		
		
		@Override
		protected Void doInBackground(Integer... params) {
			byte[] inbuffer = new byte[USB_BUFFER_SIZE];
			byte[] linebuffer = new byte[1024];
			byte[] nwags = new byte[1024];
			int cnt, idx = 0, ipx = 0;
			try {
				while (!isCancelled()) {
					cnt = mSerial.read(inbuffer);
					if(cnt<0){
						Log.e(TAG, "mSerial read failed");
						return null;
						
					}
					/*
					if ((cnt = conn.bulkTransfer(epIN, inbuffer, USB_BUFFER_SIZE, 0)) < 2) {
						Log.e(TAG, "Bulk read failed");
						return null;
					}*/
					for (int i = 0; i < cnt; i++) {
						
						nwags[ipx++] = inbuffer[i];
						
						//if (i % 64 == 0) { // Skip the two FTDI bytes that are spaced every 64 bytes
						//	i++;
						//	continue;
					//	}		
						if (inbuffer[i] == '\n') {
							for(int j=0;j<ipx;j++){
								Log.d(TAG,"inbuffer["+j+"]= "+nwags[j]);
							}
							Log.d(TAG, "nwags: "+new String(nwags,0,ipx));
							Log.d(TAG, "nance: "+new String(linebuffer,0,idx));
							publishProgress(new String(linebuffer, 0, idx));
							idx = 0;
							ipx = 0;
						} else
							linebuffer[idx++] = inbuffer[i];
						
						
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "listener read exception: " + e.getMessage());
			}
			return null;
		}
		
		// When we receive a full line of input, parse it and send
		// a broadcast to notify activities if needed.
		@Override
		protected void onProgressUpdate(String... values) {
			Bundle b;
			if (values.length <= 0)
				return;
			sendRaw(values[0]);
			if ((b = machine.processJSON(values[0])) == null)
				return;
			updateInfo(values[0], b);
		}
		
		@Override
		protected void onCancelled() {
			Log.i(TAG, "ListenerTask cancelled");
		}
		
		@Override
	    protected void onPostExecute(Void result) {
			Log.i(TAG, "post execute ListenerTask");
			disconnect();
			return;
	    }
	}
	
	
	// Added from AUSBSML
	FTDriver mSerial;
	boolean mRunningMainListener = false;	// instead of mRunningMainLoop
	
	private static final int LINEFEED_CODE_CRLF = 1;
		
		// default settings
		private int mReadLinefeedCode		= LINEFEED_CODE_CRLF;
		private int mWriteLinefeedCode		= LINEFEED_CODE_CRLF;
		private int mBaudrate				= FTDriver.BAUD115200;
		private int mDataBits				= FTDriver.FTDI_SET_DATA_BITS_8;
		private int mParity					= FTDriver.FTDI_SET_DATA_PARITY_NONE;
		private int mStopBits				= FTDriver.FTDI_SET_DATA_STOP_BITS_1;
		private int mFlowControl			= FTDriver.FTDI_SET_FLOW_CTRL_NONE;
		private int mBreak					= FTDriver.FTDI_SET_NOBREAK;
		
	// Added from AUSBSML
	int loadDefaultBaudrate() {
		String res = settings.getString("baud", Integer.toString(FTDriver.BAUD115200));
        return Integer.valueOf(res);
	}

	private void mainlistener(){
		mRunningMainListener = true;
		Toast.makeText(this,  "connected", Toast.LENGTH_SHORT).show();
		
		Log.d(TAG, "start mainlistener");
		
		
		// instead of new Thread(mLoop).start();...
		
		mListener = new ListenerTask();
		mListener.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		// Let everyone know we are connected
		Bundle b = new Bundle();
		b.putBoolean("connection", true);
		Intent i = new Intent(CONNECTION_STATUS);
		i.putExtras(b);
		sendBroadcast(i, null);
		refresh();
	}

	private void openUsbSerial(){
		if(!mSerial.isConnected()){
			Log.d(TAG, "onNewIntent begin");
			mBaudrate = loadDefaultBaudrate();
			if(!mSerial.begin(mBaudrate)) {
				Toast.makeText(this, "cannot open", Toast.LENGTH_SHORT).show();
				return;
			} else {
				Toast.makeText(this, "connected", Toast.LENGTH_SHORT).show();
			}
		}
		if(!mRunningMainListener){
			mainlistener();
		}
	}

	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent");
		openUsbSerial();
	};
	
	private void closeUsbSerial() {
		disconnect();
		mSerial.end();
	}

	
	void loadDefaultSettingValues(){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		String res = pref.getString("readlinefeedcode_list", Integer.toString(LINEFEED_CODE_CRLF));
		mReadLinefeedCode = Integer.valueOf(res);
		
		res = pref.getString("writelinefeedcode_list", Integer.toString(LINEFEED_CODE_CRLF));
		mWriteLinefeedCode = Integer.valueOf(res);
		
		res = pref.getString("databits_list", Integer.toString(FTDriver.FTDI_SET_DATA_BITS_8));
		mDataBits = Integer.valueOf(res);
		mSerial.setSerialPropertyDataBit(mDataBits, FTDriver.CH_A);
		
		res = pref.getString("parity_list", Integer.toString(FTDriver.FTDI_SET_DATA_PARITY_NONE));
		mParity = Integer.valueOf(res) << 8;
		mSerial.setSerialPropertyParity(mParity, FTDriver.CH_A);
		
		res = pref.getString("stopbits_list", Integer.toString(FTDriver.FTDI_SET_DATA_STOP_BITS_1));
        mStopBits = Integer.valueOf(res) << 11; // stopbits_list's number is 0 to 2
        mSerial.setSerialPropertyStopBits(mStopBits, FTDriver.CH_A);

        res = pref
                .getString("flowcontrol_list", Integer.toString(FTDriver.FTDI_SET_FLOW_CTRL_NONE));
        mFlowControl = Integer.valueOf(res) << 8;
        mSerial.setFlowControl(FTDriver.CH_A, mFlowControl);

        res = pref.getString("break_list", Integer.toString(FTDriver.FTDI_SET_NOBREAK));
        mBreak = Integer.valueOf(res) << 14;
        mSerial.setSerialPropertyBreak(mBreak, FTDriver.CH_A);

        mSerial.setSerialPropertyToChip(FTDriver.CH_A);
		
	}
	

}
