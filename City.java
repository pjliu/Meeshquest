/*
 * @(#)City.java        1.0 2007/01/23
 *
 * Copyright Ben Zoller (University of Maryland, College Park), 2007
 * All rights reserved. Permission is granted for use and modification in CMSC420 
 * at the University of Maryland.
 */

//package cmsc420.geometry;
import java.awt.geom.Point2D;

/**
 * City class is an analogue to a real-world city in 3D space. Each city
 * contains a location ((x,y,z) coordinates), name, radius, and color.
 * <p>
 * Useful <code>java.awt.geom.Point2D</code> methods (such as distance() in x,y plan) can
 * be utilized by calling toPoint2D(), which creates a Point2D copy of this
 * city's (x,y) location.
 */
public class City extends Geometry {
	/** name of this city */
	protected String name;

	/** coordinates of this city */
	protected int x;
	protected int y;
	protected int z;

	protected int radius;

	/** color of this city */
	protected String color;
	
	/**
	 * Constructs a city.
	 * 
	 * @param name
	 *            name of the city.
	 * @param x
	 *            X coordinate of the city.
	 * @param y
	 *            Y coordinate of the city.
	 * @param Z
	 *            Z coordinate of the city.
	 * @param radius
	 *            radius of the city
	 * @param color
	 *            color of the city
	 */
	public City(final String name, final int x, final int y, final int z, final int radius, final String color) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.radius = radius;
		this.color = color;
	}
	
	public City(final City city){
		this(city.getName(), city.getX(), city.getY(), city.getZ(), city.getRadius(), city.getColor());
	}

	/**
	 * Gets the name of this city.
	 * 
	 * @return name of this city.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the X coordinate of this city.
	 * 
	 * @return X coordinate of this city
	 */
	public int getX() {
		return x;
	}

	/**
	 * Gets the Y coordinate of this city.
	 * 
	 * @return Y coordinate of this city
	 */
	public int getY() {
		return y;
	}

	/**
	 * Gets the Z coordinate of this city.
	 * 
	 * @return Z coordinate of this city
	 */
	public int getZ() {
		return z;
	}
	
	/**
	 * Gets the radius of this city.
	 * 
	 * @return radius of this city.
	 */
	public int getRadius() {
		return radius;
	}
	
	/**
	 * Gets the color of this city.
	 * 
	 * @return color of this city
	 */
	public String getColor() {
		return color;
	}

	/**
	 * Determines if this city is equal to another object. The result is true if
	 * and only if the object is not null and a City object that contains the
	 * same name, X, Y and Z coordinates, radius, and color.
	 * 
	 * @param obj
	 *         the object to compare this city against
	 * @return <code>true</code> if cities are equal, <code>false</code>
	 *         otherwise
	 */
	public boolean equals(final Object obj) {
		if (obj == this)
			return true;
		if (obj != null && (obj.getClass().equals(this.getClass()))) {
			City c = (City) obj;
			return (name.equals(c.name) && x==c.x && y==c.y && z==c.z && radius==c.radius && color.equals(c.color));
		}
		return false;
	}

	/**
	 * Returns a hash code for this city.
	 * 
	 * @return hash code for this city
	 */
	public int hashCode() {
		int hash = 12;
		hash = 37 * hash + name.hashCode();
		hash = 37 * hash + x;
		hash = 37 * hash + y;
		hash = 37 * hash + z;		
		hash = 37 * hash + radius;
		hash = 37 * hash + color.hashCode();		
		return hash;
	}

	/**
	 * Returns an (x,y,z) representation of the city. Important: casts the x, y and z
	 * coordinates to integers.
	 * 
	 * @return string representing the location of the city
	 */
	public String getLocationString() {
		final StringBuilder location = new StringBuilder();
		location.append("(");
		location.append(x);
		location.append(",");
		location.append(y);
		location.append(",");
		location.append(z);
		location.append(")");
		return location.toString();
	}


	/**
	 * Returns a Point2D instance representing the City's location in x y plane.
	 * 
	 * @return location in x y plane of this city
	 */
	public Point2D toPoint2D() {
		return new Point2D.Float(x, y);
	}
	
	public String toString() {
		return getLocationString();
	}

	@Override
	public int getType() {
		return POINT;
	}
}