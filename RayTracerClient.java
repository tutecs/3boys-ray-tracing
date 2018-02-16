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
		ReceiveMessages messageGetter = new ReceiveMessages(listenPort);
		ReceiveData getter = new ReceiveData(pxPort, "PixelGetter", height, width);
		getter.start();
		isReady(xs, addresses, spheres, messageGetter);
		Vec3f[][] screen = new Vec3f[width][height];
		int nCols = 0;
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


  

=======
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
<<<<<<< HEAD

    public static void isReady(int[][] xs, InetAddress[] addresses, List<Sphere> spheres, ReceiveMessages messageGetter){
        //receive things send out all the spheres call send shperes in while loop 
        //after amount of time t we will resend the data. port is second 


        for(InetAddress address : addresses){
            sendSpheres(address, spheres);

        }

        String[][] AddressPort = messageGetter.getReady();
        int i = 0;

        for(String[] address : AddressPort){
            InetAddress addr2 = InetAddress.getByName(address[0])

            sendRender(addr2, xs[i])
            ++i;

        }

        for(String stuff: addressPort){
            DatagramSocket socket = new DatagramSocket(stuff[1]);
            byte[] buf = new byte [2048];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            

        }        


        try
        {

        }


    }
    public static Vec3f[][] receive(InetAddress[] addresses, int[][] xs)
    {
        ArrayList<InetAddress> addressList = new ArrayList<InetAddress>(Arrays.asList(addresses));
        boolean receivedAll = false;;

        Vec3f[][] screen = new Vec3f[width][height];

        try
        {
            DatagramSocket socket = new DatagramSocket(inPort);

            while(!receivedAll)
            {
                byte[] screenData = new byte[1024];
                DatagramPacket packet = new DatagramPacket(screenData, screenData.length);
                socket.receive(packet);
                ByteArrayInputStream in = new ByteArrayInputStream(screenData);
                ObjectInputStream is = new ObjectInputStream(in);

                Vec3f[][] partScreen = null;

                try
                {
                    partScreen = (Vec3f[][]) is.readObject();
                }
                catch (ClassNotFoundException cn)
                {
                    cn.printStackTrace();
                    System.exit(1);
                }
                String inAddress = packet.getAddress().getHostAddress();
                int addressIDX = addressList.indexOf(inAddress);
                if(addressIDX >= 0)
                {
                    int xStart = xs[addressIDX][0], xStop = xs[addressIDX][1];

                    for (int y = 0; y < height; ++y)
                    {
                        for (int x = xStart; x < xStop; ++x)
                        {
                            screen[x][y] = partScreen[x][y];
                        }
                    }
                    addressList.remove(addressIDX);
                }

                if(addressList.isEmpty()) { receivedAll = true; }
            }
            socket.close();
        }
        catch (UnknownHostException e) {
        e.printStackTrace();
        } catch (SocketException e) {
        e.printStackTrace();
        } catch (IOException e) {
        e.printStackTrace();
        }
        return screen;
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
	private ArrayList<String> messages;
	private int port;
	public ReceiveMessages(int listenPort)
	{
		port = listenPort;
		messages = new ArrayList<String>();
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
				String data = new String(receivePacket.getData(), 0, receivePacket.length);
				String address = receivePacket.getAddress().getHostAddress();
				int fromPort = receivePacket.getPort();
				String message = String.format("%s:%s:%d", data, address, fromPort);
				messages.add(message)

			}
			socket.close();
		}catch (InterruptedException e) {
     		System.out.println("Thread " +  threadName + " interrupted.");
  		} catch (SocketException e) {
        	e.printStackTrace();
        }
	}
	public String[][] getReady()
	{
		String[][] addresses = new String[0][2];
		i = 0;
		for(String message : messages)
		{
			String[] messageData = messages.split(":");
			if(messageData[0] == "We really out here.")
			{
				String[] addrPort = {messageData[1], messageData[2]};
				addresses = Array.copyOf(addresses, addresses.length + 1);
				addresses[i] = addrPort;
				++i;
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
