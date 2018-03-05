// Server file for the 3boys ray tracing program
// Ethan Duryea, Joshua Weller, John Zamites
// Version 1.0
import java.net.*;
import java.io.*;
import java.util.*;


public class Server
{

	static InetAddress address = null;
	static int listenPort;
	static int outPort = 3334;
	public static void getSceneAndRender()
	{
		try
		{
			DatagramSocket socket = new DatagramSocket(listenPort);
			// address = InetAddress.getLocalHost();
			boolean completed = false;
			List<Sphere> spheres = getSpheres(socket);
			System.out.printf("we have received %d spheres from address: %s \n", spheres.size(), address.toString());
			String ready = "Done.";
			sendString(ready, outPort, socket);
			boolean running = true;
			int d = -1;
			while(running)
			{
				int[] xs = getRenderRequest(socket, d);
				// System.out.printf("Received %d xs to render \n", xs.length - 1);
				// String message = receiveString(socket);
				// String[] xData = message.split(":");
				if(xs != null && xs[0] == -1)
				{
					running = false;
					break;
				}
				if(xs != null)
				{
					d = xs[0];
					RayTracer.render(spheres, xs, socket, address, outPort);
				}
				else
				{
					socket = new DatagramSocket(listenPort);
				}
				System.out.println("Done.");
				sendString("Done", outPort, socket);
			}
			socket.close();
		} catch (SocketException e) {
        	e.printStackTrace();
        } // catch (UnknownHostException e) {
		// 	e.printStackTrace();
		// }
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
		// System.out.printf("We received message: %s", message);
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
			// InetAddress address = packet.getAddress();
			String data = new String(packet.getData(), 0, packet.getLength());
			String[] dataArray = data.split(":");
			int l = Integer.parseInt(dataArray[0]);
			int d = Integer.parseInt(dataArray[1]);
			String sphereData = dataArray[2];
			spheres.add(makeSphere(sphereData));
			int count = 1;
			address = packet.getAddress();
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
					count = 1;
				}
				address = packet.getAddress();
				// System.out.println(address.toString());
				// outPort = packet.getPort();
			}
			// address = InetAddress.getLocalHost();
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
	public static int[] getRenderRequest(DatagramSocket socket, int prevD)
	{
		ArrayList<Integer> receivedXs = new ArrayList<Integer>();
		byte[] buf = new byte[256];
		try
		{
			boolean firstX = false;
			DatagramPacket packet;
			String data;
			String[] dataArray;
			int l = 0;
			int d = 0;
			int count = 0;
			while(!firstX)
			{
				socket.setSoTimeout(1*1000);
				packet = new DatagramPacket(buf, buf.length);
				try
				{
					socket.receive(packet);
				}
				catch (SocketTimeoutException e)
				{
					socket.close();
					return null;
				}

				data = new String(packet.getData(), 0, packet.getLength());
				dataArray = data.split(":");
				if(dataArray[0].equals("End"))
				{
					return new int[] {-1};
				}
				if(dataArray[0].equals("xData"))
				{
					l = Integer.parseInt(dataArray[1]);
					d = Integer.parseInt(dataArray[2]);
					if(prevD != d)
					{
						int currentX = Integer.parseInt(dataArray[3]);
						receivedXs.add(currentX);
						firstX = true;
						count++;
					}
				}
			}
			while(count < l)
			{
				
				System.out.printf("%d of %d : %d \n", count, l, d);
				buf = new byte[256];
				packet = new DatagramPacket(buf, buf.length);
		
				
				socket.setSoTimeout(1*1000);
				try
				{
					socket.receive(packet);
				}
				catch (SocketTimeoutException e)
				{
					socket.close();
					return null;
				}
				data = new String(packet.getData(), 0, packet.getLength());
				dataArray = data.split(":");
				if(dataArray[0].equals("End"))
				{
					return null;
				}
				if(dataArray[0].equals("xData"))
				{
					int currentL = Integer.parseInt(dataArray[1]);
					int currentD = Integer.parseInt(dataArray[2]);
					int currentX = Integer.parseInt(dataArray[3]);
					if(currentD == d)
					{
						if(!receivedXs.contains(currentX))
						{
							receivedXs.add(currentX);
							++count;
						}
					}
					else
					{
						System.out.println("Different d, clearing xs.");
						d = currentD;
						l = currentL;
						receivedXs = new ArrayList<Integer>();
						receivedXs.add(currentX);
						count = 1;
					}
				}
			}
			int[] xs = new int[receivedXs.size() + 1];
			xs[0] = d;
			for(int i = 0; i < receivedXs.size(); i++)
			{
				xs[i+1] = receivedXs.get(i);
			}
			return xs;
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
