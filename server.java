// Server file for the 3boys ray tracing program
// Ethan Duryea, Joshua Weller, John Zamites
// Version 1.0
import java.net.*;
import java.io.*;


public class RayTracerServer
{
	public static void getSceneAndRender(int listenPort)
	{
		try
		{
			DatagramSocket socket = new DatagramSocket(listenPort);
			byte[] sphereData = new byte[1024];
			byte[] xData = new byte[40];

			DatagramPacket packet = new DatagramPacket(sphereData, sphereData.length);
			socket.receive(packet);
			byte[] data = packet.getData()
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			ObjectInputStream is = new ObjectInputStream(in);
			try
			{
				List<Sphere> spheres = (List<Sphere>) is.readObject();
			}
			catch (ClassNotFoundException cn)
			{
				cn.printStackTrace();
			}

			DatagramPacket workPacket = new DatagramPacket(xData, xData.length);
			byte[] xs = packet.getData();
			try
			{
				int[] xs = (int[]) is.readObject();
			}
			catch (ClassNotFoundException cn)
			{
				cn.printStackTrace();
			}

			int xStart = xs[0], xStop = xs[1];


			Vec3f[][] screen = render(spheres, xStart, xStop);


			InetAddress address = packet.getAddress();
			int port = packet.getPort();
			byte[] screenReply = screen.getBytes();
			DatagramPacket sendScreen = new DatagramPacket(screenReply, screenReply.length, address, port);

			socket.send(sendScreen);

			socket.close();
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}
		catch (IOException i)
		{
			i.printStackTrace();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
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

		getSceneAndRender(port);
	}
}
