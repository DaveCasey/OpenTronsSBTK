package com.opentrons.sbtk.Support;



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.opentrons.sbtk.Support.SBTKConfig.SBTKType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class Maquina {
	private static final String TAG = "Maquina";
	private static final String UPDATE_BLOCK_FORMAT = "{\"%s\": {%s}}";
	private static final String UPDATE_SINGLE_FORMAT = "{\"%s\":%s}";
	private static final String UPDATE_VALUE_FORMAT = "\"%s\": %s";
	public static final String axisIndexToName[] = { "x", "y", "z", "a", "b", "c" };
	// Machine state variables
	private Bundle state;
	private Bundle axis[] = new Bundle[6];
	private Bundle motor[] = new Bundle[4];
	private Bundle axisP[] = new Bundle[6];
	private Bundle motorP[] = new Bundle[4];
	private static SBTKConfig machineVars;
	
	private static SharedPreferences settings;
	private static Context mContext;
	private static boolean debug;
	
	public boolean not_finished;
	private String key, value;
	
	public static Vector<MyEntry> myIndex;
	
	private StringBuilder sb = new StringBuilder();
	private ProgressDialog progressBar;
	
	public static StringBuilder generalSB = new StringBuilder();
	
	public Maquina(Context vContext) {
		for (int i = 0; i < 4; i++) {
			motor[i] = new Bundle();
			motorP[i] = new Bundle();
		}
		for (int i = 0; i < 6; i++) {
			axis[i] = new Bundle();
			axisP[i] = new Bundle();
		}
		state = new Bundle();
		machineVars = new SBTKConfig();
		mContext = vContext;
		settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		debug = settings.getBoolean("debug", false);
		
		if(myIndex!=null){
			for(MyEntry entry:myIndex){
				entry.value=false;
			}
		}
		else
			myIndex = new Vector<MyEntry>();
		
		Map<String,?> mp = settings.getAll();
		Iterator<?> it = mp.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry mapEntry = (Map.Entry) it.next();
			key = (String) mapEntry.getKey();
			int i=0;
			for(MyEntry entry:myIndex){
				if(entry.name.equals("key"))break;
				else i++;
			}
			if(i==myIndex.size()){
				MyEntry moo = new MyEntry();
				moo.name = "key";
				moo.value = false;
				myIndex.add(moo);
			}
		}
		
		
	}

	public Bundle getStatusBundle() {
		return state;
	}

	

	
	public List<String> updateSystemBundle(Bundle b) {
		String scratch;
		ArrayList<String> cmds = new ArrayList<String>();

		state.putAll(b);

		for (SBTKConfig.SBTKType v : machineVars.getSys()) {
			if (b.containsKey(v.name)) {
				scratch = "";
				if (v.type.equals("float"))
					scratch = Float.toString(b.getFloat(v.name));
				if (v.type.equals("boolean"))
					scratch = b.getBoolean(v.name) ? "1" : "0";
				if (v.type.equals("string"))
					scratch = b.getString(v.name);
				if (v.type.equals("int"))
					scratch = Integer.toString(b.getInt(v.name));
				cmds.add(String.format(UPDATE_SINGLE_FORMAT, v.name, scratch));
			}
		}

		return cmds;
	}


	private void setQueue(int qr) {
		Log.d(TAG, "qr = " + qr);
		state.putInt("qr", qr);
	}
	
	private void setMessage(JSONObject r) throws JSONException {
		if(r.has("msg"))
			state.putString("msg", r.getString("msg"));
	}
	
	private void setTemp(double tp) {
		Log.d(TAG, "tp = " + String.valueOf(tp));
		state.putString("temp", String.valueOf(tp));
	}
	
	private void setStatus(JSONObject sr) throws JSONException {
		if (sr.has("posx"))
			state.putFloat("posx", (float) sr.getDouble("posx"));
		if (sr.has("posy"))
			state.putFloat("posy", (float) sr.getDouble("posy"));
		if (sr.has("posz"))
			state.putFloat("posz", (float) sr.getDouble("posz"));
		if (sr.has("posa"))
			state.putFloat("posa", (float) sr.getDouble("posa"));
		if (sr.has("vel"))
			state.putFloat("velocity", (float) sr.getDouble("vel"));
		if (sr.has("line"))
			state.putInt("line", sr.getInt("line"));
		if (sr.has("momo"))
			switch (sr.getInt("momo")) {
			case 0:
				state.putString("momo", "seek");
				break;
			case 1:
				state.putString("momo", "feed");
				break;
			case 2:
				state.putString("momo", "cw_arc");
				break;
			case 3:
				state.putString("momo", "ccw_arc");
				break;
			case 4:
				state.putString("momo", "cancel");
				break;
			case 5:
				state.putString("momo", "probe");
				break;
			default:
				state.putString("momo", Integer.toString(sr.getInt("momo")));
				break;
			}

		if (sr.has("stat"))
			switch (sr.getInt("stat")) {
			case 0:
				state.putString("status", "init");
				break;
			case 1:
				state.putString("status", "ready");
				break;
			case 2:
				state.putString("status", "shutdown");
				break;
			case 3:
				state.putString("status", "stop");
				break;
			case 4:
				state.putString("status", "end");
				break;
			case 5:
				state.putString("status", "run");
				break;
			case 6:
				state.putString("status", "hold");
				break;
			case 7:
				state.putString("status", "probe");
				break;
			case 8:
				state.putString("status", "cycle");
				break;
			case 9:
				state.putString("status", "homing");
				break;
			case 10:
				state.putString("status", "jog");
				break;
			}

		if (sr.has("unit")) {
			switch (sr.getInt("unit")) {
			case 0:
				state.putString("units", "inches");
				break;
			case 1:
				state.putString("units", "mm");
				break;
			case 2:
				state.putString("units", "degrees");
				break;
			}
		}
		
		if (sr.has("temp")){
			state.putString("temp", sr.getString("temp"));
		}
		if (sr.has("echo")){
			state.putString("echo", sr.getString("echo"));
		}
		
	}

	private void putSys(JSONObject sysjson) throws JSONException {
		for (SBTKConfig.SBTKType v : machineVars.getSys()) {
			if (sysjson.has(v.name)) {
				if (v.type.equals("float"))
					state.putFloat(v.name, (float) sysjson.getDouble(v.name));
				if (v.type.equals("boolean"))
					state.putBoolean(v.name, sysjson.getInt(v.name) == 1);
				if (v.type.equals("int"))
					state.putInt(v.name, sysjson.getInt(v.name));
				if (v.type.equals("string"))
					state.putString(v.name, sysjson.getString(v.name));
			}
		}
	}

	

	
	
	
	public Bundle processJSON(String string) {
		Bundle bResult = null;
		try {
			JSONObject json = new JSONObject(string);

			if (json.has("r")) {
				if(debug){
					if(string.contains("xzb")){
						String stop = "here";
					}
				}
				if(json.has("f")) {
					bResult = processFooter(json.getJSONArray("f"));
					int check = bResult.getInt("checksum");
					if(checksumTest(string,check)==false) {
					}
					
					switch(bResult.getInt("status")) {
						case 0: // OK
						case 3: // NOOP
						case 60: // NULL move
						break;
						
						default:
							Log.e(TAG, "Status code error: " + string);
							bResult = new Bundle();
							bResult.putString("json", "error");
							bResult.putString("error", "wtf1");
							return bResult;
					}
				} else {
					bResult = new Bundle();
				}
				
				bResult.putAll(processBody(string, json.getJSONObject("r")));
				bResult.putString("nwags", string);
				return bResult;
			}
			if (json.has("sr")) {
				bResult = processStatusReport(json.getJSONObject("sr"));
				bResult.putString("nwags", string);
				return bResult;
			}
			if (json.has("qr")) {
				bResult = processQueueReport(json.getInt("qr"));
				bResult.putString("nwags", string);
				return bResult;
			}
			if (json.has("temp")){
				bResult = processTempReport(json.getDouble("temp"));
				bResult.putString("nwags", string);
				return bResult;
			}
		} catch (Exception e) {
			if(e.getMessage()!=null)
				Log.e(TAG, e.getMessage());
			bResult = new Bundle();
			bResult.putString("json", "error");
			bResult.putString("error", e.getMessage());
			bResult.putString("nwags", "");
		}
		return bResult;
	}

	// [<protocol_version>, <status_code>, <input_available>, <checksum>]
	private Bundle processFooter(JSONArray json) throws JSONException,
			NumberFormatException {
		Bundle b = new Bundle();
		b.putInt("protocol", json.getInt(0));
		b.putInt("status", json.getInt(1));
		if(json.getInt(1)==48){
			String stop;
			stop = "here";
		}
		b.putInt("buffer", json.getInt(2));
		b.putInt("checksum", Integer.parseInt(json.getString(3)));
		return b;
	}

	private Bundle processBody(String json_string, JSONObject json)
			throws JSONException {
		Bundle fResult=null;
		if(json.has("f")) {
			fResult = processFooter(json.getJSONArray("f"));
			int check = fResult.getInt("checksum");
			if(checksumTest(json_string, check) == false) {
			}
			
			switch(fResult.getInt("status")) {
				case 0:	// OK
				case 3:	// NOOP
				case 60: // NULL move
				break;
				
				default:
					Log.e(TAG, "Status code error: " + json_string);
					fResult = new Bundle();
					fResult.putString("json","error");
					fResult.putString("error", "wtf2");
					return fResult;
			}
		} else {
			fResult = new Bundle();
		}
		fResult.putAll(getStatusBundle());
		//Bundle fResult = processFooter(json.getJSONArray("f"));
		if (json.has("msg")){
			fResult.putAll(processMessage(json));
			return fResult;
		}
		
		
		
		if (json.has("sr"))
			fResult.putAll(processStatusReport(json.getJSONObject("sr")));
		if (json.has("qr"))
			fResult.putAll(processQueueReport(json.getInt("qr")));
		if (json.has("sys")){
			fResult.putAll(processSys(json.getJSONObject("sys")));
		} else if (json.has("fb")||json.has("fv")||json.has("hv")||json.has("id")||json.has("ja")||json.has("ct")||
					json.has("st")||json.has("ej")||json.has("jv")||json.has("tv")||json.has("qv")||json.has("sv")||
					json.has("si")||json.has("ic")||json.has("ec")||json.has("ee")||json.has("ex")||json.has("gpl")||
					json.has("gun")||json.has("gco")||json.has("gpa")||json.has("gdi")||
					json.has("nwa")||json.has("nwadt")||json.has("nwae")||json.has("nwsal"))
		{
			fResult.putAll(processSys(json));
		}
		
		Iterator<?> jist = json.keys();
		while(jist.hasNext()){
			String jill = jist.next().toString();
			/*if (jill.startsWith("1")){
				if(jill.length()==1)
					fResult.putAll(processMotor(1, json.getJSONObject(jill)));
				else{
					JSONObject job = new JSONObject();
					job.put(jill.substring(1),json.get(jill));
					fResult.putAll(processMotor(1,job));
					fResult.putAll(getMotorBundleP(1));
				}
			}*/
			
		}
		
		return fResult;
	}
	
	private Bundle processMessage(JSONObject r) throws JSONException {
		setMessage(r);
		Bundle b = getStatusBundle();
		b.putString("json", "sr");
		return b;
	}
	
	private Bundle processStatusReport(JSONObject sr) throws JSONException {
		setStatus(sr);
		Bundle b = getStatusBundle();
		b.putString("json", "sr");
		return b;
	}

	private Bundle processQueueReport(int qr) {
		setQueue(qr);
		Bundle b = getStatusBundle();
		b.putString("json", "qr");
		return b;
	}
	
	private Bundle processTempReport(double tp) {
		setTemp(tp);
		Bundle b = getStatusBundle();
		b.putString("json", "temp");
		return b;
	}

	private Bundle processSys(JSONObject sys) throws JSONException {
		putSys(sys);
		Bundle b = getStatusBundle();
		b.putString("json", "sys");
		return b;
	}

	
	private boolean checksumTest(String s, int val) {
		// Check checksum
		int pos = s.lastIndexOf(",");
		if (pos == -1) // Shouldn't be possible!
			return false;
		String subval = s.substring(0, pos);
		long y = (subval.hashCode() & 0x00000000ffffffffL) % 9999;
		if (y != val) {
			Log.e(TAG, "Checksum error for: " + s + " (" + y + "," + val + ")");
			return false;
		}
		return true;
	}
	
	public void loadFloat(String mKey, String sKey, int group, String backup, int category) {
		Bundle update = new Bundle();
		String str = settings.getString(sKey, backup);
		value = str;
		Float floaty = (float) 0.0;
		try{
			floaty = Float.parseFloat(str);
		}catch(NumberFormatException e){
			e.printStackTrace();
		}
		update.putFloat(mKey, floaty);
		switch(category){
		case 0:
			//updateAxisBundle(group, update);
			break;
		case 1:
			//updateMotorBundle(group, update);
			break;
		case 2:
			updateSystemBundle(update);
			break;
		}
	}
	
	public void loadInteger(String mKey, String sKey, int group, String backup, int category){
		Bundle update = new Bundle();
		String str = settings.getString(sKey, backup);
		value = str;
		Integer inty = 0;
		try{
			inty = Integer.parseInt(str);
		}catch(NumberFormatException e){
			e.printStackTrace();
		}
		update.putInt(mKey, inty);
		switch(category){
		case 0:
			//updateAxisBundle(group, update);
			break;
		case 1:
			//updateMotorBundle(group, update);
			break;
		case 2:
			updateSystemBundle(update);
			break;
		}
	}
	
	public void loadBoolean(String mKey, String sKey, int group, boolean backup, int category){
		Bundle update = new Bundle();
		boolean booly = settings.getBoolean(sKey, backup);
		generalSB.setLength(0);
		generalSB.append(booly);
		value = generalSB.toString();
		update.putBoolean(mKey, booly);
		switch(category){
		case 0:
			//updateAxisBundle(group, update);
			break;
		case 1:
			//updateMotorBundle(group, update);
			break;
		case 2:
			updateSystemBundle(update);
			break;
		}
	}
	
	public void loadString(String mKey, String sKey, int group, String backup, int category){
		Bundle update = new Bundle();
		String str = settings.getString(sKey, backup);
		value = str;
		
		update.putString(mKey, str);
		switch(category){
		case 0:
			//updateAxisBundle(group, update);
			break;
		case 1:
			//updateMotorBundle(group, update);
			break;
		case 2:
			updateSystemBundle(update);
			break;
		}
	}
	
	
	
	public boolean loadPreferenceSettings(){
		boolean result = false;
		Map<String,?> mp = settings.getAll();
		Iterator<?> it = mp.entrySet().iterator();
		
		while(it.hasNext()) {
			Map.Entry mapEntry = (Map.Entry) it.next();
			key = (String) mapEntry.getKey();
			if(debug)
				Log.d(TAG,"pre-loading setting: "+key+"="+String.valueOf(mapEntry.getValue()));
			
	/*ma*/		if(key.equals("1ma")||key.equals("2ma")||key.equals("3ma")||key.equals("4ma")) {
				int mnum = 0;
				if(key.equals("1ma")){
					mnum = 1;
					loadInteger("ma",key,mnum,"0",1);
				}else if(key.equals("2ma")){
					mnum = 2;
					loadInteger("ma",key,mnum,"1",1);
				}else if(key.equals("3ma")){
					mnum = 3;
					loadInteger("ma",key,mnum,"2",1);
				}else if(key.equals("4ma")){
					mnum = 4;
					loadInteger("ma",key,mnum,"3",1);
				}
				//helpAMSInty("ma",key,mnum,"",1);
				
	/*mi*/		} else if(key.equals("1mi")||key.equals("2mi")||key.equals("3mi")||key.equals("4mi")) {
				int mnum = 0;
				if(key.equals("1mi")){
					mnum = 1;
				}else if(key.equals("2mi")){
					mnum = 2;
				}else if(key.equals("3mi")){
					mnum = 3;
				}else if(key.equals("4mi")){
					mnum = 4;
				}
				loadInteger("mi",key,mnum,"8",1);
				
	/*tr*/		} else if(key.equals("1tr")||key.equals("2tr")||key.equals("3tr")||key.equals("4tr")) {
				int mnum = 0;
				if(key.equals("1tr")){
					mnum = 1;
					loadFloat("tr",key,mnum,"36.54",1);
				}else if(key.equals("2tr")){
					mnum = 2;
					loadFloat("tr",key,mnum,"36.54",1);
				}else if(key.equals("3tr")){
					mnum = 3;
					loadFloat("tr",key,mnum,"8.1",1);
				}else if(key.equals("4tr")){
					mnum = 4;
					loadFloat("tr",key,mnum,"360",1);
				}
				//helpAMSFloaty("tr",key,mnum,"",1);
				
	/*sa*/		} else if(key.equals("1sa")||key.equals("2sa")||key.equals("3sa")||key.equals("4sa")) {
				int mnum = 0;
				if(key.equals("1sa")){
					mnum = 1;
				}else if(key.equals("2sa")){
					mnum = 2;
				}else if(key.equals("3sa")){
					mnum = 3;
				}else if(key.equals("4sa")){
					mnum = 4;
				}
				loadFloat("sa",key,mnum,"1.8",1);
				
	/*po*/		} else if(key.equals("1po")||key.equals("2po")||key.equals("3po")||key.equals("4po")) {
				Bundle update = new Bundle();
				int mnum = 0;
				if(key.equals("1po")){
					mnum = 1;
					update.putBoolean("po", settings.getBoolean(key, false));
					value = String.valueOf(settings.getBoolean(key, false));
				}else if(key.equals("2po")){
					mnum = 2;
					update.putBoolean("po", settings.getBoolean(key, true));
					value = String.valueOf(settings.getBoolean(key, true));
				}else if(key.equals("3po")){
					mnum = 3;
					update.putBoolean("po", settings.getBoolean(key, true));
					value = String.valueOf(settings.getBoolean(key, true));
				}else if(key.equals("4po")){
					mnum = 4;
					update.putBoolean("po", settings.getBoolean(key, true));
					value = String.valueOf(settings.getBoolean(key, true));
				}
				
				loadBoolean("pm",key,mnum,false,1);
				
	/*pm*/		} else if(key.equals("1pm")||key.equals("2pm")||key.equals("3pm")||key.equals("4pm")) {
				Bundle update = new Bundle();			
				int mnum = 0;
				if(key.equals("1pm")){
					mnum = 1;
					update.putBoolean("pm", settings.getBoolean(key, false));
					value = String.valueOf(settings.getBoolean(key, false));
				}else if(key.equals("2pm")){
					mnum = 2;
					update.putBoolean("pm", settings.getBoolean(key, false));
					value = String.valueOf(settings.getBoolean(key, false));
				}else if(key.equals("3pm")){
					mnum = 3;
					update.putBoolean("pm", settings.getBoolean(key, false));
					value = String.valueOf(settings.getBoolean(key, false));
				}else if(key.equals("4pm")){
					mnum = 4;
					update.putBoolean("pm", settings.getBoolean(key, true));
					value = String.valueOf(settings.getBoolean(key, true));
				}			
				
				loadBoolean("pm",key,mnum,false,1);
				
	/*am*/		} 
			 // axis 
			 else if(key.equals("xam")||key.equals("yam")||key.equals("zam")||
					 key.equals("aam")||key.equals("bam")||key.equals("cam")) {
				int a = 0;
				if(key.equals("xam")){
					a = 0;
					loadInteger("am",key,a,"1",0);
				}else if(key.equals("yam")){
					a = 1;
					loadInteger("am",key,a,"1",0);
				}else if(key.equals("zam")){
					a = 2;
					loadInteger("am",key,a,"1",0);
				}else if(key.equals("aam")){
					a = 3;
					loadInteger("am",key,a,"3",0);
				}else if(key.equals("bam")){
					a = 4;
					loadInteger("am",key,a,"0",0);
				}else if(key.equals("cam")){
					a = 5;
					loadInteger("am",key,a,"0",0);
				}
				//helpAMSInty("am",key,a,"",0);
				
			} else if(key.equals("xsn")||key.equals("ysn")||key.equals("zsn")||
					  key.equals("asn")||key.equals("bsn")||key.equals("csn")) {
				int a = 0;
				if(key.equals("xsn")){
					a = 0;
				}else if(key.equals("ysn")){
					a = 1;
				}else if(key.equals("zsn")){
					a = 2;
				}else if(key.equals("asn")){
					a = 3;
				}else if(key.equals("bsn")){
					a = 4;
				}else if(key.equals("csn")){
					a = 5;
				}
				loadInteger("sn",key,a,"3",0);
				
			} else if(key.equals("xsx")||key.equals("ysx")||key.equals("zsx")||
						key.equals("asx")||key.equals("bsx")||key.equals("csx")) {
				int a = 0;
			    if(key.equals("xsx")){
					a = 0;
				}else if(key.equals("ysx")){
					a = 1;
				}else if(key.equals("zsx")){
					a = 2;
				}else if(key.equals("asx")){
					a = 3;
				}else if(key.equals("bsx")){
					a = 4;
				}else if(key.equals("csx")){
					a = 5;
				}
			    loadInteger("sx",key,a,"",0);
			    
	/*vm*/		} else if(key.equals("xvm")||key.equals("yvm")||key.equals("zvm")||
					  key.equals("avm")||key.equals("bvm")||key.equals("cvm")) {
				int a = 0;
				if(key.equals("xvm")){
					a = 0;
					loadFloat("vm",key,a,"10240",0);
				}else if(key.equals("yvm")){
					a = 1;
					loadFloat("vm",key,a,"10240",0);
				}else if(key.equals("zvm")){
					a = 2;
					loadFloat("vm",key,a,"10240",0);
				}else if(key.equals("avm")){
					a = 3;
					loadFloat("vm",key,a,"36000",0);
				}else if(key.equals("bvm")){
					a = 4;
				}else if(key.equals("cvm")){
					a = 5;
				}	
				
			} else if(key.equals("xra")||key.equals("yra")||key.equals("zra")||
					  key.equals("ara")||key.equals("bra")||key.equals("cra")) {
				int a = 0;
			    if(key.equals("xra")){
					a = 0;
				}else if(key.equals("yra")){
					a = 1;
				}else if(key.equals("zra")){
					a = 2;
				}else if(key.equals("ara")){
					a = 3;
					loadFloat("ara",key,a,"0.3183099",0);
				}else if(key.equals("bra")){
					a = 4;
				}else if(key.equals("cra")){
					a = 5;
				}
			    //helpAMSFloaty("ra",key,a,"",0);
			    
			} else if(key.equals("xzb")||key.equals("yzb")||key.equals("zzb")||
					  key.equals("azb")||key.equals("bzb")||key.equals("czb")) {
				int a = 0;
			    if(key.equals("xzb")) {
					a = 0;
				}else if(key.equals("yzb")) {
					a = 1;
				}else if(key.equals("zzb")) {
					a = 2;
				}else if(key.equals("azb")) {
					a = 3;
				}else if(key.equals("bzb")) {
					a = 4;
				}else if(key.equals("czb")) {
					a = 5;
				}
			    loadFloat("zb",key,a,"",0);
			    
	/*tm*/		} else if(key.equals("xtm")||key.equals("ytm")||key.equals("ztm")||
					  key.equals("atm")||key.equals("btm")||key.equals("ctm")) {
				int a = 0;
				if(key.equals("xtm")){
					a = 0;
				}else if(key.equals("ytm")){
					a = 1;
				}else if(key.equals("ztm")){
					a = 2;
				}else if(key.equals("atm")){
					a = 3;
				}else if(key.equals("btm")){
					a = 4;
				}else if(key.equals("ctm")){
					a = 5;
				}
				loadFloat("tm",key,a,"150",0);
				
	/*jm*/		} else if(key.equals("xjm")||key.equals("yjm")||key.equals("zjm")||
					  key.equals("ajm")||key.equals("bjm")||key.equals("cjm")) {
				int a = 0;
				if(key.equals("xjm")){
					a = 0;
					loadFloat("jm",key,a,"100000000",0);
				}else if(key.equals("yjm")){
					a = 1;
					loadFloat("jm",key,a,"100000000",0);
				}else if(key.equals("zjm")){
					a = 2;
					loadFloat("jm",key,a,"75000000",0);
				}else if(key.equals("ajm")){
					a = 3;
					loadFloat("jm",key,a,"200000000",0);
				}else if(key.equals("bjm")){
					a = 4;
				}else if(key.equals("cjm")){
					a = 5;
				}		
				//helpAMSFloaty("jm",key,a,"",0);
				
			} else if(key.equals("xjh")||key.equals("yjh")||key.equals("zjh")||
					  key.equals("ajh")||key.equals("bjh")||key.equals("cjh")) {
				int a = 0;
			    if(key.equals("xjh")){
					a = 0;
				}else if(key.equals("yjh")){
					a = 1;
				}else if(key.equals("zjh")){
					a = 2;
				}else if(key.equals("ajh")){
					a = 3;
				}else if(key.equals("bjh")){
					a = 4;
				}else if(key.equals("cjh")){
					a = 5;
				}
			} else if(key.equals("xjd")||key.equals("yjd")||key.equals("zjd")||
					  key.equals("ajd")||key.equals("bjd")||key.equals("cjd")) {
				int a = 0;
			     if(key.equals("xjd")){
					a = 0;
				}else if(key.equals("yjd")){
					a = 1;
				}else if(key.equals("zjd")){
					a = 2;
				}else if(key.equals("ajd")){
					a = 3;
				}else if(key.equals("bjd")){
					a = 4;
				}else if(key.equals("cjd")){
					a = 5;
				}
			     loadFloat("jd",key,a,"0.05",0);
				
			} else if(key.equals("xlv")||key.equals("ylv")||key.equals("zlv")||
					  key.equals("alv")||key.equals("blv")||key.equals("clv")) {
				int a = 0;
			    if(key.equals("xlv")){
					a = 0;
				}else if(key.equals("ylv")){
					a = 1;
				}else if(key.equals("zlv")){
					a = 2;
				}else if(key.equals("alv")){
					a = 3;
				}else if(key.equals("blv")){
					a = 4;
				}else if(key.equals("clv")){
					a = 5;
				}
			    loadFloat("lv",key,a,"100",0);
			    
			} else if(key.equals("xsv")||key.equals("ysv")||key.equals("zsv")||
					  key.equals("asv")||key.equals("bsv")||key.equals("csv")) {
				int a = 0;
				if(key.equals("xsv")){
					a = 0;
					loadFloat("sv",key,a,"500",0);
				}else if(key.equals("ysv")){
					a = 1;
					loadFloat("sv",key,a,"500",0);
				}else if(key.equals("zsv")){
					a = 2;
					loadFloat("sv",key,a,"500",0);
				}else if(key.equals("asv")){
					a = 3;
					loadFloat("sv",key,a,"600",0);
				}else if(key.equals("bsv")){
					a = 4;
				}else if(key.equals("csv")){
					a = 5;
				}
				//helpAMSFloaty("sv",key,a,"400",0);
			
			} else if(key.equals("xfr")||key.equals("yfr")||key.equals("zfr")||
					  key.equals("afr")||key.equals("bfr")||key.equals("cfr")) {
				int a = 0;
		      	if(key.equals("xfr")){
					a = 0;
					loadFloat("fr",key,a,"10240",0);
				}else if(key.equals("yfr")){
					a = 1;
					loadFloat("fr",key,a,"10240",0);
				}else if(key.equals("zfr")){
					a = 2;
					loadFloat("fr",key,a,"10240",0);
				}else if(key.equals("afr")){
					a = 3;
					loadFloat("fr",key,a,"36000",0);
				}else if(key.equals("bfr")){
					a = 4;
				}else if(key.equals("cfr")){
					a = 5;
				}
				//helpAMSFloaty("fr",key,a,"",0);
			} 
			// NWs
			else if(key.equals("nwa")) {
				loadString("nwa",key,0,"XYZA",2);
			} else if(key.equals("nwae")) {
				loadFloat("nwae",key,0,"18.0",2);
			} else if(key.equals("nwadt")) {
				loadFloat("nwadt",key,0,"22.0",2);
			} else if(key.equals("nwsal")) {
				loadInteger("nwsal",key,0,"0",2);
			}
			 // system
			 else if(key.equals("hv")) {
				loadInteger("hv",key,0,"8.00",2);
			} else if(key.equals("ja")) {
				loadFloat("ja",key,0,"100000",2);
			} else if(key.equals("ct")) {
				loadFloat("ct",key,0,"0.001",2);
			} else if(key.equals("st")) {
				loadInteger("st",key,0,"1",2);
			} else if(key.equals("ej")) {
				loadBoolean("ej",key,0,false,2);
			} else if(key.equals("jv")) {
				loadInteger("jv",key,0,"5",2);
			} else if(key.equals("tv")) {
				loadInteger("tv",key,0,"1",2);
			} else if(key.equals("qv")) {
				loadInteger("qv",key,0,"2",2);
			} else if(key.equals("sv")) {
				loadInteger("sv",key,0,"1",2);
			} else if(key.equals("si")) {
				loadInteger("si",key,0,"250",2);
			} else if(key.equals("ic")) {
				loadInteger("ic",key,0,"0",2);
			} else if(key.equals("ec")) {
				loadBoolean("ec",key,0,false,2);
			} else if(key.equals("ee")) {
				loadBoolean("ee",key,0,false,2);
			} else if(key.equals("ex")) {
				loadInteger("ex",key,0,"0",2);
			} else if(key.equals("gpl")) {
				loadInteger("gpl",key,0,"0",2);
			} else if(key.equals("gun")) {
				loadBoolean("gun",key,0,true,2);
			} else if(key.equals("gco")) {
				loadInteger("gco",key,0,"1",2);
			} else if(key.equals("gpa")) {
				loadInteger("gpa",key,0,"2",2);
			} else if(key.equals("gdi")) {
				loadInteger("gdi",key,0,"0",2);
			}
			else{
				if(debug)
					Log.d(TAG,"not loaded");
			}
		}
		
		
		return result;
	}
	
	
	

	
	public class MyEntry{
		public String name;
		public boolean value;
	}
}

