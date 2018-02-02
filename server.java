// Server file for the 3boys ray tracing program
import java.net.*;
import RayTracer
import Sphere


public class RayTraceServer {
	public static void main(String[] args) {
		new RayTraceServerThread().start();
	}
}

public class RayTraceServerThread {
	int listenPort;
	List<Sphere> spheres;
	public RayTraceServerThread(int port) {
		listenPort = port;
	}
	public List<Sphere> getData() {
		try {
			ServerSocket serverSocket = new ServerSocket(listenPort);
			Socket socket = serverSocket.accept()
			ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
			spheres = (List<Sphere) inStream.readObject();
			socket.close()
			InetAddress address = socket.getInetAddess()

