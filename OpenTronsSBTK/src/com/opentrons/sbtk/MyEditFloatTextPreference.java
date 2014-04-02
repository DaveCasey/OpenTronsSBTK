package com.opentrons.sbtk;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

public class MyEditFloatTextPreference extends EditTextPreference{
	SharedPreferences settings;
	private PrefsListener mPreferencesListener;
	private String thisKey = this.getKey();
	
	public MyEditFloatTextPreference(Context context) { 
		super(context);
		settings = PreferenceManager.getDefaultSharedPreferences(context);
		settings.registerOnSharedPreferenceChangeListener(mPreferencesListener);
		this.getEditText().setInputType(2|8192);
	}
	
	public MyEditFloatTextPreference(Context context, AttributeSet attrs) { 
		super(context, attrs);
		settings = PreferenceManager.getDefaultSharedPreferences(context);
		settings.registerOnSharedPreferenceChangeListener(mPreferencesListener);
		this.getEditText().setInputType(2|8192);
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
