package se.goransson.microbridge.android;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CopyOnWriteArrayList;

import android.os.Handler;

/**
 * Lightweight TCP server that supports multiple clients connecting on a given
 * port.
 * 
 * @author Niels Brouwers
 * 
 */
public class Server {

	// Message constants
	public final static int SERVER_STARTED = 1;
	public final static int SERVER_STOPPED = 2;
	public final static int CLIENT_DISCONNECTED = 3;
	public final static int CLIENT_CONNECTED = 4;
	public final static int CLIENT_RECEIVE = 5;

	// Server socket for the TCP connection
	private ServerSocket serverSocket = null;

	// TCP port to use
	private final int port;

	// List of connected clients. Concurrency-safe arraylist because Clients can
	// join/leave at any point,
	// which means inserts/removes can occur at any time from different threads.
	private CopyOnWriteArrayList<Client> clients = new CopyOnWriteArrayList<Client>();

	// Indicates that the main server loop should keep running.
	private boolean keepAlive = true;

	// Main thread.
	private Thread listenThread;

	// Client message handler
	private Handler clientHandler;

	/**
	 * Constructs a new server instance on default port (4567).
	 * 
	 * @param clientHandler
	 *            Handler to receive messages.
	 */
	public Server(Handler clientHandler) {
		this(clientHandler, 4567);
	}

	/**
	 * Constructs a new server instance.
	 * 
	 * @param port
	 *            TCP port to use.
	 * @param clientHandler
	 *            Handler to receive messages.
	 */
	public Server(Handler clientHandler, int port) {
		this.clientHandler = clientHandler;
		this.port = port;
	}

	/**
	 * @return TCP port this server accepts connections on.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return true iff the server is running.
	 */
	public boolean isRunning() {
		return listenThread != null && listenThread.isAlive();
	}

	/**
	 * @return the number of currently connected clients
	 */
	public int getClientCount() {
		return clients.size();
	}

	/**
	 * Starts the server.
	 * 
	 * @throws IOException
	 */
	public void start() throws IOException {

		keepAlive = true;
		serverSocket = new ServerSocket(port);

		(listenThread = new Thread() {
			public void run() {

				Socket socket;
				try {
					while (keepAlive) {

						try {

							socket = serverSocket.accept();

							// Create Client object.
							Client client = new Client(Server.this, socket);
							clients.add(client);

							// Notify listeners - client connected
							clientHandler.obtainMessage(CLIENT_CONNECTED)
									.sendToTarget();

						} catch (SocketException ex) {
							// A SocketException is thrown when the stop method
							// calls 'close' on the serverSocket object. This
							// means we should break out of the connection
							// accept loop.
							keepAlive = false;
						}

					}

				} catch (IOException e) {
					// println(e.toString());
				}
			}
		}).start();

		// Notify listeners - server started
		clientHandler.obtainMessage(SERVER_STARTED).sendToTarget();
	}

	/**
	 * Stops the server
	 */
	public void stop() {
		// Stop listening in the TCP port.
		if (serverSocket != null)
			try {
				serverSocket.close();
			} catch (IOException e) {
				// println(e.toString());
			}

		// Close all clients.
		for (Client client : clients)
			client.close();

		// Notify listeners - server stopped
		clientHandler.obtainMessage(SERVER_STOPPED).sendToTarget();
	}

	/**
	 * Called by the Client class to remove itself from the server.
	 * 
	 * @param client
	 *            Client to disconnect
	 */
	protected void disconnectClient(Client client) {
		this.clients.remove(client);

		// Notify listeners - client disconnected
		clientHandler.obtainMessage(CLIENT_DISCONNECTED).sendToTarget();
	}

	/**
	 * Fires the receive event. Called by the client when it has new data to
	 * offer.
	 * 
	 * @param client
	 *            source client
	 * @param data
	 *            data
	 */
	protected void receive(Client client, byte[] data) {
		// Notify listeners - receive
		clientHandler.obtainMessage(CLIENT_RECEIVE, data).sendToTarget();
	}

	/**
	 * Send bytes to all connected clients.
	 * 
	 * @param data
	 *            data to send
	 * @throws IOException
	 */
	public void send(byte[] data) throws IOException {
		for (Client client : clients)
			client.send(data);
	}

}
