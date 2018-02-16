// Server file for the 3boys ray tracing program
// Ethan Duryea, Joshua Weller, John Zamites
// Version 1.0
import java.net.*;
import java.io.*;
import java.util.*;


public class Server
{
	static InetAddress address;
	static int listenPort;
	static int outPort;
	public static void getSceneAndRender()
	{
		try
		{
			DatagramSocket socket = new DatagramSocket(listenPort);
			boolean completed = false;
			List<Sphere> spheres = getSpheres(socket);

			String ready = "We really out here.";
			sendString(ready, outPort, socket);
			boolean running = true;
			while(running)
			{
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
						xs[i-1] = Integer.parseInt(xData[i]);
					}
					RayTracer	.render(spheres, xs, socket, address, outPort);
				}
				sendString("Done", outPort, socket);
			}
			socket.close();
		} catch (SocketException e) {
        	e.printStackTrace();
        }
	}
	public static String receiveString(DatagramSocket socket)
	{
		String message = null;
		byte[] buf = new byte[256];
		try
		{
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			message = new String(packet.getData(), 0, packet.getLength());
		} catch (IOException e) {
        	e.printStackTrace();
        }
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
			InetAddress address = packet.getAddress();
			String data = new String(packet.getData(), 0, packet.getLength());
			String[] dataArray = data.split(":");
			int l = Integer.parseInt(dataArray[0]);
			int d = Integer.parseInt(dataArray[1]);
			String sphereData = dataArray[2];
			spheres.add(makeSphere(sphereData));
			int count = 1;
			while(count < l)
			{
				buf = new byte[256];
				packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				data = new String(packet.getData(), 0, packet.getLength());
				dataArray = data.split(":");
				int currentL = Integer.parseInt(dataArray[0]);
				int currentD = Integer.parseInt(dataArray[1]);
				sphereData = dataArray[2];
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
				address = packet.getAddress();
				outPort = packet.getPort();
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
			float centerx 	= Float.parseFloat(data[1]);
			float centery 	= Float.parseFloat(data[2]);
			float centerz 	= Float.parseFloat(data[3]);
			float colorx	= Float.parseFloat(data[4]);
			float colory	= Float.parseFloat(data[5]);
			float colorz	= Float.parseFloat(data[6]);
			Vec3f center 	= new Vec3f(centerx,centery,centerz);
			Vec3f color 	= new Vec3f(colorx,colory,colorz);
			sphere = Sphere.Light(center, color);
		}
		else
		{
			float centerx 	= Float.parseFloat(data[1]);
			float centery 	= Float.parseFloat(data[2]);
			float centerz 	= Float.parseFloat(data[3]);
			float radius 	= Float.parseFloat(data[4]);
			float colorx	= Float.parseFloat(data[5]);
			float colory	= Float.parseFloat(data[6]);
			float colorz	= Float.parseFloat(data[7]);
			float transparency	= Float.parseFloat(data[8]);
			float reflection	= Float.parseFloat(data[9]);
			Vec3f center 	= new Vec3f(centerx,centery,centerz);
			Vec3f color 	= new Vec3f(colorx,colory,colorz);
			sphere 	= new Sphere(center, radius, color, reflection, transparency);
		}
		return sphere;
	}
	public static void main(String args[]) throws IOException
	{
		if(args.length != 1)
		{
			System.out.println("Usage: java Server [inPort#]");
			System.exit(1);
			return;
		}
		try
		{
			listenPort = Integer.parseInt(args[0]);
			getSceneAndRender();
		}
		catch (NumberFormatException nfe)
		{
			System.out.println("The port number must be an integer.");
            System.exit(1);
		}
	}
}
