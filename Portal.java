/*
 * @(#)Portal.java        1.0 2007/01/23
 *
 * Copyright Ben Zoller (University of Maryland, College Park), 2007
 * All rights reserved. Permission is granted for use and modification in CMSC420 
 * at the University of Maryland.
 */

//package cmsc420.geometry;

import java.util.Map;
import java.util.HashMap;

/**
 * Portal class is an analogue to a real-world portal in 3D space. Each portal
 * contains a location ((x,y,z) coordinates), name.
 */
public class Portal extends City {

	private Map<Integer, Road> nearestRoad = new HashMap<Integer, Road>();

	/**
	 * Constructs a portal.
	 * 
	 * @param name
	 *            name of the portal
	 * @param x
	 *            X coordinate of the portal
	 * @param y
	 *            Y coordinate of the portal
	 * @param Z
	 *            Z coordinate of the portal
	 */
	public Portal(final String name, final int x, final int y, final int z) {
		super(name, x, y, z, 0, null);
	}
	
	public Portal(final Portal portal) {
		this(portal.getName(), portal.getX(), portal.getY(), portal.getZ());	
	}
	
	public void setNearestRoad(int z, Road road) {
		this.nearestRoad.put(new Integer(z), road);
	}
	
	public Road getNearestRoad(int z) {
		return nearestRoad.get(new Integer(z));
	}

	/**
	 * Determines if this portal is equal to another object. The result is true if
	 * and only if the object is not null and a Portal object that contains the
	 * same name, X, Y and Z coordinates.
	 * 
	 * @param obj
	 *            the object to compare this portal against
	 * @return <code>true</code> if cities are equal, <code>false</code>
	 *         otherwise
	 */
	public boolean equals(final Object obj) {
		if (obj == this)
			return true;
		if (obj != null && (obj.getClass().equals(this.getClass()))) {
			Portal p = (Portal) obj;
			return (name.equals(p.name) && x==p.x && y==p.y && z==p.z);
		}
		return false;
	}
	
	public boolean equalsLoc(final Object obj) {
		if (obj == this)
			return true;
		if (obj != null && (obj.getClass().equals(this.getClass()))) {
			Portal p = (Portal) obj;
			return (x==p.x && y==p.y && z==p.z);
		}
		return false;
	}	
	
	

	/**
	 * Returns a hash code for this portal.
	 * 
	 * @return hash code for this portal
	 */
	public int hashCode() {
		int hash = 12;
		hash = 37 * hash + name.hashCode();
		hash = 37 * hash + x;
		hash = 37 * hash + y;
		hash = 37 * hash + z;			
		return hash;
	}
	
}