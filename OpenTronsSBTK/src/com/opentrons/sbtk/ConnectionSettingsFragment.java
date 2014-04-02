package com.opentrons.sbtk;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class ConnectionSettingsFragment extends PreferenceFragment{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(getActivity(), R.xml.preference_connection, false);
		addPreferencesFromResource(R.xml.preference_connection);
	}
}
