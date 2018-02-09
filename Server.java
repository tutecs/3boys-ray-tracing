// Server file for the 3boys ray tracing program
// Ethan Duryea, Joshua Weller, John Zamites
// Version 1.0
import java.net.*;
import java.io.*;
import java.util.*;


public class Server
{
	public static void getSceneAndRender(int listenPort)
	{
		boolean completed = false;
		try
		{
			while(!completed)
			{
				DatagramSocket socket = new DatagramSocket(listenPort);
				byte[] sphereData = new byte[1024];
				byte[] xData = new byte[1024];

				DatagramPacket packet = new DatagramPacket(sphereData, sphereData.length);
				socket.receive(packet);
				byte[] data = packet.getData();
				ByteArrayInputStream in = new ByteArrayInputStream(data);
				ObjectInputStream is = new ObjectInputStream(in);
				List<Sphere> spheres = new ArrayList<Sphere>();
				int[] xs = new int[2];
				try
				{
					spheres = (List<Sphere>) is.readObject();
				}
				catch (ClassNotFoundException cne) {
					cne.printStackTrace();
					System.exit(1);
				}
				DatagramPacket workPacket = new DatagramPacket(xData, xData.length);
				String renderRequest = new String(workPacket.getData(), 0, workPacket.getLength());
				String[] splits = renderRequest.split(" ");
				if(splits[0].equals("xrange") && splits.length == 3) {
					int xStart = Integer.parseInt(splits[1]);
					int xStop = Integer.parseInt(splits[2]);

					Vec3f[][] screen = RayTracer.render(spheres, xStart, xStop);

					InetAddress address = packet.getAddress();
					int port = packet.getPort();
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					ObjectOutputStream os = new ObjectOutputStream(outputStream);
					os.writeObject(screen);
					byte[] screenData = outputStream.toByteArray();
					DatagramPacket sendScreen = new DatagramPacket(screenData, screenData.length, address, port);
					socket.send(sendScreen);
					completed = true;
					socket.close();
				}
			}
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}
		catch (IOException i)
		{
			i.printStackTrace();
		}
	}
	public static void main(String args[]) throws IOException
	{
		if(args.length != 1)
		{
			System.out.println("Usage: java RayTracerServer [port#]");
			System.exit(1);
			return;
		}
		try
		{
			int port = Integer.parseInt(args[0]);
			getSceneAndRender(port);
		}
		catch (NumberFormatException nfe)
		{
			System.out.println("The port number must be an integer.");
            System.exit(1);
		}
	}
}
