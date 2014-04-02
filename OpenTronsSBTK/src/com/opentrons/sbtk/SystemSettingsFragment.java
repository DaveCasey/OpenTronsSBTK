package com.opentrons.sbtk;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SystemSettingsFragment extends PreferenceFragment{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		PreferenceManager.setDefaultValues(getActivity(), R.xml.preference_system, false);
		addPreferencesFromResource(R.xml.preference_system);
		
	}
	
}
