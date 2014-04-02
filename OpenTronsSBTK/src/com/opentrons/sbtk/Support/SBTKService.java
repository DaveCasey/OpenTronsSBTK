package com.opentrons.sbtk.Support;


import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

abstract public class SBTKService extends Service {
	public static final String CMD_HEAT_OFF = "{\"heat\":0}\n";
	public static final String CMD_COOL_OFF = "{\"cool\":0}\n";
	public static final String CMD_FAN_OFF = "{\"fan\":0}\n";
	public static final String CMD_TEMP_OFF = "{\"temp\":0}\n";
	
	
	
	// buffer size on TinyG
	public static final int TINYG_BUFFER_SIZE = 254;

	// broadcast messages when we get updated data
	public static final String STATUS = "com.opentrons.sbtk.STATUS";
	public static final String SYSTEM = "com.opentrons.sbtk.SYSTEM";
	public static final String JSON_ERROR = "com.opentrons.sbtk.JSON_ERROR";
	public static final String CONNECTION_STATUS = "com.opentrons.sbtk.CONNECTION_STATUS";
	public static final String RAWS = "com.opentrons.sbtk.RAWS";
	public static final String TEMP = "com.opentrons.sbtk.TEMP";
	
	
	protected static final String TAG = "SBTKService";
	protected Maquina machine;
	private final Semaphore serialBufferAvail = new Semaphore(
			TINYG_BUFFER_SIZE, true);
	protected final Semaphore writeLock = new Semaphore(1, true);
	private final BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
	private final IBinder mBinder = new SBTKBinder();
	private final QueueProcessor procQ = new QueueProcessor();
	private Thread dequeueWorker;
	private boolean paused = false;
	private volatile boolean flushed;
	//private BlackBox ioLog;
	private SharedPreferences settings;

	
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		machine = new Maquina(getApplicationContext());
	}

	@Override
	public void onDestroy() {
		disconnect();
	}

	public Maquina getMachine() {
		return machine;
	}

	public void connect() {
		if (dequeueWorker == null || !dequeueWorker.isAlive()) {
			dequeueWorker = new Thread(procQ);
			dequeueWorker.start();
		}	

		paused = false;
		flushed = false;

		//ioLog = new BlackBox();

		//settings = PreferenceManager.getDefaultSharedPreferences(this);
		logging();
}

	abstract protected void write(String cmd);
	abstract protected void write(byte b[]);
	abstract protected void write(byte b[], int length);

	public void logging() {
		/*
		if (settings.getBoolean("debug", false))
			if(ioLog!=null)
				ioLog.open();
		else 
			if(ioLog!=null)
				ioLog.close();
				*/
	}

	public void disconnect() {
		// Let everyone know we are disconnected
		Bundle b = new Bundle();
		b.putBoolean("connection", false);
		Intent i = new Intent(CONNECTION_STATUS);
		i.putExtras(b);
		sendBroadcast(i, null);
		int inuse = TINYG_BUFFER_SIZE - serialBufferAvail.availablePermits();
		if (inuse > 0)
			serialBufferAvail.release(inuse);
		queue.clear();
		writeLock.release();
		if (dequeueWorker != null)
			dequeueWorker.interrupt();
		dequeueWorker = null;
		//ioLog.close();
		Log.d(TAG, "disconnect done");
	}

	public class SBTKBinder extends Binder {
		public SBTKService getService() {
			return SBTKService.this;
		}
	}

	@SuppressLint("DefaultLocale")
	public static String short_jog(String axis, double step) {
		return String.format("g91g0%s%f", axis, step);
	}

	public void send_gcode(String gcode) {
		send_message("{\"gc\": \"" + gcode + "\"}\n"); // In verbose mode
														// 2, only the f is
														// returned for gc
	}

	public void send_command(String cmd) {
		//write(cmd);
		send_message(cmd);
	}
	
	public int getInUse(){
		return TINYG_BUFFER_SIZE - serialBufferAvail.availablePermits();
	}
	
	public void cleanse(){
		try{
			int inuse = TINYG_BUFFER_SIZE - serialBufferAvail.availablePermits();
			if (inuse > 0)
				serialBufferAvail.release(inuse);
			if (!queue.isEmpty()) {
				queue.clear();
				flushed = true;
			}
			paused = false;
			writeLock.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	// Enqueue a command
	public void send_message(String cmd) {
		try {
			Log.d(TAG, "adding " + cmd);
			queue.put(cmd);
		} catch (InterruptedException e) {
			// This really shouldn't happen
			e.printStackTrace();
		}
	}

	// Pause can be completed by either a resume or a flush.
	public void send_stop() {
		Log.d(TAG, "in send_stop()");
		send_pause();
		send_flush();
	}

	public void send_pause() {
		if (!paused) {
			try {
				writeLock.acquire();
				Log.d(TAG, "sending feedhold");
				write("!");
				//ioLog.write("* ", "!\n");
				paused = true;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void send_flush() {
			try {
				if (!paused)
					writeLock.acquire();
				Log.d(TAG, "sending queue flush");
				write("%");
				//ioLog.write("* ", "%\n");

				Log.d(TAG, "permits: " + serialBufferAvail.availablePermits());
				int inuse = TINYG_BUFFER_SIZE - serialBufferAvail.availablePermits();
				if (inuse > 0)
					serialBufferAvail.release(inuse);
				if (!queue.isEmpty()) {
					queue.clear();
					flushed = true;
				}
				paused = false;
				writeLock.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public void send_resume() {
		if (paused) {
			Log.d(TAG, "Sending cycle start");
			write("~");
			//ioLog.write("* ", "~\n");
			paused = false;
			writeLock.release();
		}
	}

	public void send_reset() {
		byte[] rst = {0x18};

		Log.d(TAG, "in send_reset()");
		try {
			writeLock.acquire();
			Log.d(TAG, "sending reset");
			//ioLog.write("* ", "RESET\n");
			write(rst);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		int inuse = TINYG_BUFFER_SIZE - serialBufferAvail.availablePermits();
		if (inuse > 0)
			serialBufferAvail.release(inuse);
		if (!queue.isEmpty()) {
			queue.clear();
			flushed = true;
		}
		writeLock.release();
		refresh();
	}
	
	
	// apply any new values in the bundle to the machine state
	// and sends the necessary change commands to Module
	public void putSystem(Bundle b) {
		List<String> cmds = machine.updateSystemBundle(b);

		for (String cmd : cmds) {
			Log.d(TAG, "update system command: " + cmd);
			send_message(cmd + "\n");
		}
	}
	
	public Bundle getMachineStatus() {
		return machine.getStatusBundle();
	}

	public int queueSize() {
		return queue.size();
	}

	protected void updateInfo(String line, Bundle b) {
		String json = b.getString("json");
		Intent i;

		//ioLog.write("< ", line + "\n");
		if (json != null) {
			if (json.equals("sr")) {
				i = new Intent(STATUS);
				i.putExtras(b);
				sendBroadcast(i, null);
			}
			if (json.equals("error")) {
				//ioLog.write("* ", "Parse error on JSON line\n");
				i = new Intent(JSON_ERROR);
				i.putExtras(b);
				sendBroadcast(i, null);
			}
			if (json.equals("sys")) {
				i = new Intent(SYSTEM);
				i.putExtras(b);
				sendBroadcast(i, null);
			}
			if (json.equals("temp")) {
				i = new Intent(TEMP);
				i.putExtras(b);
				sendBroadcast(i, null);
			}
		}
		int freed = b.getInt("buffer");
		if (freed > 0)
			serialBufferAvail.release(freed);
	}
	
	protected void sendRaw(String values){
		//Toast.makeText(this, values, Toast.LENGTH_SHORT).show();
		String raws = "rawness";
		Log.d(TAG, raws);
		Bundle bun = new Bundle();
		bun.putString(raws, values);
		Intent i = new Intent(RAWS);
		i.putExtras(bun);
		sendBroadcast(i,null);
	}
	// Asks for the service to send a full update of all state.
	public void refresh() {
		send_message(CMD_HEAT_OFF);
		send_message(CMD_COOL_OFF);
		send_message(CMD_FAN_OFF);
		send_message(CMD_TEMP_OFF);
	}

	private class QueueProcessor implements Runnable {
		public void run() {
			try {
				while (true) {
					String cmd = queue.take();
					serialBufferAvail.acquire(cmd.length());
					flushed = false;
					writeLock.acquire();
					if (flushed) { // Don't write that last command if we wiped the queue
						Log.d(TAG, "Skipping command line");
						flushed = false;
					} else {
						//ioLog.write("> ", cmd);
						write(cmd);
					}
					serialBufferAvail.release(cmd.length());	// THIS IS SPECIAL BECAUASE NO FOOTERS COMING BACK TO TELL US HOW MUCH TO RELEASE
					writeLock.release();
					
				}
			} catch (InterruptedException e) {
				Log.d(TAG, "Exiting queue processor");
			}
		}
	}

	
	
}