package com.opentrons.sbtk;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

public class MyEditTextPreference extends EditTextPreference{
	SharedPreferences settings;
	private PrefsListener mPreferencesListener;
	private String thisKey = this.getKey();
	
	public MyEditTextPreference(Context context) { 
		super(context);
		settings = PreferenceManager.getDefaultSharedPreferences(context);
		settings.registerOnSharedPreferenceChangeListener(mPreferencesListener);
	}
	
	public MyEditTextPreference(Context context, AttributeSet attrs) { 
		super(context, attrs);
		settings = PreferenceManager.getDefaultSharedPreferences(context);
		settings.registerOnSharedPreferenceChangeListener(mPreferencesListener);
	}
	
	@Override
	public void setText(String value) {
		super.setText(value);
		setSummary(String.valueOf(getText()));
	}
	
	private class PrefsListener implements SharedPreferences.OnSharedPreferenceChangeListener
	{
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			if(key.equals(thisKey))
				setSummary(String.valueOf(getText()));
		}
	}
	
	
}
