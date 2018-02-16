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
        sendSpheres(addresses.toArray(new Sphere[addresses.size()]), spheres);

        int[][] xs = divideWork(addresses.length);

		Server.receiveString();

		for(int i = 0; i < addresses.length; ++i)
		{
            // Send render request to each node.
            // xs[i] contains the starting width and the ending width for the render request
			sendRender(addresses[i], xs[i]);
		}
        Vec3f[][] screen = receive(addresses, xs);
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
	public static void sendSpheres(InetAddress addresses[], Sphere[] spheres) {
		  Random rand = new Random();
		  int d = rand.nextInt(100);
		  for(InetAddress address : addresses){
			  for(int i = 0; i < spheres.length; i++;){
				  String sphereStr;
				  DatagramSocket socket = new DatagramSocket();
				  sphereStr = String.format("%d:%d:%s" l, d, spheres[i].toString);
				  buf = sphereStr.getBytes();
				  DatagramPacket packet = new DatagramPacket(buf, buf.length, address, outport);
				  socket.send(packet);
			  }
		  }
	}
    public static void sendRender(InetAddress address, int[] xs)
    {
		int xStart = xs[0];
		int xStop = xs[1];
		try
		{
			DatagramSocket socket = new DatagramSocket();
            byte[] data = String.format("xrange:%d:%d", xStart, xStop).getBytes();
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
    public static Vec3f[][] receive(InetAddress[] addresses, int[][] xs, int port)
    {
		Vec3f[][] screen = color = new Vec3f[width][height];
		int nCols = 0;
		InetAddress address = addresses[i];
		ReceiveData packetGetter = new ReceiveData(port, String.format("Getter: %d", i), height, width);
		packetGetter.start();

		while(nCols < screen.length)
		{
			Vec3f[][] receivedScreen = packetGetter.receivedCol();
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
    public static int[][] divideWork(int nNodes)
    {
        int[][] xs = new int[nNodes][2];
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
            xs[i][0] = current;
            xs[i][1] = current + stepSize;
        }
        xs[nNodes-1][0] = current;
        if(even)
        {
            xs[nNodes-1][1] = current + stepSize;
        }
        else
        {
            xs[nNodes-1][1] = current + stepSize + 1;
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
		} catch (InterruptedException e) {
     		System.out.println("Thread " +  threadName + " interrupted.");
  		} catch (SocketException e) {
        	e.printStackTrace();
        }
		socket.close();
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
