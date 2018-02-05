// 3boys client for raytracer
// Ethan Duryea, Joshua Weller, John Zamites
// Version 1.0

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class RayTracerClient
{

    public static void main(String[] args) throws IOException
    {
        if(args.length != 2)
        {
            System.out.println("Usage: java RayTracerClient [input.scene] [output.ppm]")
            System.exit(1);
            return;
        }
        String sceneFile = args[0];
        String outputFile = args[1];
        String[] addresses = getAddresses();
    }
    public static String[] getAddresses()
    {
        try
        {
            List<String> addresses = new ArrayList<String>();
            File addressFile = new File("SERVER_LIST");
            Scanner sc = new Scanner(addressFile);
            while(sc.hasNextLine())
            {
                addresses.append(sc.nextLine());
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return addresses.toArray();
    }
}
