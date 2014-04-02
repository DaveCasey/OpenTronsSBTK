package com.opentrons.sbtk;

import java.util.Locale;


import com.opentrons.sbtk.USBAccessory.USBAccessoryService;
import com.opentrons.sbtk.USBHost.USBHostService;
import com.opentrons.sbtk.ControlsFragment.ControlsFragmentListener;
import com.opentrons.sbtk.DeviceListActivity;
import com.opentrons.sbtk.MainActivity;
import com.opentrons.sbtk.Bluetooth.BluetoothSerialService;
import com.opentrons.sbtk.Support.SBTKService;


import android.annotation.SuppressLint;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements 
	HomeFragment.HomeFragmentListener,
	ControlsFragmentListener {

	
	public static final String TAG = "MainActivity";
	public static int bindType = 0;
	public static boolean connected = false;
	public static ServiceConnection currentServiceConnection;
	public static BroadcastReceiver mIntentReceiver;
	//public static PrefsListener mPreferencesListener;
	public static boolean pendingConnect = false;
	public static SBTKService SBTKBot = null;
	public static BluetoothSerialService BTSBTKBot = null;
	
	public static SharedPreferences settings;
	public static Context mContext;
	public static boolean debug;
	
	// bluetooth
	// Message types sent from the BluetoothReadService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;	
	
	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	
	public static final int REQUEST_CONNECT_DEVICE = 1;
	public static final int REQUEST_ENABLE_BT = 2;
	public static final int REQUEST_WTF = 3;
	
	private static final int REQUEST_CONFIG = 12;
	
	public static BluetoothAdapter mBluetoothAdapter = null;
	public static String mConnectedDeviceName = null;
	public static MenuItem mMenuItemConnect;
	public static final int TEXT_MAX_SIZE = 8192;
	
	public static boolean connecting;
	
	//HOMEFRAGMENT UNLOCK/LOCK
	public static String[] tabsL;
	public static TabListener tabListenerL;
	public static SharedPreferences.Editor EdL;
	public static boolean locked = true;
	
	public static View view;
	
	public static ProgressDialog progressBar;
	public static Handler systemHandler = new Handler();
	
	public static FragmentManager fragman;
	
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a {@link FragmentPagerAdapter}
	 * derivative, which will keep every loaded fragment in memory. If this
	 * becomes too memory intensive, it may be best to switch to a
	 * {@link android.support.v13.app.FragmentStatePagerAdapter}.
	 */
	static SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	static ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		
		view = this.findViewById(R.layout.activity_main);
		
		mContext = getApplicationContext();
		settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		debug = settings.getBoolean("debug", false);
		
		
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		Resources res = getResources();
		String[] tabs = res.getStringArray(R.array.tabArray);
		
		
		
		
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		//getFragmentManager();
		fragman = getSupportFragmentManager();
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());//getFragmentManager());
		mSectionsPagerAdapter.setCount(2);
		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});
		
		TabListener tabListener = (TabListener) new MainTabListener();
		for(int i=0;i<2;i++){
			Tab tab = actionBar.newTab();
			tab.setText(tabs[i]);
			tab.setTag(tabs[i]);
			tab.setTabListener(tabListener);
			actionBar.addTab(tab);
		}
		
		bindType = Integer.parseInt(settings.getString("connectionType","0"));
		
		if(savedInstanceState!=null)
			restoreState(savedInstanceState);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		android.view.MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options, menu);
		mMenuItemConnect = menu.findItem(R.id.connect);
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);//true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(connected)
			mMenuItemConnect.setTitle(R.string.disconnect);
		else
			mMenuItemConnect.setTitle(R.string.connect);
		return super.onPrepareOptionsMenu(menu);
	}
	
	private void restoreState(Bundle inState)
	{
		MainActivity.bindType = inState.getInt("bindType");
		MainActivity.connected = inState.getBoolean("connected");
		if(debug)
			Log.d("SBTKBot","restoreState() connected state is " + MainActivity.connected);
		
	}
	
	public boolean connectionState()
	{
		return MainActivity.connected;
	}
	
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putInt("bindType", bindType);
		outState.putBoolean("connected", connected);
		if(debug)
			Log.d(TAG, "onSaveInstanceState() connected state is "
				+ connected);
		
	}
	
	public void onDestroy()
	{
		if(bindType==0){
			if(MainActivity.BTSBTKBot!=null)
			{
				unbindService(MainActivity.currentServiceConnection);
				MainActivity.BTSBTKBot = null;
			}
		}else{
			if(MainActivity.SBTKBot!=null)
			{
				unbindService(MainActivity.currentServiceConnection);
				MainActivity.SBTKBot=null;
			}
		}
		super.onDestroy();
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		/*int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);*/
		
		if(debug)
			Log.d(TAG, "onOptionsItemSelected");
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.connect:
			if (pendingConnect) {
				if(debug)
					Log.d(TAG, "Waiting for connection...");
				return true;
			}
			if(bindType==0){
				if (BTSBTKBot == null) {
					connected = false;
					currentServiceConnection = new DriverServiceConnection();
					bindDriver(currentServiceConnection);
					// We can't call connect until we know we have a binding.
					pendingConnect = true;
					if(debug)
						Log.d(TAG, "Binding... BTSBTKBot");
					
					return true;
				}
				if (connected)
					BTSBTKBot.disconnect();
				else {
					if(debug)
						Log.d(TAG, "Conn using old binding");
					//BTBetaBot.connect();
					Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
				}
			
			}else{
				if (SBTKBot == null) {
					connected = false;
					currentServiceConnection = new DriverServiceConnection();
					bindDriver(currentServiceConnection);
					// We can't call connect until we know we have a binding.
					pendingConnect = true;
					if(debug)
						Log.d(TAG, "Binding...");
					
					return true;
				}
				if (connected)
					SBTKBot.disconnect();
				else {
					if(debug)
						Log.d(TAG, "Conn using old binding");
					SBTKBot.connect();
					
				}
			}
			return true;
		case R.id.refresh:
			Fragment f = getSupportFragmentManager().findFragmentById(
					R.id.pager);//R.id.tabview
			//if (f != null && f.getClass() == FileFragment.class && ((FileFragment) f).isActive())
			//	return true;
			if(f!=null){
				Log.d(TAG,"refresh frag: "+f.toString());
			}else{
				Log.d(TAG,"refresh frag: null");
			}
			if (connected){
				if(bindType==0){
					BTSBTKBot.refresh();
				}else{
					SBTKBot.refresh();
				}
			}else{ 
				endConnecting();
				Toast.makeText(this, "Not connected!", Toast.LENGTH_SHORT)
						.show();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	public int getBluetoothConnectionState(){
		if (BTSBTKBot!=null)
			return BTSBTKBot.getState();
		else
			return BluetoothSerialService.STATE_NONE;
	}
	
	public void finishDialogNoBluetooth() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.alert_dialog_no_bt)
		.setIcon(android.R.drawable.ic_dialog_info)
		.setTitle(R.string.app_name)
		.setCancelable( false )
		.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				finish();            	
			}
		});
		AlertDialog alert = builder.create();
		alert.show(); 
	}
	
	public static void endConnecting(){
		Log.d(TAG, "endConnecting");
		connecting = false;
	}
	
	public void endStartup(){
		Log.d(TAG, "endStartup");
		//startup_sequence = false;
	}
	
/*
	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}
*/
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
		int mCount = 0;
		final private FragmentManager mFragmentManager;
		
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
			mFragmentManager = fm;
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a PlaceholderFragment (defined as a static inner class
			// below).
			Fragment f;
			switch(position){
			case 0:
				f = mFragmentManager.findFragmentByTag("Home");
				if(f==null){
					f = new HomeFragment();
					//mFragmentManager.beginTransaction().add(f, "Home").commit();
				}
				break;
			case 1:
				f = mFragmentManager.findFragmentByTag("Controls");
				if(f==null){
					f = new ControlsFragment();
					//mFragmentManager.beginTransaction().add(f, "Controls").commit();
					Log.d(TAG, "getItem():f = new ControlsFragment()");
				}else{
					Log.d(TAG, "getItem():f is not null");
				}
				
				break;
			case 2:
				f = mFragmentManager.findFragmentByTag("Config");
				if(f==null){
					f = new ConfigFragment();
					//mFragmentManager.beginTransaction().add(f, "Config").commit();
				}
				break;
			default:
				f = mFragmentManager.findFragmentByTag("Home");
				if(f==null){
					f = new HomeFragment();
					//mFragmentManager.beginTransaction().add(f, "Home").commit();
				}
				break;
			}
			return f;//PlaceholderFragment.newInstance(position + 1);
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return mCount;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			Resources res = getResources();
			String[] tabs = res.getStringArray(R.array.tabArray);
			
			switch (position) {
			case 0:
				return tabs[position];//getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return tabs[position];//getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return tabs[position];//getString(R.string.title_section3).toUpperCase(l);
			case 3:
				return tabs[position];
			}
			return null;
		}
		
		@Override
	    public int getItemPosition(Object object){
			Fragment fragment = (Fragment) object;
			for(int i = 0; i < mCount; i++) {
				if(getItem(i).getClass()==fragment.getClass()){
					return i;
				}
			}
	        return PagerAdapter.POSITION_NONE;
	    }
		
		
		public void setCount(int count){
			if(count>0 && count<=10){
				mCount = count;
			}
		}
		
		
		@Override
	    public void destroyItem(ViewGroup container, int position, Object object) {
	        super.destroyItem(container, position, object);
	        FragmentManager manager = ((Fragment) object).getFragmentManager();
	        FragmentTransaction trans = manager.beginTransaction();
	        trans.remove((Fragment) object);
	        trans.commit();
	    }
		
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";

		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			TextView textView = (TextView) rootView
					.findViewById(R.id.section_label);
			textView.setText(Integer.toString(getArguments().getInt(
					ARG_SECTION_NUMBER)));
			return rootView;
		}
	}
	
	
	private class DriverServiceConnection implements ServiceConnection
	{
		private DriverServiceConnection() { }
		
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			SBTKService.SBTKBinder binder = (SBTKService.SBTKBinder)service;
			if(debug)
				Log.d(TAG,"Service connected");
			
			
			if(bindType==0){
				MainActivity.BTSBTKBot = (BluetoothSerialService) binder.getService();
				BTSBTKBot.setHandler(mHandlerBT);
				
				if((mBluetoothAdapter!=null)&&(!mBluetoothAdapter.isEnabled())){
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setMessage(R.string.alert_dialog_turn_on_bt)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.alert_dialog_warning_title)
					.setCancelable( false )
					.setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							//mEnablingBT = true;
							Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
							startActivityForResult(enableIntent, REQUEST_ENABLE_BT);			
						}
					})
					.setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							finishDialogNoBluetooth();
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				}else if(getBluetoothConnectionState()==BluetoothSerialService.STATE_NONE){
					Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
				} else if (getBluetoothConnectionState()==BluetoothSerialService.STATE_CONNECTED){
					BTSBTKBot.stop();
					BTSBTKBot.start();
				}
			} else {
				MainActivity.SBTKBot = binder.getService();
				AlertDialog.Builder settingsBldr = new AlertDialog.Builder(MainActivity.this);
				settingsBldr.setMessage("Load settings for board?")
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle("Board Settings")
					.setCancelable( true )
					.setPositiveButton("Load", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							
						}
					})
					.setNeutralButton("Review", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							
						}
					});
				AlertDialog alert = settingsBldr.create();
				alert.show();
			}
			
			if(MainActivity.pendingConnect)
			{
				if(bindType==0){
					BTSBTKBot.connect();
					pendingConnect = false;
				}else{
					MainActivity.SBTKBot.connect();
					MainActivity.pendingConnect = false;
				}
			}
		}
		
		public void onServiceDisconnected(ComponentName className)
		{
			if(debug)
				Log.d(TAG, "Service disconnected");
			MainActivity.SBTKBot = null;
			
			if(BTSBTKBot!=null)
				BTSBTKBot.disconnect();
			BTSBTKBot=null;
		}
	}
	
	
	
	
	
//	Listener stuff
	
	private class MainTabListener implements ActionBar.TabListener
	{

		private MainTabListener(){}
		
		public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction fat){	}
		
		public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction fat)
		{
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			Fragment f;
			FragmentManager fm = getSupportFragmentManager();
			f = fm.findFragmentByTag((String) tab.getText());
			
			if (f == null)
			{
				Log.d(TAG, "MainTabListener->onTabSelected:f==null");
				if(tab.getText().equals("Settings"))
				{
					Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
					startActivity(intent);
					return;
				} /*else if (tab.getText().equals("Controls")) {
					f = new ControlsFragment();
					if(bindType==0)
					{
						if(BTSBTKBot != null){
							//((ControlsFragment) f).updateState(BTSBTKBot.getMachine());
						}
					}else{
						if(SBTKBot != null){
							//((ControlsFragment) f).updateState(SBTKBot.getMachine());
						}
					}
					
				}else if(tab.getText().equals("Config")){
					f = new ConfigFragment();
				}else{
					f = new HomeFragment();
				}*/
				//ft.add(R.id.tabview, f, (String) tab.getText());
				//ft.replace(R.id.pager, f);//R.id.tabview
				mViewPager.setCurrentItem(tab.getPosition());
				/*
				Fragment ef = mSectionsPagerAdapter.getItem(mViewPager.getCurrentItem());
				if(ef.getClass()==ControlsFragment.class){
					ControlsFragment cf = (ControlsFragment) ef;
					cf.setBack();
				}
				*/
				
				if(!tab.getText().equals("Settings"))
					ft.addToBackStack(null);
				ft.commit();
			} else {
				if (f.isDetached())
				{	
					//ft.replace(R.id.pager, f);//R.id.tabview
					mViewPager.setCurrentItem(tab.getPosition());
					if(!tab.getText().equals("Settings"))
						ft.addToBackStack(null);
					
					ft.commit();
				}
				
			}
			
		}
		
		public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction fat)
		{
			android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			FragmentManager fm = MainActivity.this.getSupportFragmentManager();
			Fragment f = (Fragment) fm.findFragmentByTag((String)tab.getText());
			if (f != null)
				ft.detach(f);
			ft.commit();
		}

	}	
	
	private final static Handler mHandlerBT = new Handler(){
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
			case MESSAGE_STATE_CHANGE:
				if(debug)
					Log.d(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothSerialService.STATE_CONNECTED:
					connected = true;
					BTSBTKBot.connect();
					if (mMenuItemConnect != null) {
						mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
						mMenuItemConnect.setTitle(R.string.disconnect);
					}					
					
					if(mViewPager.getCurrentItem()==1){
						setControlsBackground();
					}
					//mInputManager.showSoftInput(mEmulatorView, InputMethodManager.SHOW_IMPLICIT);

					//mTitle.setText(R.string.title_connected_to);
					//mTitle.append(mConnectedDeviceName);
					break;

				case BluetoothSerialService.STATE_CONNECTING:
					//mTitle.setText(R.string.title_connecting);
					break;

				case BluetoothSerialService.STATE_LISTEN:
				case BluetoothSerialService.STATE_NONE:
					if (mMenuItemConnect != null) {
						mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
						mMenuItemConnect.setTitle(R.string.connect);
					}

					//mInputManager.hideSoftInputFromWindow(mEmulatorView.getWindowToken(), 0);

					//mTitle.setText(R.string.title_not_connected);

					break;
				}
				break;
			/*
			case MESSAGE_WRITE:
				if (mLocalEcho) {
					byte[] writeBuf = (byte[]) msg.obj;
					mEmulatorView.write(writeBuf, msg.arg1);
				}

				break;
				/*                
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;              
                mEmulatorView.write(readBuf, msg.arg1);

                break;
				 */                
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				
				
				Toast.makeText(mContext, "Connected to "
						+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
				
				endConnecting();
				
				break;
			case MESSAGE_TOAST:
				endConnecting();
				Toast.makeText(mContext, msg.getData().getString(TOAST),
						Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
	
	public boolean bindDriver(ServiceConnection s)
	{
		if(debug)
			Log.d(TAG, "bindDriver(ServiceConnection s)");
		
		switch (bindType) {
		case 0: // BlueTooth
			
			//return bindService(new Intent(getApplicationContext(),
			//		TinyGNetwork.class), s, Context.BIND_AUTO_CREATE);
			
			ComponentName mBTService = startService(new Intent(this, BluetoothSerialService.class));
			return bindService(new Intent(this, BluetoothSerialService.class)
				, s, Context.BIND_AUTO_CREATE);
			
			
		case 1: // USB host
			// Check to see if the platform supports USB host
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
				Toast.makeText(this, R.string.no_usb_host, Toast.LENGTH_SHORT)
						.show();
				return false;
			}
			ComponentName mUSBHService = startService(new Intent(this, USBHostService.class));
			return bindService(new Intent(getApplicationContext(),
					USBHostService.class), s, Context.BIND_AUTO_CREATE);
		case 2: // USB accessory
			// Check to see if the platform support USB accessory
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				Toast.makeText(this, R.string.no_usb_accessory,
						Toast.LENGTH_SHORT).show();
				return false;
			}
			ComponentName mUSBAService = startService(new Intent(this, USBAccessoryService.class));
			return bindService(new Intent(getApplicationContext(),
					USBAccessoryService.class), s, Context.BIND_AUTO_CREATE);
		default:
			return false;
		}
	}

	@Override
	public void unlock() {
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		Resources res = getResources();
		tabsL = res.getStringArray(R.array.tabArray);
		tabListenerL = (TabListener) new MainTabListener();
		EdL = settings.edit();
		if(locked){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Congratulations, you can now change settings. But please proceed with caution.")
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Configuration Access")
			.setCancelable( false )
			.setPositiveButton("ENTER", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					locked = false;
					EdL.putString("locked", "false");
					EdL.commit();
					if(actionBar.getTabCount()<3){
						for(int i=2; i < tabsL.length; i++){
							Tab tab = actionBar.newTab();
							tab.setText(tabsL[i]);
							tab.setTag(tabsL[i]);
							tab.setTabListener(tabListenerL);
							actionBar.addTab(tab);
							mSectionsPagerAdapter.setCount(i+1);
							mSectionsPagerAdapter.notifyDataSetChanged();
						}
					}
				}
			})
			.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			
		} else {
			for(int i = tabsL.length; i>2; i--){
				
				mSectionsPagerAdapter.setCount(i-1);
				actionBar.removeTabAt(i-1);
				mSectionsPagerAdapter.notifyDataSetChanged();
			}
			//mViewPager.getAdapter().notifyDataSetChanged();
			/*
			if(actionBar.getTabCount()>5){
				actionBar.removeTabAt(5);
				actionBar.removeTabAt(4);
				actionBar.removeTabAt(3);
			}
			*/
			
			EdL.putString("locked", "true");
			EdL.commit();
			locked = true;
		}
		
	}

	@Override
	public boolean isLocked() {
		return locked;
	}

	@Override
	public void sendCommand(String cmd) {
		if(bindType==0){
			if((MainActivity.BTSBTKBot == null)||(!MainActivity.connected))
				return;
			
			MainActivity.BTSBTKBot.send_command(cmd);
		}else{
			if((MainActivity.SBTKBot == null)||(!MainActivity.connected))
				return;
			
			MainActivity.SBTKBot.send_command(cmd);
		}
	}
	
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(debug)
			Log.d(TAG, "onActivityResult " + resultCode + " : requestCode is " + requestCode);
		if(resultCode == Activity.RESULT_OK){
			
			switch(requestCode) {
			case REQUEST_CONNECT_DEVICE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK) {
					
					connecting = true;
					progressBar = new ProgressDialog(MainActivity.this);
					progressBar.setCancelable(false);
					progressBar.setIndeterminate(true);
					progressBar.setMessage("Connecting ...");
					progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressBar.setProgress(0);
					progressBar.setMax(100);
					progressBar.show();
					
					new Thread(new Runnable() {
						public void run() {
							while(connecting){
								try{
									Thread.sleep(1000);
								}catch(InterruptedException e){
									e.printStackTrace();
								}
							}
							
							progressBar.dismiss();
							if(connected){
								//systemHandler.postDelayed(SettingsRunner, 1000);
							//LoadSettings();
							}
							
						}
					}).start();
					
					/*
					Toast toast = Toast.makeText(getApplicationContext(),"Connecting...",Toast.LENGTH_LONG);
		            toast.setGravity(Gravity.CENTER, 0, 0);
		            toast.show();
		            */
					// Get the device MAC address
					String address = data.getExtras()
							.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
					// Get the BLuetoothDevice object
					BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
					// Attempt to connect to the device
					BTSBTKBot.connect(device);                
				}
				break;

			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode == Activity.RESULT_OK) {
					if(debug)
						Log.d(TAG, "BT not enabled");
					//finishDialogNoBluetooth();                
				}
			}
		}
	}
	
	public class SBTKServiceReceiver extends BroadcastReceiver
	{
		//@Override
		@SuppressLint({"NewApi"})
		public void onReceive(Context context, Intent intent) 
		{
			Bundle b = intent.getExtras();
			String action;
			action = intent.getAction();
			if(action.equals(SBTKService.JSON_ERROR)){
				
			}
			if(action.equals(SBTKService.CONNECTION_STATUS))
			{
				MainActivity.connected = b.getBoolean("connection");
				if(!MainActivity.connected){
					MainActivity.pendingConnect = false;
					//connected
				}
				MainActivity.this.invalidateOptionsMenu();
			}
			if(action.equals(SBTKService.TEMP)) {
				Log.d(TAG,"TEMP!");
				SectionsPagerAdapter spa = (SectionsPagerAdapter) mViewPager.getAdapter();
				Fragment f = spa.getItem(mViewPager.getCurrentItem());
				if(f!=null) {
					Log.d(TAG,"Fragment f ==> "+f.toString());
				}else{
					Log.d(TAG,"Fragment f ==> null");
				}
				if(f!=null && f.getClass()==ControlsFragment.class) {
					((ControlsFragment) f).updateTemp(b);
				}else{
					Log.d(TAG,"updateTemp not called");
				}
				
				
			}
			
		}
		
	}
	
	@Override
	public void onResume()
	{
		IntentFilter updateFilter = new IntentFilter();
		updateFilter.addAction(SBTKService.STATUS);
		updateFilter.addAction(SBTKService.CONNECTION_STATUS);
		updateFilter.addAction(SBTKService.SYSTEM);
		updateFilter.addAction(SBTKService.TEMP);
		mIntentReceiver = new SBTKServiceReceiver();
		registerReceiver(mIntentReceiver, updateFilter);
		
		super.onResume();
		
		final ActionBar actionBar = getActionBar();
		SectionsPagerAdapter spa = (SectionsPagerAdapter) mViewPager.getAdapter();
		Fragment f = spa.getItem(mViewPager.getCurrentItem());
		if(f!=null){
			if(f.getClass()==ConfigFragment.class){
				actionBar.setSelectedNavigationItem(2);
			}else if(f.getClass()==ControlsFragment.class){
				actionBar.setSelectedNavigationItem(1);
			}else if(f.getClass()==HomeFragment.class){
				actionBar.setSelectedNavigationItem(0);
			}
		}
	}
	
	@Override
	public void onPause(){
		unregisterReceiver(mIntentReceiver);
		super.onPause();
	}

	@Override
	public void sendReset()
	{
		if(bindType==0){
			if((MainActivity.BTSBTKBot == null) || (!MainActivity.connected))
				return;
			
			MainActivity.BTSBTKBot.send_reset();
		}else{
			if((MainActivity.SBTKBot == null) || (!MainActivity.connected))
				return;
			
			MainActivity.SBTKBot.send_reset();
		}
	}

	@Override
	public boolean isConnected() {
		return connected;
	}
	
	public static void setControlsBackground(){
		SectionsPagerAdapter spa = (SectionsPagerAdapter) mViewPager.getAdapter();
		Fragment f = spa.getItem(mViewPager.getCurrentItem());
		if(f!=null) {
			Log.d(TAG,"Fragment f ==> "+f.toString());
		}else{
			Log.d(TAG,"Fragment f ==> null");
		}
		if(f!=null && f.getClass()==ControlsFragment.class) {
			((ControlsFragment) f).setBack();
		}else{
			Log.d(TAG,"setBack not called");
		}
	}
	
	
}
