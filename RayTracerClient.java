// 3boys client for raytracer
// Ethan Duryea, Joshua Weller, John Zamites, Elliot Spicer
// Version 1.0

import java.net.*;
import java.io.*;
import java.util.*;


public class RayTracerClient
{
    private static int outPort = 3333;
    private static int inPort = 4444;
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
        sendSpheres(addresses, spheres);

        int[][] xs = divideWork(addresses.length);

		for(int i = 0; i < addresses.length; ++i)
		{
            // Send render request to each node.
            // xs[i] contains the starting width and the ending width for the render request
			sendRender(addresses[i], xs[i]);
		}
        receive(addresses, xs);
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
// l is length and d random outside the loop 

      public String sendSpheres(InetAddress addresses[], Sphere[] spheres) {
            Random rand = new Random();
            int d = rand.nextInt(100);
            for(InetAddress address : addresses){        
                for(int i = 0; i < spheres.length; i++;){                
                    String sphereStr;
                    DatagramSocket socket = new DatagramSocket();
                    sphereStr = String.format("%d, %d, %s" l, d, spheres[i].toString);
                    buf = sphereStr.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, outport);
                    socket.send(packet);
                    packet = new DataGramPacket(buf, buf.length)
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    return received;
                }   
            }



      }

   /* public static void sendSpheres(InetAddress[] addresses, List<Sphere> spheres)
    {
        //spherePacket(Sphere sphere, int i, int d)  http://www.coderpanda.com/java-socket-programming-transferring-java-object-through-socket-using-udp/
        for(InetAddress address : addresses)
        {
            

            }

            try
            { 
                



                DatagramSocket socket = new DatagramSocket();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ObjectOutputStream os = new ObjectOutputStream(outputStream);
                os.writeObject(spheres);
                byte[] data = outputStream.toByteArray();
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
    }

*/


    public static void sendRender(InetAddress address, int[] xs)
    {
		int xStart = xs[0];
		int xStop = xs[1];
		try
		{
			DatagramSocket socket = new DatagramSocket();
            byte[] data = String.format("xrange %d %d", xStart, xStop).getBytes();
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
    /* public static int[][] divideWork(int nNodes)
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
    */
    
}
