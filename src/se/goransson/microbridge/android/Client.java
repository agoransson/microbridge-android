package se.goransson.microbridge.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import android.util.Log;

public class Client {

	private Socket socket;

	private final Server server;

	private final InputStream input;
	private final OutputStream output;

	private boolean keepAlive = true;

	public Client(Server server, Socket socket) throws IOException {
		this.server = server;
		this.socket = socket;
		socket.setKeepAlive(true);

		this.input = this.socket.getInputStream();
		this.output = this.socket.getOutputStream();

		startCommunicationThread();
	}

	public void startCommunicationThread() {
		(new Thread() {
			public void run() {
				while (keepAlive) {
					try {

						// Check for input
						if (input.available() > 0) {

							int bytesRead;
							byte buf[] = new byte[input.available()];
							bytesRead = input.read(buf);

							if (bytesRead == -1)
								keepAlive = false;
							else
								server.receive(Client.this, buf);
						}

					} catch (IOException e) {
						// println(e.toString());
						keepAlive = false;
						Log.d("microbridge", "IOException: " + e);
					}
				}

				// Client exited, notify parent server
				server.disconnectClient(Client.this);
			}
		}).start();
	}

	public void close() {
		keepAlive = false;

		// Close the socket, will throw an IOException in the listener thread.
		try {
			socket.close();
		} catch (IOException e) {
			// println(e.toString());
			Log.e("microbridge", "error while closing socket", e);
		}
	}

	public void send(byte[] data) throws IOException {
		try {
			output.write(data);
			output.flush();
		} catch (SocketException ex) {
			// println(ex.toString());
			// Broken socket, disconnect
			close();
			server.disconnectClient(this);
		}
	}
}
