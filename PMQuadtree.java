//package cmsc420.pmquadtree;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Iterator;

/*
import cmsc420.geom.Inclusive2DIntersectionVerifier;
import cmsc420.geometry.City;
import cmsc420.geometry.Geometry;
import cmsc420.geometry.Road;
import cmsc420.geometry.RoadNameComparator;
*/

public abstract class PMQuadtree {

	/** stores all mapped roads in the PM Quadtree */
	final protected TreeSet<Road> allRoads;
	
	/** stores all mapped portals in the PM Quadtree */
	final protected HashMap<String, Portal> allPortals;
	
	/** stores how many roads are connected to each city */
	final protected HashMap<String, Integer> numRoadsForCity;
	
	/** number of portals*/
	protected int numPortals;
	
	/** root of the PM Quadtree */
	protected Node root;

	/** spatial width of the PM Quadtree */
	final protected int spatialWidth;

	/** spatial height of the PM Quadtree */
	final protected int spatialHeight;

	/** spatial origin of the PM Quadtree (i.e. (0,0)) */
	final protected Point2D.Float spatialOrigin;

	/** validator for the PM Quadtree */
	final protected Validator validator;

	/** singleton white node */
	final protected White white = new White();

	/** order of the PM Quadtree (one of: {1,2,3}) */
	final protected int order;


///////////////////////////////////////////////////	///////////////////////////////////////////////////
	public abstract class Node {
		/** Type flag for an empty PM Quadtree leaf node */
		public static final int WHITE = 0;

		/** Type flag for a non-empty PM Quadtree leaf node */
		public static final int BLACK = 1;

		/** Type flag for a PM Quadtree internal node */
		public static final int GRAY = 2;

		/** type of PR Quadtree node (either empty, leaf, or internal) */
		protected final int type;

		/**
		 * Constructor for abstract Node class.
		 * 
		 * @param type
		 *            type of the node (either empty, leaf, or internal)
		 */
		protected Node(final int type) {
			this.type = type;
		}

		/**
		 * Gets the type of this PM Quadtree node. One of: BLACK, WHITE, GRAY.
		 * 
		 * @return type of this PM Quadtree node
		 */
		public int getType() {
			return type;
		}

		/**
		 * Adds a geometry to this PM Quadtree node.
		 * 
		 * @param g
		 *            geometry to be added
		 * @param origin
		 *            origin of the rectangular bounds of this node
		 * @param width
		 *            width of the rectangular bounds of this node
		 * @param height
		 *            height of the rectangular bounds of this node
		 * @return this node after the geometry has been added
		 */
		public Node add(final Geometry g, final Point2D.Float origin, final int width, final int height) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Removes a geometry from this PM Quadtree node.
		 * 
		 * @param g
		 *            geometry to be removed
		 * @param origin
		 *            origin of the rectangular bounds of this node
		 * @param width
		 *            width of the rectangular bounds of this node
		 * @param height
		 *            height of the rectangular bounds of this node
		 * @return this node after the geometry has been removed
		 */
		public Node remove(final Geometry g, final Point2D.Float origin, final int width, final int height) {
			throw new UnsupportedOperationException();
		}
		
		/**
		 * Returns if this node follows the rules of the PM Quadtree.
		 * 
		 * @return <code>true</code> if the node follows the rules of the PM
		 *         Quadtree; <code>false</code> otherwise
		 */
		public boolean isValid() {
			throw new UnsupportedOperationException();
		}

	}

///////////////////////////////////////////////////	///////////////////////////////////////////////////
	/**
	 * White class represents an empty PM Quadtree leaf node.
	 */
	public class White extends Node {
		/**
		 * Constructs and initializes an empty PM Quadtree leaf node.
		 */
		public White() {
			super(WHITE);
		}

		public Node add(final Geometry g, final Point2D.Float origin, final int width, final int height) {
			final Black blackNode = new Black();
			return blackNode.add(g, origin, width, height);
		}

		public Node remove(final Geometry g, final Point2D.Float origin, final int width, final int height) {
			/* this is necessary for cleanup operations */
			return this;
		}
		
		/**
		 * Returns if this node follows the rules of the PM Quadtree.
		 * 
		 * @return <code>true</code> if the node follows the rules of the PM
		 *         Quadtree; <code>false</code> otherwise
		 */
		public boolean isValid() {
			return true;
		}
		
		/**
		 * Gets all the geometries contained by this node and its descendant nodes
		 * (if applicable).
		 * 
		 * @return list of geometries contained by this node
		 */
		public LinkedList<Geometry> getGeometry() {
			throw new UnsupportedOperationException();
		}		

		public String toString() {
			return "white";
		}
	}

///////////////////////////////////////////////////	///////////////////////////////////////////////////
	/**
	 * Black class represents a non-empty PM Quadtree leaf node. Black nodes are
	 * capable of storing both cities (points) and roads (line segments).
	 * <p>
	 * Each black node stores cities, roads and portals into its own sorted geometry
	 * list.
	 * <p>
	 * Black nodes are split into a gray node if they do not satisfy the rules
	 * of the PM Quadtree.
	 */
	public class Black extends Node {

		/** list of cities, roads and portals contained within black node */
		final protected LinkedList<Geometry> geometry;

		/** number of cities/portals contained within this black node */
		protected int numPoints;
		
		/**
		 * Constructs and initializes a non-empty PM Quadtree leaf node.
		 */
		public Black() {
			super(BLACK);
			geometry = new LinkedList<Geometry>();
			numPoints = 0;
		}

		/**
		 * Gets a linked list of the cities and roads contained by this black
		 * node.
		 * 
		 * @return list of cities, roads contained within this black node
		 */
		public LinkedList<Geometry> getGeometry() {
			return geometry;
		}

		/**
		 * Gets the index of the geometry in this black node's geometry list.
		 * 
		 * @param g
		 *            geometry to be searched for in the sorted geometry list
		 * @return index of the search key, if it is contained in the list;
		 *         otherwise, (-(insertion point) - 1)
		 */
		private int getIndex(final Geometry g) {
			return Collections.binarySearch(geometry, g);
		}

		/**
		 * Adds a geometry to this black node. After insertion, if the node becomes
		 * invalid, it will be split into a Gray node.
		 */
		public Node add(final Geometry g, final Point2D.Float origin, final int width, final int height) {
			if (g instanceof Road) {
				// g is a road
				Road r = (Road)g;
				/* create region rectangle */
				final Rectangle2D.Float rect = new Rectangle2D.Float(origin.x,origin.y, width, height);
				
				/* check if start point intersects with region */
				if (Inclusive2DIntersectionVerifier.intersects(r.getStart().toPoint2D(), rect)) {
					addGeometryToList(r.getStart());
				}
	
				/* check if end point intersects with region */
				if (Inclusive2DIntersectionVerifier.intersects(r.getEnd().toPoint2D(), rect)) {
					addGeometryToList(r.getEnd());
				}
				
			}

			/* add the road or portal to the geometry list */
			addGeometryToList(g);
			
			/* check if this node is valid */
			if (isValid()) {
				/* valid so return this black node */
				return this;
			} else {
				/* invalid so partition into a Gray node */
				return partition(origin, width, height);
			}
		}

		/**
		 * Adds a geometry to this node's geometry list.
		 * 
		 * @param g
		 *            geometry to be added
		 */
		private boolean addGeometryToList(final Geometry g) {
			/* search for the non-existent item */
			final int index = getIndex(g);

			/* add the non-existent item to the list */
			if (index < 0) {
				geometry.add(-index - 1, g);

				if (g instanceof City || g instanceof Portal) {
					// g is a city or portal
					numPoints++;
					if (g instanceof Portal) {
						allPortals.put(((Portal)g).getName(), ((Portal)g));
						numPortals++;
					}
				}

				return true;
			}
			return false;
		}

		/**
		 * Removes a geometry from this PM Quadtree node.
		 * 
		 */
		public Node remove(final Geometry g, final Point2D.Float origin, final int width, final int height) {
			if (g instanceof Road) {			
				// g is a road
				Road r = (Road)g;
				/* create region rectangle */
				final Rectangle2D.Float rect = new Rectangle2D.Float(origin.x,origin.y, width, height);
	
				/* check if start point intersects with region */
				if (Inclusive2DIntersectionVerifier.intersects(r.getStart().toPoint2D(), rect)) {
					removeGeometryFromList(r.getStart());
				}
	
				/* check if end point intersects with region */
				if (Inclusive2DIntersectionVerifier.intersects(r.getEnd().toPoint2D(), rect)) {
					removeGeometryFromList(r.getEnd());
				}
			}

			/* remove the road or portal */
			removeGeometryFromList(g);

			if (geometry.isEmpty()) {
				return white;
			} else {
				return this;
			}
		}

		/**
		 * Removes a specified geometry from this black node.
		 * 
		 * @param g
		 *            geometry to be removed
		 * @return if the geometry was successfully removed from this black node
		 */
		private boolean removeGeometryFromList(final Geometry g) {
			final int index = getIndex(g);
			if (index >= 0) {
				geometry.remove(index);

				if (g instanceof City || g instanceof Portal) {
					numPoints--;
					if (g instanceof Portal) {
						allPortals.remove(((Portal)g).getName());
						numPortals--;
					}
				}
				return true;
			}
			return false;
		}



		/**
		 * Returns if this node follows the rules of the PM Quadtree.
		 * 
		 * @return <code>true</code> if the node follows the rules of the PM
		 *         Quadtree; <code>false</code> otherwise
		 */
		public boolean isValid() {
			return validator.valid(this);
		}

		/**
		 * Gets the number of cities contained in this black node.
		 * 
		 * @return number of cities contained in this black node
		 */
		public int getNumPoints() {
			return numPoints;
		}

		/**
		 * Partitions an invalid back node into a gray node and adds this black
		 * node's roads to the new gray node.
		 * 
		 * @param origin
		 *            origin of the rectangular bounds of this node
		 * @param width
		 *            width of the rectangular bounds of this node
		 * @param height
		 *            height of the rectangular bounds of this node
		 * @return the new gray node
		 * @throws InvalidPartitionThrowable
		 *             if the quadtree was partitioned too deeply
		 * @throws IntersectingRoadsThrowable
		 *             if two roads intersect
		 */
		private Node partition(final Point2D.Float origin, final int width, final int height) 
		{		
			/* create new gray node */
			Node gray = new Gray(origin, width, height);

			// add portal only; endpoints of roads are added in recursive calls
			// to black.add()
			for (int i = 0; i < numPoints; i++) {
				final Geometry g = geometry.get(i);
				if (g instanceof Portal) {
					gray = gray.add(g, origin, width, height);
				}
			}			
			// add roads
			for (int i = numPoints; i < geometry.size(); i++) {
				final Geometry g = geometry.get(i);
				gray = gray.add(g, origin, width, height);
			}
			return gray;
		}

		/**
		 * Returns a string representing this black node and its road list.
		 * 
		 * @return a string representing this black node and its road list
		 */
		public String toString() {
			return "black: " + geometry.toString();
		}

		/**
		 * Returns if this black node contains a city.
		 * 
		 * @return if this black node contains a city
		 */
		public boolean containsCity() {
			return (numPoints - numPortals > 0);
		}

		/**
		 * @return true if this black node contains at least a road
		 */
		public boolean containsRoad() {
			return (geometry.size() - numPoints) > 0;
		}

		/**
		 * If this black node contains a city, returns the city contained within
		 * this black node. Else returns <code>null</code>.
		 * 
		 * @return the city if it exists, else <code>null</code>
		 */
		public City getCity() {
			final Geometry g = geometry.getFirst();
			return g instanceof City ? (City)g : null;
		}

		public Portal getPortal() {
			final Geometry g = geometry.getFirst();
			return g instanceof Portal ? (Portal)g : null;
		}		
	}

///////////////////////////////////////////////////	///////////////////////////////////////////////////
	/**
	 * Gray class represents an internal PM Quadtree node.
	 */
	public class Gray extends Node {
		/** this gray node's 4 child nodes */
		final protected Node[] children;

		/** regions representing this gray node's 4 child nodes */
		final protected Rectangle2D.Float[] regions;

		/** origin of the rectangular bounds of this node */
		final protected Point2D.Float origin;

		/** the origin of rectangular bounds of each of the node's child nodes */
		final protected Point2D.Float[] origins;

		/** half the width of the rectangular bounds of this node */
		final protected int halfWidth;

		/** half the height of the rectangular bounds of this node */
		final protected int halfHeight;

		/**
		 * Constructs and initializes an internal PM Quadtree node.
		 * 
		 * @param origin
		 *            origin of the rectangular bounds of this node
		 * @param width
		 *            width of the rectangular bounds of this node
		 * @param height
		 *            height of the rectangular bounds of this node
		 */
		public Gray(final Point2D.Float origin, final int width, final int height) {
			super(GRAY);

			/* set this node's origin */
			this.origin = origin;

			/* initialize the children as white nodes */
			children = new Node[4];
			for (int i = 0; i < 4; i++) {
				children[i] = white;
			}

			/* get half the width and half the height */
			halfWidth = width >> 1;
			halfHeight = height >> 1;

			/* initialize the child origins */
			origins = new Point2D.Float[4];
			origins[0] = new Point2D.Float(origin.x, origin.y + halfHeight);
			origins[1] = new Point2D.Float(origin.x + halfWidth, origin.y + halfHeight);
			origins[2] = new Point2D.Float(origin.x, origin.y);
			origins[3] = new Point2D.Float(origin.x + halfWidth, origin.y);

			/* initialize the child regions */
			regions = new Rectangle2D.Float[4];
			for (int i = 0; i < 4; i++) {
				regions[i] = new Rectangle2D.Float(origins[i].x, origins[i].y, halfWidth, halfHeight);
			}
		}

		/**
		 * Adds a road to this PM Quadtree node.
		 * 
		 * @param g
		 *            road to be added
		 * @param origin
		 *            origin of the rectangular bounds of this node
		 * @param width
		 *            width of the rectangular bounds of this node
		 * @param height
		 *            height of the rectangular bounds of this node
		 * @return this node after the city has been added
		 * @throws InvalidPartitionThrowable
		 *             if the map if partitioned too deeply
		 * @throws IntersectingRoadsThrowable
		 *             if this road intersects with another road
		 */
		public Node add(final Geometry g, final Point2D.Float origin, final int width, final int height) {
			if (g instanceof Portal) {
				for (int i = 0; i < 4; i++) {
					if (Inclusive2DIntersectionVerifier.intersects(((Portal)g).toPoint2D(), regions[i])) {
						children[i] = children[i].add(g, origins[i], halfWidth, halfHeight);
					}
				}
			} else if (g instanceof Road) {
				for (int i = 0; i < 4; i++) {
					if (Inclusive2DIntersectionVerifier.intersects(((Road)g).toLine2D(), regions[i])) {
						children[i] = children[i].add(g, origins[i], halfWidth, halfHeight);
					}
				}
			}			

			return this;
		}


		/**
		 * Removes a road from this PM Quadtree node.
		 * 
		 * @param g
		 *            road to be removed
		 * @param origin
		 *            origin of the rectangular bounds of this node
		 * @param width
		 *            width of the rectangular bounds of this node
		 * @param height
		 *            height of the rectangular bounds of this node
		 * @return this node after the city has been added
		 */
		public Node remove(Geometry g, Point2D.Float origin, int width, int height) {
			if (g instanceof Portal) {
				for (int i = 0; i < 4; i++) {
					if (Inclusive2DIntersectionVerifier.intersects(((Portal)g).toPoint2D(), regions[i])) {
						children[i] = children[i].remove(g, origins[i], halfWidth, halfHeight);
					}
				}
			} else if (g instanceof Road) {
				for (int i = 0; i < 4; i++) {
					if (Inclusive2DIntersectionVerifier.intersects(((Road)g).toLine2D(), regions[i])) {
						children[i] = children[i].remove(g, origins[i], halfWidth, halfHeight);
					}
				}
			}

			if (numWhiteChildren() == 4) {
				return white;

			} else if (numWhiteChildren() == 3 && numBlackChildren() == 1) {
				for (final Node node : children) {
					if (node.getType() == BLACK) {
						return node;
					}
				}
				return this;

			} else if (numBlackChildren() >= 1 || numWhiteChildren() >= 1) {
				final Black b = new Black();
				for (final Node node : children) {
					if (node.getType() != WHITE) {
						LinkedList<Geometry> geometryList = node.getType() == BLACK ? ((Black)node).getGeometry() : ((Gray)node).getGeometry();
						for (final Geometry g1 : geometryList) {
							b.addGeometryToList(g1);
						}						
					}
				}

				if (b.isValid()) {
					return b;
				} else {
					return this;
				}
			} else {
				return this;
			}
		}

		private int numWhiteChildren() {
			int total = 0;
			for (final Node n : children) {
				if (n == white) {
					total++;
				}
			}
			return total;
		}

		private int numBlackChildren() {
			int total = 0;
			for (final Node n : children) {
				if (n.getType() == BLACK) {
					total++;
				}
			}
			return total;
		}

		/**
		 * Returns if this node follows the rules of the PM Quadtree.
		 * 
		 * @return <code>true</code> if the node follows the rules of the PM
		 *         Quadtree; <code>false</code> otherwise
		 */
		public boolean isValid() {
			return children[0].isValid() && 
			       children[1].isValid() && 
				   children[2].isValid() && 
				   children[3].isValid();
		}

		public String toString() {
			StringBuilder grayStringBuilder = new StringBuilder("gray:");
			for (Node child : children) {
				grayStringBuilder.append("\n\t");
				grayStringBuilder.append(child.toString());
			}
			return grayStringBuilder.toString();
		}

		public LinkedList<Geometry> getGeometry() {
			final LinkedList<Geometry> geometry = new LinkedList<Geometry>();
			for (Node child : children) {
				if (child.getType() != WHITE) {					
					LinkedList<Geometry> geometryList = child.getType() == BLACK ? ((Black)child).getGeometry() : ((Gray)child).getGeometry();
					geometry.addAll(geometryList);
				}
			}
			return geometry;
		}
		
		/**
		 * Gets the child node of this node according to which quadrant it falls
		 * in.
		 * 
		 * @param quadrant
		 *            quadrant number (top left is 0, top right is 1, bottom
		 *            left is 2, bottom right is 3)
		 * @return child node
		 */
		public Node getChild(final int quadrant) {
			if (quadrant < 0 || quadrant > 3) {
				throw new IllegalArgumentException();
			} else {
				return children[quadrant];
			}
		}

		/**
		 * Gets the rectangular region for the specified child node of this
		 * internal node.
		 * 
		 * @param quadrant
		 *            quadrant that child lies within
		 * @return rectangular region for this child node
		 */
		public Rectangle2D.Float getChildRegion(int quadrant) {
			if (quadrant < 0 || quadrant > 3) {
				throw new IllegalArgumentException();
			} else {
				return regions[quadrant];
			}
		}

		/**
		 * Gets the center X coordinate of this node's rectangular bounds.
		 * 
		 * @return center X coordinate of this node's rectangular bounds
		 */
		public int getCenterX() {
			return (int) origin.x + halfWidth;
		}

		/**
		 * Gets the center Y coordinate of this node's rectangular bounds.
		 * 
		 * @return center Y coordinate of this node's rectangular bounds
		 */
		public int getCenterY() {
			return (int) origin.y + halfHeight;
		}

		/**
		 * Gets half the width of this internal node.
		 * 
		 * @return half the width of this internal node
		 */
		public int getHalfWidth() {
			return halfWidth;
		}

		/**
		 * Gets half the height of this internal node.
		 * 
		 * @return half the height of this internal node
		 */
		public int getHalfHeight() {
			return halfHeight;
		}
	}

	
///////////////////////////////////////////////////	///////////////////////////////////////////////////	
	public PMQuadtree(final Validator validator, final int spatialWidth, final int spatialHeight, final int order) {
		if (order != 1 && order != 3) {
			throw new IllegalArgumentException("order must be one of: {1,3}");
		}

		root = white;
		this.validator = validator;
		this.spatialWidth = spatialWidth;
		this.spatialHeight = spatialHeight;
		spatialOrigin = new Point2D.Float(0.0f, 0.0f);
		allRoads = new TreeSet<Road>(new RoadNameComparator());
		numRoadsForCity = new HashMap<String, Integer>();
		allPortals = new HashMap<String, Portal>();
		this.order = order;
	}

	public Node getRoot() {
		return root;
	}
	
	public void addRoad(final Road g) 
			throws RoadAlreadyMappedThrowable, OutOfBoundsThrowable, RoadIntersectsAnotherRoadThrowable, RoadViolatesPMRulesThrowable {
		final Road g2 = new Road(g.getEnd(), g.getStart());

		if (allRoads.contains(g) || allRoads.contains(g2)) {
			throw new RoadAlreadyMappedThrowable();
		}		

		Rectangle2D.Float world = new Rectangle2D.Float(spatialOrigin.x, spatialOrigin.y, spatialWidth, spatialHeight);		
		if (!Inclusive2DIntersectionVerifier.intersects(g.toLine2D(), world)) {
			throw new OutOfBoundsThrowable();
		}		
		root = root.add(g, spatialOrigin, spatialWidth, spatialHeight);
		allRoads.add(g);
		if (Inclusive2DIntersectionVerifier.intersects(g.getStart().toPoint2D(), world)) {
			increaseNumRoadsMap(g.getStart().getName());
		}
		if (Inclusive2DIntersectionVerifier.intersects(g.getEnd().toPoint2D(), world)) {
			increaseNumRoadsMap(g.getEnd().getName());
		}
	}
	
	public void removeRoad(final Road g) throws RoadNotMappedThrowable {
		if (allRoads.contains(g)) {
			root = root.remove(g, spatialOrigin, spatialWidth, spatialHeight);

			allRoads.remove(g);
			decreaseNumRoadsMap(g.getStart().getName());
			decreaseNumRoadsMap(g.getEnd().getName());
		} else {
			throw new RoadNotMappedThrowable();
		}
	}	
	
	public void addPortal(final Portal p) 
			throws OutOfBoundsThrowable, PortalViolatesPMRulesThrowable {	
		Rectangle2D.Float world = new Rectangle2D.Float(spatialOrigin.x, spatialOrigin.y, spatialWidth, spatialHeight);		

		if (!Inclusive2DIntersectionVerifier.intersects(p.toPoint2D(), world)) {
			throw new OutOfBoundsThrowable();
		}
		root = root.add(p, spatialOrigin, spatialWidth, spatialHeight);	
	}
	
	public void removePortal(final String name) {
		if (allPortals.containsKey(name)) {
			root = root.remove((Portal)allPortals.get(name), spatialOrigin, spatialWidth, spatialHeight);
		}
	}	

	private void increaseNumRoadsMap(final String name) {
		Integer numRoads = numRoadsForCity.get(name);
		if (numRoads != null) {
			numRoads++;
			numRoadsForCity.put(name, numRoads);
		} else {
			numRoadsForCity.put(name, 1);
		}
	}
	
	private void decreaseNumRoadsMap(final String name) {
		Integer numRoads = numRoadsForCity.get(name);
		numRoads--;
		if (numRoads > 0) {
			numRoadsForCity.put(name, numRoads);
		} else {
			numRoadsForCity.remove(name);
		}
	}
	

	public void clear() {
		root = white;
		allRoads.clear();
		numRoadsForCity.clear();
		allPortals.clear();
		numPortals = 0;
	}

	public boolean isEmpty() {
		return (root == white);
	}

	public boolean containsCity(final String name) {
		final Integer numRoads = numRoadsForCity.get(name);
		return (numRoads != null);
	}
	
	public boolean containsPortalLoc(final Portal portal) {
		Iterator iter = allPortals.values().iterator();
		while (iter.hasNext()) {
			Portal p = (Portal)iter.next();
			if (p.equalsLoc(portal)) {
				return true;
			}
		}
		return false;
	}	
			
	public boolean containsRoad(final Road road) {
		return allRoads.contains(road);
	}

	public int getOrder() {
		return order;
	}
	
	public int getNumCities() {
		return numRoadsForCity.keySet().size();
	}

	public int getNumPortals() {
		return numPortals;
	}
	
	public int getNumRoads() {
		return allRoads.size();
	}
	
	public Map<String, Portal> getAllPortals() {
		return allPortals;
	}

}
