package se.goransson.microbridge.android;

import java.io.IOException;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Based on the ProcessingAdb library by agoransson.
 * 
 * https://github.com/agoransson/processingadb
 * 
 * @author ksango
 * 
 */
public class MicroBridge {

	protected static final String TAG = "MicroBridge";

	/** Library version */
	public final static String VERSION = "##version##";

	/** ADB Server instance */
	private Server mServer;

	/** ADB is disconnected */
	public static final int STATE_DISCONNECTED = 100;

	/** ADB is connected */
	public static final int STATE_CONNECTED = 101;

	/** Listener reference */
	private AdbListener listener;

	/**
	 * Current state of ADB connection, value should be either
	 * {@link #STATE_CONNECTED} or {@link #STATE_DISCONNECTED}
	 */
	private int state = STATE_DISCONNECTED;

	/** Display DEBUG messages, default is false */
	private boolean debug = false;

	/**
	 * Create the MicroBridge library instance.
	 * 
	 * @param the
	 *            parent context
	 */
	public MicroBridge(AdbListener listener) {
		this.listener = listener;
	}

	/**
	 * Connect to the Adb Device, default port 4567.
	 */
	public void connect() {
		connect(4567);
	}

	/**
	 * Connect to the Adb Device.
	 * 
	 * @param port
	 *            The port on which the Adb Device is running.
	 */
	public void connect(int port) {
		mServer = null;
		try {
			mServer = new Server(eventHandler, port);
			mServer.start();
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
			state = STATE_DISCONNECTED;
		}
	}

	private Handler eventHandler = new Handler(Looper.getMainLooper()) {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Server.SERVER_STARTED:
				if (debug)
					Log.i(TAG, "Adb Server started");
				if (listener != null) {
					listener.adbConnected();
				}
				break;
			case Server.SERVER_STOPPED:
				if (debug)
					Log.i(TAG, "Adb Server stopped");
				break;
			case Server.CLIENT_DISCONNECTED:
				if (debug)
					Log.i(TAG, "Adb Client disconnected");
				if (listener != null) {
					listener.adbDisconnected();
				}
				state = STATE_DISCONNECTED;
				break;
			case Server.CLIENT_CONNECTED:
				if (debug)
					Log.i(TAG, "Adb Client connected");
				state = STATE_CONNECTED;
				break;
			case Server.CLIENT_RECEIVE:
				if (debug)
					Log.i(TAG, "Adb Client received data");
				byte[] data = (byte[]) msg.obj;

				if (data.length < 2)
					return;

				// Invoke the adb-event method with the sensor value as argument
				if (listener != null) {
					listener.adbEvent(data);
				}

				break;
			}
		}
	};

	/**
	 * return the version of the library.
	 * 
	 * @return String
	 */
	public static String version() {
		return VERSION;
	}

	/**
	 * Write a string to the adb device.
	 * 
	 * @param message
	 *            The message to send.
	 */
	public void write(String message) {
		write(message.getBytes());
	}

	/**
	 * Write a character to the adb device.
	 * 
	 * @param c
	 *            The character to send.
	 */
	public void write(char c) {
		write(new byte[] { (byte) c });
	}

	/**
	 * 
	 * @param value
	 *            the value to write
	 */
	public void write(byte[] value) {
		try {
			mServer.send(value);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
		}
	}

	/**
	 * Stop the communication with the adb device.
	 */
	public void stop() {
		mServer.stop();
	}

	/**
	 * Set the debug state for the library
	 * 
	 * @param debug
	 *            true or false
	 */
	public void setDEBUG(boolean debug) {
		this.debug = debug;
	}

	/**
	 * Get the connected state for the ADB connection
	 * 
	 * @return connection state, either {@link #STATE_CONNECTED} or
	 *         {@link #STATE_DISCONNECTED}
	 */
	public int getState() {
		return state;
	}

}
