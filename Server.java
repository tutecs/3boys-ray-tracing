// Server file for the 3boys ray tracing program
// Ethan Duryea, Joshua Weller, John Zamites
// Version 1.0
import java.net.*;
import java.io.*;
import java.util.*;


public class Server
{
	InetAddress address = null;
	int outPort = null;
	public static void getSceneAndRender(int listenPort)
	{
		DatagramSocket socket = new DatagramSocket(listenPort);
		boolean completed = false;
		List<Sphere> spheres = getSpheres(listenPort);

		String ready = "We really out here.";
		sendString(ready, outPort, socket);
		boolean running = true;
		while(running)
		{
			int xStart = null;
			int xStop = null;
			String message = receiveString(socket);
			String[] xData = message.split(":");
			if(xData[0].equals("End"))
			{
				running = false;
				break;
			}
			if(xData[0].equals("xrange"))
			{
				int[] xs = new int[xData.length-1];
				for(int i = 1; i < xData.length; ++i)
				{
					xs[i-1] = xData[i];
				}
				render(spheres, xs, socket, address, outPort);
			}
			sendString("Done", outport, socket);
		}
		socket.close();
	}
	public static String receiveString(DatagramSocket socket)
	{
		byte[] buf = new byte[256];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);
		String message = new String(packet.getData(), 0, packet.getLength());
		return message;
	}
	public static void sendString(String send, int port, DatagramSocket socket)
	{
		try
		{
			byte[] data = send.getBytes();
			DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
			socket.send(packet);
		} catch (UnknownHostException e) {
        e.printStackTrace();
        } catch (SocketException e) {
        e.printStackTrace();
        } catch (IOException e) {
        e.printStackTrace();
        }
	}
	public static List<Sphere> getSpheres(DatagramSocket socket)
	{
		List<Sphere> spheres = new ArrayList<Sphere>();
		byte[] buf = new byte[256];
		try
		{
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			address = socket.getAddress();
			String data = new String(packet.getData(), 0, packet.getLength());
			String[] dataArray = data.split(":");
			int l = Integer.parseInt(dataArray[0]);
			int d = Integer.parseInt(dataArray[1]);
			String sphereData = dataArray[2];
			spheres.add(makeSphere(sphereData));
			count = 1;
			while(count < l)
			{
				byte[] buf = new byte[256];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				String data = new String(packet.getData(), 0, packet.getLength());
				String[] dataArray = data.split(":");
				int currentL = Integer.parseInt(dataArray[0]);
				int currentD = Integer.parseInt(dataArray[1]);
				String sphereData = dataArray[2];
				if(currentD == d)
				{
					spheres.add(makeSphere(sphereData));
					++count;
				}
				else
				{
					d = currentD;
					l = currentL;
					spheres = new ArrayList<Sphere>();
					spheres.add(makeSphere(sphereData));
				}
				address = socket.getAddress();
				outPort = socket.getPort();
			}
			return spheres;
		} catch (UnknownHostException e) {
        e.printStackTrace();
        } catch (SocketException e) {
        e.printStackTrace();
        } catch (IOException e) {
        e.printStackTrace();
        }
		return null;
	}
	public static Sphere makeSphere(String dataStr)
	{
		String[] data 	= dataStr.split("\\s+");
		String type 	= data[0];
		Sphere sphere 	= null;
		if(type.equals("light"))
		{
			float center.x 	= Float.parseFloat(data[1]);
			float center.y 	= Float.parseFloat(data[2]);
			float center.z 	= Float.parseFloat(data[3]);
			float color.x	= Float.parseFloat(data[4]);
			float color.y	= Float.parseFloat(data[5]);
			float color.z	= Float.parseFloat(data[6]);
			Vec3f center 	= new Vec3f(center.x,center.y,center.z);
			Vec3f color 	= new Vec3f(color.x,color.y,color.z);
			Sphere sphere = Sphere.Light(center, color);
		}
		else
		{
			float center.x 	= Float.parseFloat(data[1]);
			float center.y 	= Float.parseFloat(data[2]);
			float center.z 	= Float.parseFloat(data[3]);
			float radius 	= Float.parseFloat(data[4]);
			float color.x	= Float.parseFloat(data[5]);
			float color.y	= Float.parseFloat(data[6]);
			float color.z	= Float.parseFloat(data[7]);
			float transparency	= Float.parseFloat(data[8]);
			float reflection	= Float.parseFloat(data[9]);
			Vec3f center 	= new Vec3f(center.x,center.y,center.z);
			Vec3f color 	= new Vec3f(color.x,color.y,color.z);
			Sphere sphere 	= new Sphere(center, radius, color, reflection, transparency);
		}
		return sphere;
	}
	public static void main(String args[]) throws IOException
	{
		if(args.length != 1)
		{
			System.out.println("Usage: java Server [port#]");
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
