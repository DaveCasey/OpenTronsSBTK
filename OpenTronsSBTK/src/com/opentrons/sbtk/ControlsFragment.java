package com.opentrons.sbtk;



import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ControlsFragment extends Fragment{
	View view;
	private static ControlsFragmentListener parent;
	private static final String TAG = "ControlsFragment";
	
	
	public static SharedPreferences settings;
	public static Context mContext;
	public static boolean debug;
	
	public static boolean locked = true;
	
	
	public static Button resetBtn;
	
	public static ToggleButton centTgl;
	public static SeekBar speedSkBr;
	public static Spinner speedSpnr;
	public static TextView speedTxtVw;
	//ArrayAdapter<String> speed_ab;
	
	
	public static ToggleButton heatTgl;
	public static SeekBar tempSkBr;
	public static TextView tempTxtVw;
	public static ToggleButton tempTgl;
	//ArrayAdapter<String> temp_ab;
	
	public static ToggleButton coolTgl;
	public static ToggleButton fanTgl;
	
	public static String mostRecentTemp;
	
	public static RelativeLayout big_back;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG,"onAttach called");
		try {
			parent = (ControlsFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement ControlsFragmentListener");
		}
		
		if(view!=null){
			RelativeLayout backRltvLyt = (RelativeLayout) view.findViewById(R.id.big_back);
			if(parent.isConnected()){
				backRltvLyt.setBackgroundColor(getResources().getColor(R.color.light_green));
			}else{
				backRltvLyt.setBackgroundColor(Color.WHITE);
			}
		}else{
			Log.d(TAG,"view is null");
		}
		
	}
	
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(TAG,"onCreateView called");
		view = inflater.inflate(R.layout.controls, container, false);
		
		resetBtn = (Button) view.findViewById(R.id.reset_button);
		
		centTgl = (ToggleButton) view.findViewById(R.id.centrifuge_toggle);
		speedSkBr = (SeekBar) view.findViewById(R.id.centrifuge_seekbar);
		speedSpnr = (Spinner) view.findViewById(R.id.centrifuge_spinner);
		speedTxtVw = (TextView) view.findViewById(R.id.speed_value);
		
		heatTgl = (ToggleButton) view.findViewById(R.id.heat_toggle);
		tempSkBr = (SeekBar) view.findViewById(R.id.heat_seekbar);
		tempTxtVw = (TextView) view.findViewById(R.id.temp_value);
		tempTgl = (ToggleButton) view.findViewById(R.id.temp_toggle);
		
		
		coolTgl = (ToggleButton) view.findViewById(R.id.cool_toggle);
		fanTgl = (ToggleButton) view.findViewById(R.id.fan_toggle);
		
		big_back = (RelativeLayout) view.findViewById(R.id.big_back);
		if(parent.isConnected()){
			big_back.setBackgroundColor(getResources().getColor(R.color.light_green));
		}else{
			big_back.setBackgroundColor(Color.WHITE);
		}
		
		
		resetBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				centTgl.setChecked(false);
				heatTgl.setChecked(false);
				tempTgl.setChecked(false);
				coolTgl.setChecked(false);
				fanTgl.setChecked(false);
				speedSpnr.setSelection(0);
				speedSkBr.setProgress(0);
				tempSkBr.setProgress(0);
				parent.sendReset();
			}
		});
		
		String[] speedArray = getResources().getStringArray(R.array.speeds);
		final ArrayAdapter<String> speed_ab = new ArrayAdapter<String>(getActivity().getApplicationContext(), R.layout.spinner_item, speedArray);
		speedSpnr.setAdapter(speed_ab);
		
		speedSpnr.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentus, View view,
					int pos, long id) {
				String strValue;
				switch(pos){
				case 0:
					break;
				case 1:
					strValue = "0";
					speedTxtVw.setText(strValue);
					speedSkBr.setProgress(0);
					if(centTgl.isChecked()){
						((ControlsFragmentListener) parent).sendCommand("{\"speed\":0}\n");
					}
					break;
				case 2:
					strValue = "25";
					speedTxtVw.setText(strValue);
					speedSkBr.setProgress(25);
					if(centTgl.isChecked()){
						((ControlsFragmentListener) parent).sendCommand("{\"speed\":64}\n");
					}
					break;
				case 3:
					strValue = "50";
					speedTxtVw.setText(strValue);
					speedSkBr.setProgress(50);
					if(centTgl.isChecked()){
						((ControlsFragmentListener) parent).sendCommand("{\"speed\":128}\n");
					}
					break;
				case 4:
					strValue = "75";
					speedTxtVw.setText(strValue);
					speedSkBr.setProgress(75);
					if(centTgl.isChecked()){
						((ControlsFragmentListener) parent).sendCommand("{\"speed\":191}\n");
					}
					break;
				case 5:
					strValue = "100";
					speedTxtVw.setText(strValue);
					speedSkBr.setProgress(100);
					if(centTgl.isChecked()){
						((ControlsFragmentListener) parent).sendCommand("{\"speed\":255}\n");
					}
					break;
				}
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
			}
			 
		 });
		
		centTgl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					String speedStr = speedTxtVw.getText().toString();
					//speedTxtVw.setText(speedStr);
					int speedInt = 0;
					try{
						if(speedStr.equals(" - - - ")||speedStr.equals("0")){
							speedInt = 100;
							speedSkBr.setProgress(100);
						}else{
							speedInt = Integer.parseInt(speedStr);
						}
						if(speedInt>-1&&speedInt<101){
							speedInt = (speedInt*255)/100;
							String sendMsg = "{\"speed\":"+String.valueOf(speedInt)+"}\n";
							byte[] strBytes = sendMsg.getBytes();//not necessary
							parent.sendCommand(sendMsg);
						}else{
							ToastMaster("Choose a number from 0-100");
						}
					}catch(NumberFormatException e){
						e.printStackTrace();
						speedInt = 0;
					}
				}else{
					try{
						speedSkBr.setProgress(0);
						speedSpnr.setSelection(0);
						String sendMsg = "{\"speed\":0}\n";
						byte[] strBytes = sendMsg.getBytes();//not necessary
						parent.sendCommand(sendMsg);
					}catch(NumberFormatException e){
						e.printStackTrace();
					}
				}
			}
		});
		
		speedSkBr.setOnSeekBarChangeListener(
				 new OnSeekBarChangeListener() {

					@Override
					public void onProgressChanged(SeekBar seekBar, int progressValue,
							boolean fromUser) {
						int speedInt = (progressValue*255)/100;
						speedTxtVw.setText(String.valueOf(progressValue));
						if(centTgl.isChecked()){
							try{
								String sendMsg = "{\"speed\":"+String.valueOf(speedInt)+"}\n";
								//byte[] strBytes = sendMsg.getBytes();
								parent.sendCommand(sendMsg);
							}catch(Exception e){
								e.printStackTrace();
							}
						}
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						speedSpnr.setSelection(0);
						int speedInt = (speedSkBr.getProgress()*255)/100;
						speedTxtVw.setText(String.valueOf(speedSkBr.getProgress()));
						if(centTgl.isChecked()){
							try{
								String sendMsg = "{\"speed\":"+String.valueOf(speedInt)+"}\n";
								//byte[] strBytes = sendMsg.getBytes();
								parent.sendCommand(sendMsg);
							}catch(Exception e){
								e.printStackTrace();
							}
						}
					}
					 
				 });
		
		
		heatTgl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@SuppressLint("NewApi")
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					heatTgl.setBackground(getResources().getDrawable(R.drawable.red_button));
					String thermStr = tempTxtVw.getText().toString();
					//speedTxtVw.setText(speedStr);
					int thermInt = 0;
					if(thermStr.equals(" - - - ")){
						tempSkBr.setProgress(50);
						thermInt = 0;
						String sendMsg = "{\"hst\":50}\n"; 
						parent.sendCommand(sendMsg);
						sendMsg = "{\"heat\":1}\n";
						//byte[] strBytes = sendMsg.getBytes();//not necessary
						parent.sendCommand(sendMsg);
						tempTgl.setChecked(true);
					}else{
					
						try{
							thermInt = Integer.parseInt(thermStr);
							if(thermInt>-19&&thermInt<101) {
								//speedInt = (speedInt*255)/100;
								String sendMsg = "{\"hst\":"+String.valueOf(thermInt)+"}\n"; 
								parent.sendCommand(sendMsg);
								sendMsg = "{\"heat\":1}\n";
								//byte[] strBytes = sendMsg.getBytes();//not necessary
								parent.sendCommand(sendMsg);
								tempTgl.setChecked(true);
							}else{
								ToastMaster("Choose a number from 0-100");
							}
						}catch(NumberFormatException e){
							e.printStackTrace();
							thermInt = 0;
						}
						
					}
				}else{
					heatTgl.setBackground(getResources().getDrawable(R.drawable.green_button));
					try{
						String sendMsg = "{\"heat\":0}\n";
						parent.sendCommand(sendMsg);
						//sendMsg = "{\"hst\":0}\n";
						//byte[] strBytes = sendMsg.getBytes();//not necessary
						//parent.sendCommand(sendMsg);
					}catch(NumberFormatException e){
						e.printStackTrace();
					}
				}
			}
		});
		
		
		tempTgl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					String sendMsg = "{\"temp\":1}\n";
					parent.sendCommand(sendMsg);
				}else{
					String sendMsg = "{\"temp\":0}\n";
					parent.sendCommand(sendMsg);
				}
				
			}
			
			
		});
		
		
		
		tempSkBr.setOnSeekBarChangeListener(
				 new OnSeekBarChangeListener() {

					@Override
					public void onProgressChanged(SeekBar seekBar, int progressValue,
							boolean fromUser) {
						int tempInt = (int) ((progressValue*0.8d)+20);
						tempTxtVw.setText(String.valueOf(tempInt));
						if(heatTgl.isChecked()){
							try{
								String sendMsg = "{\"hst\":"+String.valueOf(tempInt)+"}\n";
								//byte[] strBytes = sendMsg.getBytes();//not necessary
								parent.sendCommand(sendMsg);
							}catch(Exception e){
								e.printStackTrace();
							}
						}
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						//speedSpnr.setSelection(0);
					}
					 
				 });
		
		
		coolTgl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@SuppressLint("NewApi")
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					coolTgl.setBackground(getResources().getDrawable(R.drawable.blue_button));
					String sendMsg = "{\"cool\":1}\n";
					parent.sendCommand(sendMsg);
					fanTgl.setChecked(true);
				}else{
					coolTgl.setBackground(getResources().getDrawable(R.drawable.green_button));
					String sendMsg = "{\"cool\":0}\n";
					parent.sendCommand(sendMsg);
					fanTgl.setChecked(false);
				}
			}
		});
		
		
		fanTgl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					String sendMsg = "{\"fan\":1}\n";
					parent.sendCommand(sendMsg);
				}else{
					String sendMsg = "{\"fan\":0}\n";
					parent.sendCommand(sendMsg);
				}
			}
		});
		
		
		return view;
	}
	
	@Override
	public void onResume(){
		Log.d(TAG,"onResume called");
		tempTgl = (ToggleButton) view.findViewById(R.id.temp_toggle);
		if(parent.isLocked()){
			fanTgl.setVisibility(View.GONE);
		}else{
			fanTgl.setVisibility(View.VISIBLE);
		}
		RelativeLayout backRltvLyt = (RelativeLayout) view.findViewById(R.id.big_back);
		if(parent.isConnected()){
			backRltvLyt.setBackgroundColor(getResources().getColor(R.color.light_green));
		}else{
			backRltvLyt.setBackgroundColor(Color.WHITE);
		}
			
			
		super.onResume();
	}
	
	/* My ToastMaster function to display a messageBox on the screen */
	void ToastMaster(String textToDisplay) {
		Toast myMessage = Toast.makeText(getActivity().getApplicationContext(), 
				textToDisplay, 
				Toast.LENGTH_SHORT);
		myMessage.setGravity(Gravity.CENTER, 0, 0);
		myMessage.show();
	}
	
	
	
	public interface ControlsFragmentListener {
		boolean isLocked();
		
		void sendCommand(String cmd);
		
		void sendReset();
		
		boolean isConnected();
	}
	
	void updateTemp(Bundle b) {
		Log.d(TAG,"updateTemp called");
		if(b.containsKey("temp")) {
			String tp = b.getString("temp");
			if(tp.contains("-999")){
				tp = "["+mostRecentTemp+"]";
			}
			//ToggleButton heartTgl = (ToggleButton) view.findViewById(R.id.heat_toggle);
			if(tempTgl.isChecked()){
				tempTgl.setText(tp);
			}
			tempTgl.setTextOn(b.getString("temp"));
			//ControlsFragment.heatTgl.setTextOn(b.getString("temp"));
		}
	}
	
	public void setBack(){
		Log.d(TAG, "setBack called");
		if(parent!=null){
		if(parent.isConnected()){
			big_back.setBackgroundColor(Color.GREEN);
		}else{
			big_back.setBackgroundColor(Color.WHITE);
		}
		}else{
			Log.d(TAG,"ControlsFagment orphan right now");
		}
	}
	
	@Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if (visible) {
        	if(view!=null){
    			RelativeLayout backRltvLyt = (RelativeLayout) view.findViewById(R.id.big_back);
    			if(parent.isConnected()){
    				backRltvLyt.setBackgroundColor(getResources().getColor(R.color.light_green));
    			}else{
    				backRltvLyt.setBackgroundColor(Color.WHITE);
    			}
    		}else{
    			Log.d(TAG,"view is null");
    		}
        }
    }
	
}
