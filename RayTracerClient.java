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
	private static int pxPort = 3334;
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
		try
		{
			DatagramSocket socket = new DatagramSocket();

	        // Get sphere data from scene file
	        List<Sphere> spheres = RayTracer.readScene(sceneFile);
	        // Send sphere data to each node
	        int[][] xs = divideWork(addresses.length);
			ReceiveMessages messageGetter = new ReceiveMessages(inPort, height, width, "Message Receiver");
			// ReceiveData getter = new ReceiveData(pxPort, "PixelGetter", height, width);
			messageGetter.start();
			isReady(xs, addresses, spheres, messageGetter, socket);
			Vec3f[][] screen = new Vec3f[width][height];
			int nCols = 0;
			while(nCols < screen.length)
			{
				Vec3f[][] receivedScreen = messageGetter.getScreen();
				for(int i = 0; i < receivedScreen.length; ++i)
				{
					if(receivedScreen[i][0] != null && screen[i][0] != null)
					{
						screen[i] = receivedScreen[i];
						++nCols;
					}
				}
			}
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
		int d = rand.nextInt(100);
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
		String xString = "xrange";
		for(int i = 0; i < xs.length; ++i)
		{
			xString = String.format("%s:%d", xString, xs[i]);
		}
        byte[] data = xString.getBytes();
		try {
	        DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, outPort);
	        socket.send(sendPacket);
		} catch(IOException io)
		{
			io.printStackTrace();
		}
    }

    public static void isReady(int[][] xs, InetAddress[] addresses, List<Sphere> spheres, ReceiveMessages messageGetter, DatagramSocket socket){
		for(InetAddress address : addresses){
            sendSpheres(address, spheres, socket);
        }
        int i = 0;

        while(i < addresses.length){
            List<String[]> AddressPort = messageGetter.getReady();
            //brings in the new addresses.

            for(String[] address : AddressPort) {
				try {
                	InetAddress addr2 = InetAddress.getByName(address[0]);
                	sendRender(addr2, xs[i], socket);
                	++i;
				} catch(UnknownHostException uh)
				{
					uh.printStackTrace();
				}
            }
        }
    }

    // public static Vec3f[][] receive(InetAddress[] addresses, int[][] xs)
    // {
    //     ArrayList<InetAddress> addressList = new ArrayList<InetAddress>(Arrays.asList(addresses));
    //     boolean receivedAll = false;;
    //     Vec3f[][] screen = new Vec3f[width][height];
    //     try
    //     {
    //         DatagramSocket socket = new DatagramSocket(inPort);
    //         while(!receivedAll)
    //         {
    //             byte[] screenData = new byte[1024];
    //             DatagramPacket packet = new DatagramPacket(screenData, screenData.length);
    //             socket.receive(packet);
    //             ByteArrayInputStream in = new ByteArrayInputStream(screenData);
    //             ObjectInputStream is = new ObjectInputStream(in);
    //             Vec3f[][] partScreen = null;
    //             try
    //             {
    //                 partScreen = (Vec3f[][]) is.readObject();
    //             }
    //             catch (ClassNotFoundException cn)
    //             {
    //                 cn.printStackTrace();
    //                 System.exit(1);
    //             }
    //             String inAddress = packet.getAddress().getHostAddress();
    //             int addressIDX = addressList.indexOf(inAddress);
    //             if(addressIDX >= 0)
    //             {
    //                 int xStart = xs[addressIDX][0], xStop = xs[addressIDX][1];
    //                 for (int y = 0; y < height; ++y)
    //                 {
    //                     for (int x = xStart; x < xStop; ++x)
    //                     {
    //                         screen[x][y] = partScreen[x][y];
    //                     }
    //                 }
    //                 addressList.remove(addressIDX);
    //             }
    //             if(addressList.isEmpty()) { receivedAll = true; }
    //         }
    //         socket.close();
    //     }
    //     catch (UnknownHostException e) {
    //     e.printStackTrace();
    //     } catch (SocketException e) {
    //     e.printStackTrace();
    //     } catch (IOException e) {
    //     e.printStackTrace();
    //     }
    //     return screen;
    // }
    public static int[][] divideWork(int nNodes)
    {
        double dNodes = (double) nNodes;
        double dstepSize = width/nNodes;
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
				nodeXs[j] = current;
				++current;
			}
			xs[i] = nodeXs;
        }
        if(even)
        {
			int[] nodeXs = new int[stepSize];
			for(int j = 0; j < stepSize; ++j)
			{
				nodeXs[j] = current;
				++current;
			}
            xs[nNodes-1] = nodeXs;
        }
        else
        {
			int[] nodeXs = new int[stepSize+1];
			for(int j = 0; j < stepSize+1; ++j)
			{
				nodeXs[j] = current;
				++current;
			}
            xs[nNodes-1] = nodeXs;
        }
        return xs;
    }
}
// class ReceiveData implements Runnable
// {
// 	private Thread t;
// 	private String threadName;
// 	private int listeningPort;
// 	private int height;
// 	private int width;
// 	private List<String> color = Collections.synchronizedList(new ArrayList<String>());
//
// 	public ReceiveData(int port, String name, int height, int width)
// 	{
// 		listeningPort = port;
// 		threadName = name;
// 		this.height = height;
// 		this.width = width;
// 		// color = new Vec3f[width][height];
// 	}
// 	public Vec3f receivedCols()
// 	{
// 		return color;
// 	}
// 	public void run()
// 	{
// 		int nCols = 0;
// 		try
// 		{
// 			DatagramSocket socket = new DatagramSocket(listeningPort);
// 			while(nCols < width)
// 			{
// 				byte[] receiveData = new byte[1024];
// 				DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
// 				socket.receive(receivePacket);
// 				String data = new String(receivePacket.getData(), 0, receivePacket.length);
// 				String[] edata = data.spilt(":");
// 				if(edata.length == (1 + (height)))
// 				{
// 					int posx = Integer.parseInt(edata[0]);
// 					for(int i = 1; i < edata.length; ++i)
// 					{
// 						String[] vecData = edata[i].split(" ");
// 					    float x = Float.parseFloat(vecData[0]);
// 					    float y = Float.parseFloat(vecData[1]);
// 					    float z = Float.parseFloat(vecData[2]);
// 						color[posx][i] = new Vec3f(x, y, z);
// 					}
// 					++nCols;
// 				}
// 			}
// 			socket.close();
// 		} catch (InterruptedException e) {
//      		System.out.println("Thread " +  threadName + " interrupted.");
//   		} catch (SocketException e) {
//         	e.printStackTrace();
//         }
// 	}
// 	public void start()
// 	{
// 		if(t == null)
// 		{
// 			t = new Thread(this, threadName);
// 			t.start();
// 		}
// 	}
// }
class ReceiveMessages implements Runnable
{
	private Thread t;
	private String threadName;
	private List<String> messages;
	private int port;
	private int height;
	private int width;
	public ReceiveMessages(int listenPort, int height, int width, String name)
	{
		port = listenPort;
		this.height = height;
		this.width = width;
		threadName = name;
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
					String data = new String(receivePacket.getData(), 0, receivePacket.getLength());
					String address = receivePacket.getAddress().getHostAddress();
					int fromPort = receivePacket.getPort();
					String message = String.format("%s:%s:%d", data, address, fromPort);
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
	public ArrayList<String[]> getReady()
	{
		ArrayList<String[]> addresses = new ArrayList<String[]>();
		synchronized(messages)
		{
			for(String message : messages)
			{
				String[] messageData = message.split(":");
				if(messageData[0].equals("We really out here."))
				{
					String[] addrPort = {messageData[1], messageData[2]};
					addresses.add(addrPort);
					messages.remove(message);
				}
			}
		}
		return addresses;
	}
	public Vec3f[][] getScreen()
	{
		Vec3f[][] screen = new Vec3f[width][height];
		synchronized(messages)
		{
			for(String message : messages)
			{
				String[] messageData = message.split(":");
				if(messageData[0].equals("rowcolor"))
				{
					int posx = Integer.parseInt(messageData[0]);
					for(int i = 1; i < messageData.length; ++i)
					{
						String[] vecData = messageData[i].split(" ");
					    float x = Float.parseFloat(vecData[0]);
					    float y = Float.parseFloat(vecData[1]);
					    float z = Float.parseFloat(vecData[2]);
						screen[posx][i] = new Vec3f(x, y, z);
					}
				}
			}
		}
		return screen;
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
