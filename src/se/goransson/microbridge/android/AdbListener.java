package se.goransson.microbridge.android;

public interface AdbListener {
	public void adbConnected();

	public void adbDisconnected();

	public void adbEvent(byte[] buffer);
}
