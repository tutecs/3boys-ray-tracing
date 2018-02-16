// 3boys client for raytracer
// Ethan Duryea, Joshua Weller, John Zamites, Elliot Spicer
// Version 1.0

import java.net.*;
import java.io.*;
import java.util.*;

public class RayTracerClient
{
    private static int outPort = 3333;
    private static int inPort = outPort;
	private statci int pxPort = 3334;
    private static int width = 640*2;
    private static int height = 480*2;

    public static void main(String[] args) throws IOException
    {
        if(args.length != 2)
        {
            System.out.println("Usage: java RayTracerClient [input.scene] [output.ppm]");
            System.exit(1);
            return;
        }

        String sceneFile = args[0];
        String outputFile = args[1];
        InetAddress[] addresses = getAddresses();

        // Get sphere data from scene file
        List<Sphere> spheres = RayTracer.readScene(sceneFile);
        // Send sphere data to each node


        int[][] xs = divideWork(addresses.length);
		ReceiveMessages messageGetter = new ReceiveMessages(inPort);
		ReceiveData getter = new ReceiveData(pxPort, "PixelGetter", height, width);
		getter.start();
		isReady(xs, addresses, spheres, messageGetter);
		Vec3f[][] screen = new Vec3f[width][height];
		int nCols = 0;
		while(nCols < screen.length)
		{
			Vec3f[][] receivedScreen = packetGetter.receivedCol();
			synchronized(receivedScreen)
			{
				for(int i = 0; i < receivedScreen.legnth; ++i)
				{
					if(receivedScreen[i][0] != null && screen[i][0] != null)
					{
						screen[i] = receivedScreen[i];
						++nCols;
					}
				}
			}
		}


		RayTracer.writePPM(outputFile, screen, width, height);

    }
    public static InetAddress[] getAddresses()
    {
        List<InetAddress> addresses = new ArrayList<InetAddress>();
        try
        {
            File addressFile = new File("SERVER_LIST");
            Scanner sc = new Scanner(addressFile);
            while(sc.hasNextLine())
            {
                addresses.add(InetAddress.getByName(sc.nextLine()));
            }
        } catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        return addresses.toArray(new InetAddress[0]);
    }
	public static void sendSpheres(InetAddress address, Sphere[] spheres) {
		  Random rand = new Random();
		  int d = rand.nextInt(100);
		  int l = spheres.length;
		  for(int i = 0; i < spheres.length; i++;) {
			  String sphereStr;
			  DatagramSocket socket = new DatagramSocket();
			  sphereStr = String.format("%d:%d:%s" l, d, spheres[i].toString);
			  buf = sphereStr.getBytes();
			  DatagramPacket packet = new DatagramPacket(buf, buf.length, address, outport);
			  socket.send(packet);
		  }
	}
    public static void sendRender(InetAddress address, int[] xs)
    {
		try
		{
			DatagramSocket socket = new DatagramSocket();
			String xString = "xrange";
			for(int i = 0; i < xs.length; ++i;)
			{
				xString = String.format("%s:%d", xString, xs[i]);
			}
            byte[] data = xString.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, outPort);
            socket.send(sendPacket);
            socket.close();
        } catch (UnknownHostException e) {
        e.printStackTrace();
        } catch (SocketException e) {
        e.printStackTrace();
        } catch (IOException e) {
        e.printStackTrace();
        }
    }
    public static int[][] divideWork(int nNodes)
    {
        int[][] xs = new int[nNodes][stepSize];
        double dNodes = (double) nNodes;
        double dstepSize = width/nNodes;
        boolean even = true;
        int stepSize = (int) dstepSize;
        if(stepSize != dstepSize)
        {
            even = false;
        }
        int current = 0;
        for(int i = 0; i < nNodes-1; ++i)
        {
			int[] nodeXs = new int[stepSize];
			for(int j = 0; j < stepSize; ++j)
			{
				nodeXs[j] = current
				++current;
			}
			xs[i] = nodeXs
        }
        if(even)
        {
			int[] nodeXs = new int[stepSize];
			for(int j = 0; j < stepSize; ++j)
			{
				nodeXs[j] = current
				++current;
			}
            xs[nNodes-1] = nodeXs;
        }
        else
        {
			int[] nodeXs = new int[stepSize+1];
			for(int j = 0; j < stepSize+1; ++j)
			{
				nodeXs[j] = current
				++current;
			}
            xs[nNodes-1] = nodeXs;
        }
        return xs;
	}
}

class ReceiveData implements Runnable
{
	private Thread t;
	private String threadName;
	private int listeningPort;
	private int height;
	private int width;
	private List<Vec3f[]> color = Collections.synchronizedList(new ArrayList<Vec3f[]>());
	private Vec3f[][] color;

	public ReceiveData(int port, String name, int height, int width)
	{
		listeningPort = port;
		threadName = name;
		this.height = height;
		this.width = width;
		color = new Vec3f[width][height];
	}
	public Vec3f receivedCols()
	{
		return color;
	}
	public void run()
	{
		int nCols = 0;
		try
		{
			DatagramSocket socket = new DatagramSocket(listeningPort);
			while(nCols < width)
			{
				byte[] receiveData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
				socket.receive(receivePacket);
				String data = new String(receivePacket.getData(), 0, receivePacket.length);
				String[] edata = data.spilt(":");
				if(edata.length == (1 + (height)))
				{
					int posx = Integer.parseInt(edata[0]);
					for(int i = 1; i < edata.length; ++i)
					{
						String[] vecData = edata[i].split(" ");
					    float x = Float.parseFloat(vecData[0]);
					    float y = Float.parseFloat(vecData[1]);
					    float z = Float.parseFloat(vecData[2]);
						color[posx][i] = new Vec3f(x, y, z);
					}
					++nCols;
				}
			}
			socket.close();
		} catch (InterruptedException e) {
     		System.out.println("Thread " +  threadName + " interrupted.");
  		} catch (SocketException e) {
        	e.printStackTrace();
        }
	}
	public void start()
	{
		if(t == null)
		{
			t = new Thread(this, threadName);
			t.start();
		}
	}
}
class ReceiveMessages implements Runnable
{
	private Thread t;
	private String threadName;
	private List<String> messages;
	private int port;
	public ReceiveMessages(int listenPort)
	{
		port = listenPort;
		messages = Collections.synchronizedList(new ArrayList<String>());
	}
	public void run()
	{
		try
		{
			DatagramSocket socket = new DatagramSocket(port);
			while(true)
			{
				synchronized (messages)
				{
					byte[] receiveData = new byte[1024];
					DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
					socket.receive(receivePacket);
					String data = new String(receivePacket.getData(), 0, receivePacket.length);
					String address = receivePacket.getAddress().getHostAddress();
					int fromPort = receivePacket.getPort();
					String message = String.format("%s:%s:%d", data, address, fromPort);
					messages.add(message);
				}
			}
			socket.close();
		}catch (InterruptedException e) {
     		System.out.println("Thread " +  threadName + " interrupted.");
  		} catch (SocketException e) {
        	e.printStackTrace();
        }
	}
	public ArrayList<String[]> getReady()
	{
		ArrayList<String[]> addresses = new ArrayList<String[]>();
		synchronized(messages)
		{
			for(String message : messages)
			{
				String[] messageData = messages.split(":");
				if(messageData[0] == "We really out here.")
				{
					String[] addrPort = {messageData[1], messageData[2]};
					addresses.add(addrPort);
					messages.remove(message);
				}
			}
		}
		return addresses;
	}
	public void start()
	{
		if(t == null)
		{
			t = new Thread(this, threadName);
			t.start();
		}
	}
}
