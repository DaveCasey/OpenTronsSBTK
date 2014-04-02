package com.opentrons.sbtk;

import java.util.List;


import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.app.FragmentTransaction;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity
implements OnSharedPreferenceChangeListener{
	private static final String TAG = "SettingsActivity";
	private int bindType = 0;
	private boolean connected = false;
	final static String ACTION_CONNECTION = "com.nwags.BetaBot.PREFS_CONNECTION";
	final static String ACTION_MOTOR = "com.nwags.BetaBot.PREFS_MOTOR";
	final static String ACTION_AXIS = "com.nwags.BetaBot.PREFS_AXIS";
	final static String ACTION_SYSTEM = "com.nwags.BetaBot.PREFS_SYSTEM";
	
	public static String KEY_CONNECTION = "connection";
	
	SharedPreferences settings;
	private static Context mContext;
	private static boolean debug;
	
	private void restoreState(Bundle inState)
	{
		this.bindType = inState.getInt("bindType");
		this.connected = inState.getBoolean("connected");
		if(debug)
			Log.d(TAG,"restoreState() connected state is " + this.connected);
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mContext = getApplicationContext();
		settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		debug = settings.getBoolean("debug", false);
		
		String action = getIntent().getAction();
	    if (action != null && action.equals(ACTION_CONNECTION)) {
	        addPreferencesFromResource(R.xml.preference_connection);
	    } else if (action!=null && action.equals(ACTION_SYSTEM)){
	    	addPreferencesFromResource(R.xml.preference_system);
	    	PreferenceManager.setDefaultValues(this, R.xml.preference_system, false);
	    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
	        // Load the legacy preferences headers
	        addPreferencesFromResource(R.xml.preference_headers_legacy);
	    }
		
	    
	    
		if(savedInstanceState != null)
			restoreState(savedInstanceState);		
	}
	
	

	private class SettingsTabListener implements ActionBar.TabListener
	{
		private SettingsTabListener(){}
		
		public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft){}
		
		public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft)
		{
			if(tab.getText().equals("Settings")){
				
				// TODO: INTENT & SETTINGSACTIVITY
				
			}else if(tab.getText().equals("Home")||tab.getText().equals("Connection")||tab.getText().equals("Command")||tab.getText().equals("File")||tab.getText().equals("Jog")){
				Intent intent = new Intent();
				// TODO: INTENT & MAINACTIVITY
				if(tab.getText().equals("Connection")){ // 1
					setResult(1,intent);
				} else if(tab.getText().equals("Command")){ // 2
					setResult(2,intent);
				} else if(tab.getText().equals("File")){ // 3
					setResult(3,intent);
				} else if(tab.getText().equals("Jog")){ // 4
					setResult(4,intent);
				} else if(tab.getText().equals("Config")){	// 5
					setResult(5,intent);
				} else { // 0
					setResult(0,intent);
				}
				finish();
			}
		}
		
		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {	}
		
	}
	
	
	@Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Preference pref = findPreference(key);
		if(pref instanceof ListPreference){
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());
		}else if(pref instanceof MyEditTextPreference){
			MyEditTextPreference myEditTextPref = (MyEditTextPreference) pref;
			pref.setSummary(myEditTextPref.getText());
		}else if(pref instanceof MyEditFloatTextPreference){
			MyEditFloatTextPreference myEditFloatTextPref = (MyEditFloatTextPreference) pref;
			pref.setSummary(myEditFloatTextPref.getText());
		}
		
		if(key.equals(KEY_CONNECTION)){
			Preference connectionPref = findPreference(key);
		}
		
	}
	
	
	
	
	
	
	
}
