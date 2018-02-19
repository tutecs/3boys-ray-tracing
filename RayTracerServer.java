import java.io.*;
import java.net.*;
import java.util.*;
// Ported from scratchapixel.com's C++ ray tracer in 2018 by Ryan Yates
//
// [header]
// A very basic raytracer example.
// [/header]
// [compile]
// g++ -std=c++11 -o raytracer -O3 -Wall raytracer.cpp
// [/compile]
// [ignore]
// Copyright (C) 2012  www.scratchapixel.com
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
// [/ignore]


class Vec3f implements Serializable
{
	public float x, y, z;

	public Vec3f() { x = 0; y = 0; z = 0; }
	public Vec3f(float v) { x = v; y = v; z = v; }
	public Vec3f(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }

	public void normalize()
	{
		float nor2 = length2();
		if (nor2 > 0) {
			float invNor = 1f / (float)Math.sqrt(nor2);
			x *= invNor; y *= invNor; z *= invNor;
		}
	}

	public float dot(Vec3f v) { return x * v.x + y * v.y + z * v.z; }
	public Vec3f times(float f) { return new Vec3f(x * f, y * f, z * f); }
	public Vec3f vtimes(Vec3f f) { return new Vec3f(x * f.x, y * f.y, z * f.z); }
	public Vec3f add(Vec3f v) { return new Vec3f(x + v.x, y + v.y, z + v.z); }
	public Vec3f sub(Vec3f v) { return new Vec3f(x - v.x, y - v.y, z - v.z); }
	public Vec3f neg() { return new Vec3f(-x, -y, -z); }

	public float length2() { return x * x + y * y + z * z; }
	public float length() { return (float)Math.sqrt(length2()); }

	public String toString() {
		return String.format("%f %f %f", x, y, z);
	}
}

class ISect
{
	public final float t0, t1;
	public ISect(float t0, float t1) { this.t0 = t0; this.t1 = t1; }
}

class Sphere implements Serializable
{
	public Vec3f center;                           /// position of the sphere
	public float radius, radius2;                  /// sphere radius and radius^2
	public Vec3f surfaceColor, emissionColor;      /// surface color and emission (light)
	public float transparency, reflection;         /// surface transparency and reflectivity

	public Sphere(
		 Vec3f c,
		 float r,
		 Vec3f sc,
		 float refl,
		 float transp) {
		center = c;
		radius = r;
		radius2 = r * r;
		surfaceColor = sc;
		transparency = transp;
		reflection = refl;
		emissionColor = new Vec3f(0f);
	}

	public static Sphere Light(
		 Vec3f c,
		 Vec3f ec) {
		Sphere s = new Sphere(c,0,new Vec3f(0f),0,0);
		s.emissionColor = ec;
		return s;
	}

	// Compute a ray-sphere intersection using the geometric solution
	public ISect intersect( Vec3f rayorig,  Vec3f raydir)
	{
		Vec3f l = center.sub(rayorig);
		float tca = l.dot(raydir);
		if (tca < 0) return null;
		float d2 = l.dot(l) - tca * tca;
		if (d2 > radius2) return null;
		float thc = (float)Math.sqrt(radius2 - d2);
		return new ISect(tca - thc, tca + thc);
	}

	public String toString() {
		if (emissionColor.x > 0)
			return String.format("light %f %f %f  %f %f %f",
								center.x, center.y, center.z,
								emissionColor.x, emissionColor.y, emissionColor.z);
		return String.format("sphere %f %f %f  %f  %f %f %f  %f %f",
							center.x, center.y, center.z,
							radius,
							surfaceColor.x, surfaceColor.y, surfaceColor.z,
							transparency, reflection);
	}
}

class RayTracer
{
	// This variable controls the maximum recursion depth
	public static final int MAX_RAY_DEPTH = 7;
	public static final float INFINITY = 1e8f;

	static float mix( float a,  float b,  float mix) { return b * mix + a * (1 - mix); }

	// This is the main trace function. It takes a ray as argument (defined by its origin
	// and direction). We test if this ray intersects any of the geometry in the scene.
	// If the ray intersects an object, we compute the intersection point, the normal
	// at the intersection point, and shade this point using this information.
	// Shading depends on the surface property (is it transparent, reflective, diffuse).
	// The function returns a color for the ray. If the ray intersects an object that
	// is the color of the object at the intersection point, otherwise it returns
	// the background color.

	public static Vec3f trace(
		Vec3f rayorig,
		Vec3f raydir,
		List<Sphere> spheres,
		int depth)
	{
		//System.out.println(String.format("rayorg: %s, raydir: %s, depth: %d", rayorig.toString(), raydir.toString(), depth));
		float tnear = INFINITY;
		Sphere sphere = null;
		// find intersection of this ray with the sphere in the scene
		for (int i = 0; i < spheres.size(); ++i) {
			ISect t = spheres.get(i).intersect(rayorig, raydir);
			if (t != null) {
				float t0 = t.t0;
				if (t.t0 < 0) t0 = t.t1;
				if (t0 < tnear) {
					tnear = t0;
					sphere = spheres.get(i);
				}
			}
		}

		// if there's no intersection return black or background color
		if (sphere == null) return new Vec3f(2f);
		//System.out.println(sphere);
		Vec3f surfaceColor = new Vec3f(0f); // color of the ray/surfaceof the object intersected by the ray
		Vec3f phit = rayorig.add(raydir.times(tnear)); // point of intersection
		Vec3f nhit = phit.sub(sphere.center); // normal at the intersection point
		nhit.normalize(); // normalize normal direction
		// If the normal and the view direction are not opposite to each other
		// reverse the normal direction. That also means we are inside the sphere so set
		// the inside boolean to true. Finally reverse the sign of IdotN which we want
		// positive.
		float bias = 1e-4f; // add some bias to the point from which we will be tracing
		boolean inside = false;
		if (raydir.dot(nhit) > 0) { nhit = nhit.neg(); inside = true; }
		if ((sphere.transparency > 0 || sphere.reflection > 0) && depth < MAX_RAY_DEPTH) {
			float facingratio = -raydir.dot(nhit);
			// change the mix value to tweak the effect
			float fresneleffect = mix((float)Math.pow(1 - facingratio, 3), 1f, 0.1f);
			// compute reflection direction (not need to normalize because all vectors
			// are already normalized)
			Vec3f refldir = raydir.sub(nhit.times(2f * raydir.dot(nhit)));
			refldir.normalize();
			Vec3f reflection = trace(phit.add(nhit.times(bias)), refldir, spheres, depth + 1);
			Vec3f refraction = new Vec3f(0f);
			// if the sphere is also transparent compute refraction ray (transmission)
			if (sphere.transparency > 0) {
				float ior = 1.1f, eta = inside ? ior : 1f / ior; // are we inside or outside the surface?
				float cosi = -nhit.dot(raydir);
				float k = 1f - eta * eta * (1f - cosi * cosi);
				Vec3f refrdir = raydir.times(eta).add(nhit.times(eta *  cosi - (float)Math.sqrt(k)));
				refrdir.normalize();
				refraction = trace(phit.sub(nhit.times(bias)), refrdir, spheres, depth + 1);
			}
			// the result is a mix of reflection and refraction (if the sphere is transparent)
			surfaceColor = sphere.surfaceColor.vtimes(reflection.times(fresneleffect).add(refraction.times((1f - fresneleffect) * sphere.transparency)));
			//System.out.println(String.format("surfaceColor: %s", surfaceColor.toString()));
		}
		else {
			// it's a diffuse object, no need to raytrace any further
			for (int i = 0; i < spheres.size(); ++i) {
				Vec3f ec = spheres.get(i).emissionColor;
				if (ec.x > 0) {
					// this is a light
					float transmission = 1f;
					Vec3f lightDirection = spheres.get(i).center.sub(phit);
					lightDirection.normalize();
					for (int j = 0; j < spheres.size(); ++j) {
						if (i != j) {
							if (spheres.get(j).intersect(phit.add(nhit.times(bias)), lightDirection) != null) {
								transmission = 0f;
								break;
							}
						}
					}
					surfaceColor = surfaceColor.add(
						sphere.surfaceColor.times(transmission).vtimes(
							ec.times((float)Math.max(0, nhit.dot(lightDirection)))));
				}
			}
		}

		//System.out.println(String.format("surfaceColor: %s, ec: %s", surfaceColor.toString(), sphere.emissionColor.toString()));
		return surfaceColor.add(sphere.emissionColor);
	}

	static int toByte(float x) {
		return (int)(Math.min(1f, x) * 255f);
	}


	static void writePPM(String fileName, Vec3f[][] bitmap, int width, int height) {
		BufferedWriter o = null;
		try {
			o = new BufferedWriter(new FileWriter(fileName));
			String header = String.format("P3 %d %d 255\n", width, height);
			o.write(header);
			o.newLine();
			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					Vec3f c = bitmap[x][y];
					o.write(String.format("%d %d %d ", toByte(c.x), toByte(c.y), toByte(c.z)));
				}
				o.newLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (o != null)
					o.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// Main rendering function. We compute a camera ray for each pixel of the image
	// trace it and return a color. If the ray hits a sphere, we return the color of the
	// sphere at the intersection point, else we return the background color.
	public static void render(List<Sphere> spheres, int[] xs, DatagramSocket socket, InetAddress address, int port)
	{
		try
		{
			//int width = 640, height = 480;
			int width = 640*2, height = 480*2;
			float fov = 45f;

			// Vec3f[][] screen = new Vec3f[width][height];

			float invWidth = 1 / (float)width, invHeight = 1 / (float)height;
			float aspectratio = (float)width / (float)height;
			float angle = (float)Math.tan((float)Math.PI * 0.5f * fov / 180.0f);
			// Trace rays
			for (Integer x : xs) {
				Vec3f[] currentCol = new Vec3f[height];
				for (int y = 0; y < height; ++y) {
					//System.out.println(String.format("Pixel: %d, %d", x, y));
					float xx = (2f * ((x + 0.5f) * invWidth) - 1f) * angle * aspectratio;
					float yy = (1f - 2f * ((y + 0.5f) * invHeight)) * angle;

					Vec3f raydir = new Vec3f(xx, yy, -1f);
					raydir.normalize();
					Vec3f vecData = trace(new Vec3f(0f), raydir, spheres, 0);
					currentCol[y] = vecData;
				}
				String sendString = String.format("rowcolor:%d:%s", x, makeColString(currentCol));
				byte[] data = sendString.getBytes();
				DatagramPacket sendData = new DatagramPacket(data, data.length, address, port);
				socket.send(sendData);
			}
		}
		catch (UnknownHostException e) {
        e.printStackTrace();
        } catch (SocketException e) {
        e.printStackTrace();
        } catch (IOException e) {
        e.printStackTrace();
        }

		// Save result to a PPM image (keep these flags if you compile under Windows)
		// writePPM(filename, screen, width, height);
		// return screen matrix
	}

	static String makeColString(Vec3f[] col)
	{
		String colString = "";
		for(int i = 0; i < col.length - 1; ++i)
		{
			colString = colString + col[i].toString() + ":";
		}
		colString = colString + col[col.length-1];
		return colString;
	}

	static float num(StreamTokenizer st) throws IOException {
		if (st.nextToken() != StreamTokenizer.TT_NUMBER) {
			System.err.println("ERROR: number expected in line "+st.lineno());
			throw new IOException(st.toString());
		}
		return (float)st.nval;
	}

	static List<Sphere> readScene(String filename) throws IOException
	{
		List<Sphere> spheres = new ArrayList<Sphere>();
		Reader reader = new BufferedReader(new FileReader(filename));
		StreamTokenizer st = new StreamTokenizer(reader);
		st.commentChar('#');
		scan: while (true) {
			switch (st.nextToken()) {
				default:
					break scan;
				case StreamTokenizer.TT_WORD:
					if (st.sval.equals("sphere")) {
						spheres.add(new Sphere(
							new Vec3f(num(st), num(st), num(st)), num(st),
							new Vec3f(num(st), num(st), num(st)), num(st), num(st)));
						//System.out.println(spheres.get(spheres.size()-1));
					} else if (st.sval.equals("light")) {
						spheres.add(Sphere.Light(
							new Vec3f(num(st), num(st), num(st)),
							new Vec3f(num(st), num(st), num(st))));
						//System.out.println(spheres.get(spheres.size()-1));
					} else {
						System.out.print(String.format("Unknown token: %s\n", st.sval));
					}
					break;
			}
		}
		if (st.ttype != StreamTokenizer.TT_EOF)
			throw new IOException(st.toString());
		return spheres;
	}

	// public static void main(String args[]) throws IOException {
	//     if (args.length != 2) {
	//         System.out.println("Usage: java RayTracer [input.scene] [output.ppm]");
	//         System.exit(1);
	//         return;
	//     }
	//
	//     List<Sphere> spheres = new ArrayList<Sphere>();
	//     readScene(args[0], spheres);
	//     render(args[1], spheres);
	// }
}
