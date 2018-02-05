// 3boys client for raytracer
// Ethan Duryea, Joshua Weller, John Zamites
// Version 1.0

import java.net.*;
import java.io.*;

public class RayTracerClient
{

    public static void main(String[] args) throws IOException
    {
        if(args.length <= 2)
        {
            System.out.println("Usage: java RayTracerClient [input.scene] [output.ppm] [address1] ... [addressN]")
            System.exit(1);
            return;
        }
        String sceneFile = args[0];
        String outputFile = args[1];
        int numServers = args.length-2;
        String[] servers = new String[numServers];
    }
}
