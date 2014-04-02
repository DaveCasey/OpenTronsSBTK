package com.opentrons.sbtk.USBAccessory;
 
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.opentrons.sbtk.MainActivity;


public final class USBAccessoryActivity extends Activity
{
protected void onCreate(Bundle savedInstanceState)
{
     super.onCreate(savedInstanceState); 
     Intent i = new Intent(this, MainActivity.class);
 
     i.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT); 
     startActivity(i); 
     finish();
   }
}
