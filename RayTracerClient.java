// 3boys client for raytracer
// Ethan Duryea, Joshua Weller, John Zamites, Elliot Spicer
// Version 1.0

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;


public class RayTracerClient
{
    private static int outPort = 3333;
    private static int inPort = 3334;
	// private static int pxPort = 3334;
    private static int width = 640*2;
    private static int height = 480*2;
	private static ReceiveMessages messageGetter = new ReceiveMessages(inPort, height, width, "Message Receiver");

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
		try
		{
			DatagramSocket socket = new DatagramSocket();

	        // Get sphere data from scene file
	        List<Sphere> spheres = RayTracer.readScene(sceneFile);
	        // Send sphere data to each node
			System.out.println(addresses.length);
			ArrayList<Integer> needXs = new ArrayList<Integer>();
			for(int i = 0; i < width; i++)
			{
				needXs.add(i);
			}
	        int[][] xs = divideWork(addresses.length, needXs);

			messageGetter.start();
			isReady(xs, addresses, spheres, socket);
			Vec3f[][] screen = new Vec3f[width][height];
			int[] pxInCol = new int[width];
			int nPx = 0;
			boolean receivedPx = false;
			while(nPx < (width*height))
			{
				ArrayList<String> pixelStrings = messageGetter.getPixelStrings();
				for(String pixelString : pixelStrings)
				{
					String[] messageData = pixelString.split(":");
					int posx = Integer.parseInt(messageData[2]);
					int posy = Integer.parseInt(messageData[3]);

					String[] vecData = messageData[4].split(" ");
					float x = Float.parseFloat(vecData[0]);
					float y = Float.parseFloat(vecData[1]);
					float z = Float.parseFloat(vecData[2]);
					if(screen[posx][posy] == null)
					{
						screen[posx][posy] = new Vec3f(x, y, z);
						pxInCol[posx] = pxInCol[posx] + 1;
						++nPx;
						receivedPx = true;
					}
				}
				boolean hasIPs = messageGetter.hasIPs();
				if(receivedPx && hasIPs && nPx < (width*height))
				{
					InetAddress[] doneIPs = messageGetter.getIPs();
					needXs = new ArrayList<Integer>();
					for(int i = 0; i < pxInCol.length; i++)
					{
						if(pxInCol[i] < height)
						{
							needXs.add(i);
						}
					}
					System.out.printf("We still need %d columns \n", needXs.size());
					xs = divideWork(doneIPs.length, needXs);
					int i = 0;
					for(InetAddress ip : doneIPs)
					{
						sendRender(ip, xs[i], socket);
						i++;
					}
				}
				System.out.printf("We've received %d of %d pixels \n", nPx, width*height);
			}
			System.out.println("Done.");
			RayTracer.writePPM(outputFile, screen, width, height);
			socket.close();
		} catch (UnknownHostException e) {
        	e.printStackTrace();
        } catch (SocketException e) {
        	e.printStackTrace();
        } catch (IOException e) {
			e.printStackTrace();
		}

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
        return addresses.toArray(new InetAddress[addresses.size()]);
    }


	public static void sendSpheres(InetAddress address, List<Sphere> spheres, DatagramSocket socket) {
		Random rand = new Random();
		int d = rand.nextInt(1000);
		int l = spheres.size();
		try {
			for(Sphere sphere : spheres) {
				String sphereStr;
				sphereStr = String.format("%d:%d:%s", l, d, sphere.toString());
				byte[] buf = sphereStr.getBytes();
				DatagramPacket packet = new DatagramPacket(buf, buf.length, address, outPort);
				socket.send(packet);
			}
		} catch(IOException io) {
			io.printStackTrace();
		}

	}
    public static void sendRender(InetAddress address, int[] xs, DatagramSocket socket)
    {
		Random rand = new Random();
		int d = rand.nextInt(1000);
		int l = xs.length;
		System.out.printf("Sending %d xs to render: %d \n", xs.length, d);
		boolean keepSending = true;
		while(keepSending)
		{
			if(messageGetter.hasPixels(address.getHostAddress(), d))
			{
				keepSending = false;
			}
			for(int i = 0; i < xs.length; ++i)
			{
				String xString = String.format("%s:%d:%d:%d", "xData", l, d, xs[i]);
				byte[] data = xString.getBytes();
				try {
					DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, outPort);
					socket.send(sendPacket);
				} catch(IOException io)
				{
					io.printStackTrace();
				}
			}
		}
    }

    public static void isReady(int[][] xs, InetAddress[] addresses, List<Sphere> spheres, DatagramSocket socket){
		for(InetAddress address : addresses){
            sendSpheres(address, spheres, socket);
        }
        int i = 0;

        while(i < addresses.length) {
            List<String[]> AddressPort = messageGetter.getReady();
            //brings in the new addresses.

            for(String[] address : AddressPort) {
				System.out.printf("Address %s is ready. Sending render info \n", address[0].toString());
				try {
                	InetAddress addr2 = InetAddress.getByName(address[0]);
                	sendRender(addr2, xs[i], socket);
                	++i;
				} catch(UnknownHostException e)
				{
					e.printStackTrace();
				}
            }
        }
    }

    public static int[][] divideWork(int nNodes, ArrayList<Integer> needXs)
    {
        double dNodes = (double) nNodes;
        double dstepSize = needXs.size()/nNodes;
        boolean even = true;
        int stepSize = (int) dstepSize;
		int[][] xs = new int[nNodes][stepSize];
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
				nodeXs[j] = needXs.get(current);
				++current;
			}
			xs[i] = nodeXs;
        }
        if(even)
        {
			int[] nodeXs = new int[stepSize];
			for(int j = 0; j < stepSize; ++j)
			{
				nodeXs[j] = needXs.get(current);
				++current;
			}
            xs[nNodes-1] = nodeXs;
        }
        else
        {
			int[] nodeXs = new int[stepSize+1];
			for(int j = 0; j < stepSize+1; ++j)
			{
				nodeXs[j] = needXs.get(current);
				++current;
			}
            xs[nNodes-1] = nodeXs;
        }
        return xs;
    }
}

class ReceiveMessages implements Runnable
{
	private Thread t;
	private String threadName;
	private CopyOnWriteArrayList<String> messages;
	private CopyOnWriteArrayList<InetAddress> doneIPs;
	private int port;
	private int height;
	private int width;
	public ReceiveMessages(int listenPort, int height, int width, String name)
	{
		port = listenPort;
		this.height = height;
		this.width = width;
		threadName = name;
		messages = new CopyOnWriteArrayList<String>();
		doneIPs = new CopyOnWriteArrayList<InetAddress>();
	}
	public void run()
	{
		try
		{
			DatagramSocket socket = new DatagramSocket(port);
			while(true)
			{
				byte[] receiveData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
				socket.receive(receivePacket);
				String data = new String(receivePacket.getData(), 0, receivePacket.getLength());
				InetAddress ipAddress = receivePacket.getAddress();
				String address = ipAddress.getHostAddress();
				int fromPort = receivePacket.getPort();
				String message = String.format("%s:%s:%d", data, address, fromPort);
				if(data.contains("Done"))
				{
					doneIPs.add(ipAddress);
				}
				else
				{
					messages.add(message);
				}
			}
			// socket.close();
		}
		// catch (InterruptedException e) {
     	// System.out.println("Thread " +  threadName + " interrupted.");
  		// }
		catch (SocketException e) {
        	e.printStackTrace();
        }
		catch (IOException io) {
			io.printStackTrace();
		}
	}
	public boolean hasIPs()
	{
		if(!doneIPs.isEmpty())
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	public InetAddress[] getIPs()
	{
		InetAddress[] addresses = doneIPs.toArray(new InetAddress[doneIPs.size()]);
		doneIPs.removeAll(doneIPs);
		return addresses;
	}
	public ArrayList<String[]> getReady()
	{
		ArrayList<String[]> addresses = new ArrayList<String[]>();
		// synchronized(messages)
		// {
			for(String message : messages)
			{
				System.out.println(message);
				String[] messageData = message.split(":");
				if(messageData[0].equals("We really out here."))
				{
					String[] addrPort = {messageData[1], messageData[2]};
					addresses.add(addrPort);
					messages.remove(message);
				}
			}
		// }
		return addresses;
	}
	public boolean hasPixels(String address, int d)
	{
		for(String message : messages)
		{
			String[] messageData = message.split(":");
			if(messageData[0].equals("rowcolor"))
			{
				if(messageData[5].equals(address) && messageData[1].equals(String.valueOf(d)))
				return true;
			}
		}
		return false;
	}
	public ArrayList<String> getPixelStrings()
	{
		ArrayList<String> pixels = new ArrayList<String>();
		for(String message : messages)
		{
			String[] messageData = message.split(":");
			if(messageData[0].equals("rowcolor"))
			{
				pixels.add(message);
				messages.remove(message);
			}
		}
		return pixels;
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
