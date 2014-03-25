
import java.awt.Color;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.PriorityQueue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/*
import cmsc420.dijkstra.Dijkstranator;
import cmsc420.dijkstra.Path;
import cmsc420.drawing.CanvasPlus;
import cmsc420.geom.Circle2D;
import cmsc420.geom.Inclusive2DIntersectionVerifier;
import cmsc420.geom.Shape2DDistanceCalculator;
import cmsc420.geometry.City;
import cmsc420.geometry.CityLocationComparator;
import cmsc420.geometry.Geometry;
import cmsc420.geometry.Road;
import cmsc420.geometry.RoadAdjacencyList;
import cmsc420.heptatrie.HeptaTrie;
import cmsc420.heptatrie.StringComparator;
import cmsc420.pmquadtree.IsolatedCityAlreadyExistsThrowable;
import cmsc420.pmquadtree.OutOfBoundsThrowable;

import cmsc420.pmquadtree.PM3Quadtree;
import cmsc420.pmquadtree.PMQuadtree;
import cmsc420.pmquadtree.RoadAlreadyExistsThrowable;
import cmsc420.pmquadtree.PMQuadtree.Black;
import cmsc420.pmquadtree.PMQuadtree.Gray;
import cmsc420.pmquadtree.PMQuadtree.Node;
import cmsc420.xml.XmlUtility;
*/

/**
 * Processes each command in the MeeshQuest program. Takes in an XML command
 * node, processes the node, and outputs the results.
 */
public class Command {
	/** output DOM Document tree */
	protected Document results;

	/** root node of results document */
	protected Element resultsNode;

	/**
	 * stores created cities sorted by their names (used with listCities
	 * command)
	 */
	// protected final HeptaTrie<String, City> citiesByName;
	protected final HeptaTrie<String, City> citiesByName;

	/**
	 * stores created cities sorted by their locations (used with listCities
	 * command)
	 */
	protected final TreeSet<City> citiesByLocation = new TreeSet<City>(new CityLocationComparator());
			
	private final RoadAdjacencyList roads = new RoadAdjacencyList();

	/** stores mapped cities in a spatial data structure */
	protected Map<Integer, PMQuadtree> pmqts = new HashMap<Integer, PMQuadtree>();

	protected Map<String, Integer> portals = new HashMap<String, Integer>();
	
	/** order of the PM Quadtree */
	protected int pmOrder;

	/** spatial width of the PM Quadtree */
	protected int spatialWidth;

	/** spatial height of the PM Quadtree */
	protected int spatialHeight;

		
	public Command(int leafOrder) {
		citiesByName = new HeptaTrie<String, City>(new StringComparator(), leafOrder);
	}

	/**
	 * Set the DOM Document tree to send the results of processed commands to.
	 * Creates the root results node.
	 * 
	 * @param results
	 *            DOM Document tree
	 */
	public void setResults(Document results) {
		this.results = results;
		resultsNode = results.createElement("results");
		results.appendChild(resultsNode);
	}

	/**
	 * Creates a command result element. Initializes the command name.
	 * 
	 * @param node
	 *            the command node to be processed
	 * @return the results node for the command
	 */
	private Element getCommandNode(final Element node) {
		final Element commandNode = results.createElement("command");
		commandNode.setAttribute("name", node.getNodeName());
		commandNode.setAttribute("id", node.getAttribute("id"));
		return commandNode;
	}

	/**
	 * Processes an integer attribute for a command. Appends the parameter to
	 * the parameters node of the results. Should not throw a number format
	 * exception if the attribute has been defined to be an integer in the
	 * schema and the XML has been validated beforehand.
	 * 
	 * @param commandNode
	 *            node containing information about the command
	 * @param attributeName
	 *            integer attribute to be processed
	 * @param parametersNode
	 *            node to append parameter information to
	 * @return integer attribute value
	 */
	private int processIntegerAttribute(final Element commandNode, final String attributeName, final Element parametersNode) {
		final String value = commandNode.getAttribute(attributeName);

		if (parametersNode != null) {
			/* add the parameters to results */
			final Element attributeNode = results.createElement(attributeName);
			attributeNode.setAttribute("value", value);
			parametersNode.appendChild(attributeNode);
		}

		/* return the integer value */
		return Integer.parseInt(value);
	}

	/**
	 * Processes a string attribute for a command. Appends the parameter to the
	 * parameters node of the results.
	 * 
	 * @param commandNode
	 *            node containing information about the command
	 * @param attributeName
	 *            string attribute to be processed
	 * @param parametersNode
	 *            node to append parameter information to
	 * @return string attribute value
	 */
	private String processStringAttribute(final Element commandNode, final String attributeName, final Element parametersNode) {
		final String value = commandNode.getAttribute(attributeName);

		if (parametersNode != null) {
			/* add parameters to results */
			final Element attributeNode = results.createElement(attributeName);
			attributeNode.setAttribute("value", value);
			parametersNode.appendChild(attributeNode);
		}

		/* return the string value */
		return value;
	}

	/**
	 * Reports that the requested command could not be performed because of an
	 * error. Appends information about the error to the results.
	 * 
	 * @param type
	 *            type of error that occurred
	 * @param command
	 *            command node being processed
	 * @param parameters
	 *            parameters of command
	 */
	private void addErrorNode(final String type, final Element command, final Element parameters) {
		final Element error = results.createElement("error");
		error.setAttribute("type", type);
		error.appendChild(command);
		error.appendChild(parameters);
		resultsNode.appendChild(error);
	}

	/**
	 * Reports that a command was successfully performed. Appends the report to
	 * the results.
	 * 
	 * @param command
	 *            command not being processed
	 * @param parameters
	 *            parameters used by the command
	 * @param output
	 *            any details to be reported about the command processed
	 */
	private Element addSuccessNode(final Element command, final Element parameters, final Element output) {
		final Element success = results.createElement("success");
		success.appendChild(command);
		success.appendChild(parameters);
		success.appendChild(output);
		resultsNode.appendChild(success);
		return success;
	}

	/**
	 * Processes the commands node (root of all commands). Gets the spatial
	 * width and height of the map and send the data to the appropriate data
	 * structures.
	 * 
	 * @param node
	 *            commands node to be processed
	 */
	public void processCommands(final Element node) {
		spatialWidth = Integer.parseInt(node.getAttribute("spatialWidth"));
		spatialHeight = Integer.parseInt(node.getAttribute("spatialHeight"));
		pmOrder = 3; //Integer.parseInt(node.getAttribute("pmOrder"));
	}

	/**
	 * Processes a createCity command. Creates a city in the dictionary (Note:
	 * does not map the city). An error occurs if a city with that name or
	 * location is already in the dictionary.
	 * 
	 * @param node
	 *            createCity node to be processed
	 */
	public void processCreateCity(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String name = processStringAttribute(node, "name", parametersNode);
		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);
		final int z = processIntegerAttribute(node, "z", parametersNode);
		final int radius = processIntegerAttribute(node, "radius", parametersNode);
		final String color = processStringAttribute(node, "color", parametersNode);

		/* create the city */
		final City city = new City(name, x, y, z, radius, color);

		if (citiesByName.containsKey(name)) {
			addErrorNode("duplicateCityName", commandNode, parametersNode);
		} else if (citiesByLocation.contains(city)) {
			addErrorNode("duplicateCityCoordinates", commandNode, parametersNode);
		} else {
			final Element outputNode = results.createElement("output");

			/* add city to dictionary */
			citiesByName.put(name, city);
			citiesByLocation.add(city);

			/* add success node to results */
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}

	public void processDeleteCity(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String name = processStringAttribute(node, "name", parametersNode);

		if (!citiesByName.containsKey(name)) {
			addErrorNode("cityDoesNotExist", commandNode, parametersNode);
		} else {
			/* delete city */
			final Element outputNode = results.createElement("output");
			final City deletedCity = (City)citiesByName.get(name);

			citiesByName.remove(name);			
			citiesByLocation.remove(deletedCity);
			TreeSet<Road> roadsForDeletedCity = roads.deleteCity(deletedCity);
			
			if (roadsForDeletedCity != null) {
				addCityNode(outputNode, "cityUnmapped", deletedCity);
				for (Road road : roadsForDeletedCity) {
					addRoadNode(outputNode, "roadUnmapped", road);
				}
			}

			/* add success node to results */
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}


	/**
	 * Clears all the data structures do there are not cities or roads in
	 * existence in the dictionary or on the map.
	 * 
	 * @param node
	 *            clearAll node to be processed
	 */
	public void processClearAll(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		/* clear data structures */
		citiesByName.clear();
		citiesByLocation.clear();
		pmqts.clear();
		roads.clear();
		portals.clear();

		/* add success node to results */
		addSuccessNode(commandNode, parametersNode, outputNode);
	}

	/**
	 * Lists all the cities, either by name or by location.
	 * 
	 * @param node
	 *            listCities node to be processed
	 */
	public void processListCities(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final String sortBy = processStringAttribute(node, "sortBy", parametersNode);

		if (citiesByName.isEmpty()) {
			addErrorNode("noCitiesToList", commandNode, parametersNode);
		} else {
			final Element outputNode = results.createElement("output");
			final Element cityListNode = results.createElement("cityList");

			Collection<City> cityCollection = null;
			if (sortBy.equals("name")) {
				List<City> cities = new ArrayList<City>(citiesByLocation.size());
				for (City c : citiesByLocation) {
					cities.add(c);
				}
				Collections.sort(cities, new Comparator<City>() {

					// @Override
					public int compare(City arg0, City arg1) {
						return arg0.getName().compareTo(arg1.getName());
					}
				});
				cityCollection = cities;
			} else if (sortBy.equals("coordinate")) {
				cityCollection = citiesByLocation;
			} else {
				/* XML validator failed */
				System.exit(-1);
			}

			for (City c : cityCollection) {
				addCityNode(cityListNode, "city", c);
			}
			outputNode.appendChild(cityListNode);

			/* add success node to results */
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}

	private void addCityNode(final Element node, final String cityNodeName, final City city) {
		final Element cityNode = results.createElement(cityNodeName);		
		cityNode.setAttribute("name", city.getName());
		cityNode.setAttribute("x", Integer.toString(city.getX()));
		cityNode.setAttribute("y", Integer.toString(city.getY()));
		cityNode.setAttribute("z", Integer.toString(city.getZ()));
		cityNode.setAttribute("radius", Integer.toString(city.getRadius()));
		cityNode.setAttribute("color", city.getColor());
		node.appendChild(cityNode);	
	}
	
	private void addRoadNode(final Element node, final String roadNodeName, final Road road) {
		final Element roadNode = results.createElement(roadNodeName);
		roadNode.setAttribute("start", road.getStart().getName());
		roadNode.setAttribute("end", road.getEnd().getName());
		node.appendChild(roadNode);
	}

	private void addRoadNode(final Element node, final String roadNodeName, final String start, final String end) {
		final Element roadNode = results.createElement(roadNodeName);
		roadNode.setAttribute("start", start);
		roadNode.setAttribute("end", end);
		node.appendChild(roadNode);
	}
	
	private void addPortalNode(final Element node, final String portalNodeName, final Portal portal) {
		final Element portalNode = results.createElement(portalNodeName);		
		portalNode.setAttribute("name", portal.getName());
		portalNode.setAttribute("x", Integer.toString(portal.getX()));
		portalNode.setAttribute("y", Integer.toString(portal.getY()));
		portalNode.setAttribute("z", Integer.toString(portal.getZ()));
		node.appendChild(portalNode);	
	}	


	public void processPrintHeptaTrie(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		if (citiesByName.isEmpty()) {
			addErrorNode("emptyHeptaTrie", commandNode, parametersNode);
		} else {
			citiesByName.addToXmlDoc(results, outputNode);

			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}

	public void processMapRoad(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String start = processStringAttribute(node, "start", parametersNode);
		final String end = processStringAttribute(node, "end", parametersNode);

		final Element outputNode = results.createElement("output");

		if (!citiesByName.containsKey(start)) {
			addErrorNode("startPointDoesNotExist", commandNode, parametersNode);
		} else if (!citiesByName.containsKey(end)) {
			addErrorNode("endPointDoesNotExist", commandNode, parametersNode);
		} else if (start.equals(end)) {
			addErrorNode("startEqualsEnd", commandNode, parametersNode);
		} else if (((City)citiesByName.get(start)).getZ() != ((City)citiesByName.get(end)).getZ()) {
			addErrorNode("roadNotOnOneLevel", commandNode, parametersNode);
		} else {
			try {
				// add to spatial structure
				City city1 = (City) citiesByName.get(start);
				City city2 = (City) citiesByName.get(end);
				Rectangle2D.Float world = new Rectangle2D.Float(0, 0, spatialWidth, spatialHeight);		

				Integer level = new Integer(city1.getZ());
				PMQuadtree pmQuadtree = pmqts.get(level);
				if (pmQuadtree == null) {
					if (pmOrder == 3) {
						pmQuadtree = new PM3Quadtree(spatialWidth, spatialHeight);
					}
					pmqts.put(level, pmQuadtree);
				}
				pmQuadtree.addRoad(new Road(city1, city2));
				if (Inclusive2DIntersectionVerifier.intersects(city1.toPoint2D(), world) && 
				    Inclusive2DIntersectionVerifier.intersects(city2.toPoint2D(), world)) {
					// add to adjacency list
					roads.addRoad(city1, city2);
				}
				// create roadCreated element
				addRoadNode(outputNode, "roadCreated", start, end); 
				// add success node to results
				addSuccessNode(commandNode, parametersNode, outputNode);
			} catch (RoadAlreadyMappedThrowable e) {
				addErrorNode("roadAlreadyMapped", commandNode, parametersNode);
			} catch (OutOfBoundsThrowable e) {
				addErrorNode("roadOutOfBounds", commandNode, parametersNode);
			} catch (RoadIntersectsAnotherRoadThrowable e) {
				addErrorNode("roadIntersectsAnotherRoad", commandNode, parametersNode);
			} catch (RoadViolatesPMRulesThrowable e) {
				addErrorNode("roadViolatesPMRules", commandNode, parametersNode);
			}
		}
	}

	public void processMapPortal(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String name = processStringAttribute(node, "name", parametersNode);
		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);
		final int z = processIntegerAttribute(node, "z", parametersNode);
		final Element outputNode = results.createElement("output");

		if (portals.containsKey(name)) {
			addErrorNode("duplicatePortalName", commandNode, parametersNode);
		} else {
			Integer level = new Integer(z);
			PMQuadtree pmQuadtree = pmqts.get(level);
			if (pmQuadtree == null) {
				if (pmOrder == 3) {
					pmQuadtree = new PM3Quadtree(spatialWidth, spatialHeight);
				}
				pmqts.put(level, pmQuadtree);
			}
			Portal portal = new Portal(name, x, y, z);
			if (pmQuadtree.containsPortalLoc(portal)) {
				addErrorNode("duplicatePortalCoordinates", commandNode, parametersNode);
			} else {
				try {
					// add to spatial structure
					pmQuadtree.addPortal(portal);
					portals.put(name, level);
					/* add success node to results */
					addSuccessNode(commandNode, parametersNode, outputNode);
				} catch (OutOfBoundsThrowable e) {
					addErrorNode("portalOutOfBounds", commandNode, parametersNode);
				} catch (PortalViolatesPMRulesThrowable e) {
					addErrorNode("portalViolatesPMRules", commandNode, parametersNode);
				}
			}			
		}
	}

	public void processUnmapRoad(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String start = processStringAttribute(node, "start",parametersNode);
		final String end = processStringAttribute(node, "end", parametersNode);

		final Element outputNode = results.createElement("output");

		if (!citiesByName.containsKey(start)) {
			addErrorNode("startPointDoesNotExist", commandNode, parametersNode);
		} else if (!citiesByName.containsKey(end)) {
			addErrorNode("endPointDoesNotExist", commandNode, parametersNode);
		} else if (start.equals(end)) {
			addErrorNode("startEqualsEnd", commandNode, parametersNode);
		} else {
			// remove spatial structure			
			try {
				City city1 = (City)citiesByName.get(start);
				City city2 = (City)citiesByName.get(end);
				Road road = new Road(city1, city2);
				Integer level = new Integer(city1.getZ());
				PMQuadtree pmQuadtree = pmqts.get(level);
				if (pmQuadtree == null) {
					throw new RoadNotMappedThrowable();
				}
				pmQuadtree.removeRoad(road);
				roads.removeRoad(road);
				addRoadNode(outputNode, "roadDeleted", road);
				// add success node to results
				addSuccessNode(commandNode, parametersNode, outputNode);
			} catch (RoadNotMappedThrowable e) {
				addErrorNode("roadNotMapped", commandNode, parametersNode);
			}
		}
	}
	
	public void processUnmapPortal(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String name = processStringAttribute(node, "name",parametersNode);

		final Element outputNode = results.createElement("output");
						
		if (!portals.containsKey(name)) {
			addErrorNode("portalDoesNotExist", commandNode, parametersNode);
		} else {
			// remove spatial structure			
			Integer level = portals.get(name);
			PMQuadtree pmQuadtree = pmqts.get(level);
			if (pmQuadtree == null) {
				addErrorNode("portalDoesNotExist", commandNode, parametersNode);
			} else {			
				pmQuadtree.removePortal(name);
				// add success node to results
				addSuccessNode(commandNode, parametersNode, outputNode);
			}
		}
	}	
	
	/**
	 * Prints out the structure of the PM Quadtree in an XML format.
	 * 
	 * @param node
	 *            printPMQuadtree command to be processed
	 */

	public void processPrintPMQuadtree(final Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		final int z = processIntegerAttribute(node, "z", parametersNode);

		Integer level = new Integer(z);
		PMQuadtree pmQuadtree = pmqts.get(level);		
		if (pmQuadtree == null || pmQuadtree.isEmpty()) {
			/* empty PR Quadtree */
			addErrorNode("mapIsEmpty", commandNode, parametersNode);
		} else {
			/* print PR Quadtree */
			final Element quadtreeNode = results.createElement("quadtree");
			quadtreeNode.setAttribute("order", Integer.toString(pmOrder));
			printPMQuadtreeHelper(pmQuadtree.getRoot(), quadtreeNode);

			outputNode.appendChild(quadtreeNode);

			/* add success node to results */
			addSuccessNode(commandNode, parametersNode, outputNode);
		}
	}

	/**
	 * Traverses each node of the PR Quadtree.
	 * 
	 * @param currentNode
	 *            PR Quadtree node being printed
	 * @param xmlNode
	 *            XML node representing the current PR Quadtree node
	 */

	private void printPMQuadtreeHelper(final PMQuadtree.Node currentNode,
			final Element xmlNode) {
		if (currentNode.getType() == PMQuadtree.Node.WHITE) {
			Element white = results.createElement("white");
			xmlNode.appendChild(white);
		} else if (currentNode.getType() == PMQuadtree.Node.BLACK) {
			PMQuadtree.Black currentLeaf = (PMQuadtree.Black) currentNode;
			Element blackNode = results.createElement("black");
			blackNode.setAttribute("cardinality", Integer.toString(currentLeaf.getGeometry().size()));
			for (Geometry g : currentLeaf.getGeometry()) {
				if (g instanceof City) {
					addCityNode(blackNode, "city", (City) g);						
				} else if (g instanceof Portal) {
					addPortalNode(blackNode, "portal", (Portal) g);	
				} else if (g instanceof Road) {
					addRoadNode(blackNode, "road", (Road) g);
				}
			}
			xmlNode.appendChild(blackNode);
		} else {
			final PMQuadtree.Gray currentInternal = (PMQuadtree.Gray) currentNode;
			final Element gray = results.createElement("gray");
			gray.setAttribute("x", Integer.toString((int) currentInternal.getCenterX()));
			gray.setAttribute("y", Integer.toString((int) currentInternal.getCenterY()));
			for (int i = 0; i < 4; i++) {
				printPMQuadtreeHelper(currentInternal.getChild(i), gray);
			}
			xmlNode.appendChild(gray);
		}
	}
	
	/**
	 * Processes a saveMap command. Saves the graphical map to a given file.
	 * 
	 * @param node
	 *            saveMap command to be processed
	 * @throws IOException
	 *             problem accessing the image file
	 */
	public void processSaveMap(final Element node) throws IOException {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final int z = processIntegerAttribute(node, "z", parametersNode);
		final String name = processStringAttribute(node, "name", parametersNode);

		final Element outputNode = results.createElement("output");

		CanvasPlus canvas = drawPMQuadtree(z);

		/* save canvas to '(name).png' */
		canvas.save(name);

		canvas.dispose();

		/* add success node to results */
		addSuccessNode(commandNode, parametersNode, outputNode);
	}		
	
	/**
	 * Finds the mapped cities within the range of a given point.
	 * 
	 * @param node
	 *            rangeCities command to be processed
	 * @throws IOException
	 */
	public void processRangeCities(final Element node) throws IOException {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);
		final int z = processIntegerAttribute(node, "z", parametersNode);
		final int radius = processIntegerAttribute(node, "radius", parametersNode);

		String pathFile = "";
		if (!node.getAttribute("saveMap").equals("")) {
			pathFile = processStringAttribute(node, "saveMap", parametersNode);
		}

		if (radius == 0) {
			addErrorNode("noCitiesExistInRange", commandNode, parametersNode);
		} else {
			final TreeSet<Geometry> cityCandidates = new TreeSet<Geometry>();
			Set<Integer> allLevels = pmqts.keySet();
			for (Integer level : allLevels) {
				if (level.intValue() >= z-radius && level.intValue() <= z+radius) {
					PMQuadtree pmQuadtree = pmqts.get(level);
					if (pmQuadtree != null && !pmQuadtree.isEmpty()) {
						rangeHelper(new Circle2D.Double(x, y, radius), pmQuadtree.getRoot(), cityCandidates, false, true);
					}
				}
			}
			
			if (cityCandidates.isEmpty()) {
				addErrorNode("noCitiesExistInRange", commandNode, parametersNode);
			} else {
				/* get city list */
				final Element cityListNode = results.createElement("cityList");
				boolean hasAtLeastOne = false;
				for (Geometry g : cityCandidates) {
					City city = (City) g;
					double deltaX = (double)city.getX() - (double)x;
					double deltaY = (double)city.getY() - (double)y;
					double deltaZ = (double)city.getZ() - (double)z;
					double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
					if (dist <= radius) {
						addCityNode(cityListNode, "city", city);
						hasAtLeastOne = true;
					}										

				}
				if (!hasAtLeastOne) {
					addErrorNode("noCitiesExistInRange", commandNode, parametersNode);				
				} else {
					outputNode.appendChild(cityListNode);
					/* add success node to results */
					addSuccessNode(commandNode, parametersNode, outputNode);
					if (pathFile.compareTo("") != 0) {
						/* save canvas to file with range circle */
						CanvasPlus canvas = drawPMQuadtree(z);
						canvas.addCircle(x, y, radius, Color.BLUE, false);
						canvas.save(pathFile);
						canvas.dispose();
					}
				}
			}
		}
	}

	/**
	 * find all roads intersecting a circle (range)
	 * 
	 * @param node
	 * @throws IOException
	 */
	public void processRangeRoads(final Element node) throws IOException {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);
		final int z = processIntegerAttribute(node, "z", parametersNode);
		final int radius = processIntegerAttribute(node, "radius", parametersNode);

		String pathFile = "";
		if (!node.getAttribute("saveMap").equals("")) {
			pathFile = processStringAttribute(node, "saveMap", parametersNode);
		}

		if (radius == 0) {
			addErrorNode("noRoadsExistInRange", commandNode, parametersNode);
		} else {
			final TreeSet<Geometry> roadCandidates = new TreeSet<Geometry>();		
			Set<Integer> allLevels = pmqts.keySet();
			for (Integer level : allLevels) {
				if (level.intValue() >= z-radius && level.intValue() <= z+radius) {
					PMQuadtree pmQuadtree = pmqts.get(level);
					if (pmQuadtree != null && !pmQuadtree.isEmpty()) {
						rangeHelper(new Circle2D.Double(x, y, radius), pmQuadtree.getRoot(), roadCandidates, true, false);
					}
				}
			}
			
			if (roadCandidates.isEmpty()) {
				addErrorNode("noRoadsExistInRange", commandNode, parametersNode);
			} else {
				/* get road list */
				final Element roadListNode = results.createElement("roadList");
				boolean hasAtLeastOne = false;
				for (Geometry g : roadCandidates) {
					Road road = (Road) g;					
					double distOnXYPlane = road.toLine2D().ptSegDist(new Point2D.Double(x, y));
					double deltaZ = (double)road.getStart().getZ() - (double)z;
					double dist = Math.sqrt(distOnXYPlane * distOnXYPlane + deltaZ * deltaZ);
					if (dist <= radius) {
						addRoadNode(roadListNode, "road", road);
						hasAtLeastOne = true;
					}										

				}
				if (!hasAtLeastOne) {
					addErrorNode("noRoadsExistInRange", commandNode, parametersNode);				
				} else {
					outputNode.appendChild(roadListNode);
					/* add success node to results */
					addSuccessNode(commandNode, parametersNode, outputNode);
					if (pathFile.compareTo("") != 0) {
						/* save canvas to file with range circle */
						CanvasPlus canvas = drawPMQuadtree(z);
						canvas.addCircle(x, y, radius, Color.BLUE, false);
						canvas.save(pathFile);
						canvas.dispose();
					}
				}
			}
		}
	}

	/**
	 * Helper function for both rangeCities and rangeRoads
	 * 
	 * @param range
	 *            defines the range as a circle
	 * @param node
	 *            is the node in the pmQuadtree being processed
	 * @param gInRange
	 *            stores the results
	 * @param includeRoads
	 *            specifies if the range search should include roads
	 * @param includeCities
	 *            specifies if the range search should include cities
	 */
	private void rangeHelper(final Circle2D.Double range, final PMQuadtree.Node node, final TreeSet<Geometry> gInRange, final boolean includeRoads, final boolean includeCities) {
		if (node.getType() == PMQuadtree.Node.BLACK) {
			final PMQuadtree.Black leaf = (PMQuadtree.Black) node;
			for (Geometry g : leaf.getGeometry()) {
				if (includeCities &&
					g instanceof City &&
					!gInRange.contains(g) &&
					Inclusive2DIntersectionVerifier.intersects(((City) g).toPoint2D(), range)) {
					gInRange.add(g);
				}
				if (includeRoads &&
					g instanceof Road &&
					!gInRange.contains(g) &&
					(((Road) g).toLine2D().ptSegDist(range.getCenter()) <= range.getRadius())) {
					gInRange.add(g);
				}
			}
		} else if (node.getType() == PMQuadtree.Node.GRAY) {
			final PMQuadtree.Gray internal = (PMQuadtree.Gray) node;
			for (int i = 0; i < 4; i++) {
				if (Inclusive2DIntersectionVerifier.intersects(internal.getChildRegion(i), range)) {
					rangeHelper(range, internal.getChild(i), gInRange, includeRoads, includeCities);
				}
			}
		}
	}	

	// // nearest city ////
	public void processNearestCity(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		/* extract attribute values from command */
		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);
		final int z = processIntegerAttribute(node, "z", parametersNode);
		
		Integer level = new Integer(z);
		PMQuadtree pmQuadtree = pmqts.get(level);		
		if (pmQuadtree == null || pmQuadtree.isEmpty()) {
			addErrorNode("cityNotFound", commandNode, parametersNode);
		} else {
			if (pmQuadtree.getNumCities() - pmQuadtree.getNumPortals() == 0) {
				addErrorNode("cityNotFound", commandNode, parametersNode);
			} else {
				addCityNode(outputNode, "city", nearestCityHelper(pmQuadtree, x, y));
				addSuccessNode(commandNode, parametersNode, outputNode);
			}
		}
	}

	private City nearestCityHelper(PMQuadtree pmQuadtree, int x, int y) {
		final Point2D.Float point = new Point2D.Float(x, y);
		PMQuadtree.Node n = pmQuadtree.getRoot();
		PriorityQueue<NearestSearchRegion> nearCities = new PriorityQueue<NearestSearchRegion>();

		if (n.getType() == PMQuadtree.Node.BLACK) {
			PMQuadtree.Black b = (PMQuadtree.Black) n;
			return b.getCity();
		}

		while (n.getType() == PMQuadtree.Node.GRAY) {
			PMQuadtree.Gray g = (PMQuadtree.Gray) n;
			PMQuadtree.Node kid;
			for (int i = 0; i < 4; i++) {
				kid = g.getChild(i);
				if (kid.getType() == PMQuadtree.Node.BLACK) {
					PMQuadtree.Black b = (PMQuadtree.Black) kid;
					City c = b.getCity();
					if (c != null) {
						double dist = point.distance(c.toPoint2D());
						nearCities.add(new NearestSearchRegion(kid, dist, c));
					}
				} else if (kid.getType() == PMQuadtree.Node.GRAY) {
					double dist = Shape2DDistanceCalculator.distance(point, g.getChildRegion(i));
					nearCities.add(new NearestSearchRegion(kid, dist, null));
				}
			}
			try {
				n = nearCities.remove().node;
			} catch (Exception ex) {
				System.err.println(nearCities.size());
				throw new IllegalStateException();
			}
		}
		return ((PMQuadtree.Black) n).getCity();
	}

	// // nearest portal ////
	public void processNearestPortal(Element node) {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");
		final Element outputNode = results.createElement("output");

		/* extract attribute values from command */
		final int x = processIntegerAttribute(node, "x", parametersNode);
		final int y = processIntegerAttribute(node, "y", parametersNode);
		final int z = processIntegerAttribute(node, "z", parametersNode);

		Integer level = new Integer(z);
		PMQuadtree pmQuadtree = pmqts.get(level);		
		if (pmQuadtree == null || pmQuadtree.isEmpty()) {
			addErrorNode("noPortalFound", commandNode, parametersNode);
		} else {
			if (pmQuadtree.getNumCities() - pmQuadtree.getNumPortals() == 0) {
				addErrorNode("noPortalFound", commandNode, parametersNode);
			} else {
				addPortalNode(outputNode, "portal", nearestPortalHelper(pmQuadtree, x, y));
				addSuccessNode(commandNode, parametersNode, outputNode);
			}
		}
	}

	private Portal nearestPortalHelper(PMQuadtree pmQuadtree, int x, int y) {
		final Point2D.Float point = new Point2D.Float(x, y);
		PMQuadtree.Node n = pmQuadtree.getRoot();
		PriorityQueue<NearestSearchRegion> nearPortals = new PriorityQueue<NearestSearchRegion>();

		if (n.getType() == PMQuadtree.Node.BLACK) {
			PMQuadtree.Black b = (PMQuadtree.Black) n;
			return b.getPortal();
		}

		while (n.getType() == PMQuadtree.Node.GRAY) {
			PMQuadtree.Gray g = (PMQuadtree.Gray) n;
			PMQuadtree.Node kid;
			for (int i = 0; i < 4; i++) {
				kid = g.getChild(i);
				if (kid.getType() == PMQuadtree.Node.BLACK) {
					PMQuadtree.Black b = (PMQuadtree.Black) kid;
					Portal p = b.getPortal();
					if (p != null) {
						double dist = point.distance(p.toPoint2D());
						nearPortals.add(new NearestSearchRegion(kid, dist, p));
					}
				} else if (kid.getType() == PMQuadtree.Node.GRAY) {
					double dist = Shape2DDistanceCalculator.distance(point, g.getChildRegion(i));
					nearPortals.add(new NearestSearchRegion(kid, dist, null));
				}
			}
			try {
				n = nearPortals.remove().node;
			} catch (Exception ex) {
				System.err.println(nearPortals.size());
				throw new IllegalStateException();
			}
		}
		return ((PMQuadtree.Black) n).getPortal();
	}


	private Road nearestRoadHelper(PMQuadtree pmQuadtree, int x, int y) {
		final Point2D.Float point = new Point2D.Float(x, y);	
		PMQuadtree.Node n = pmQuadtree.getRoot();
		PriorityQueue<NearestSearchRegion> nearRoads = new PriorityQueue<NearestSearchRegion>();
		NearestSearchRegion region = null;

		while (n.getType() == PMQuadtree.Node.GRAY) {
			PMQuadtree.Gray g = (PMQuadtree.Gray) n;
			PMQuadtree.Node kid;
			for (int i = 0; i < 4; i++) {
				kid = g.getChild(i);
				if (kid.getType() == PMQuadtree.Node.BLACK) {
					PMQuadtree.Black b = (PMQuadtree.Black) kid;
					List<Geometry> gList = b.getGeometry();
					double minDist = Double.MAX_VALUE;
					Road road = null;
					for (Geometry geom : gList) {
						if (geom.isRoad()) {
							double d = ((Road) geom).toLine2D().ptSegDist(point);
							if (d < minDist) {
								minDist = d;
								road = (Road) geom;
							}
						}
					}
					if (road == null) {
						continue;
					}
					nearRoads.add(new NearestSearchRegion(kid, minDist, road));
				} else if (kid.getType() == PMQuadtree.Node.GRAY) {
					double dist = Shape2DDistanceCalculator.distance(point,g.getChildRegion(i));
					nearRoads.add(new NearestSearchRegion(kid, dist, null));
				}
			}
			try {
				region = nearRoads.remove();
				n = region.node;
			} catch (Exception ex) {
				// should be impossible to reach here
				throw new IllegalStateException();
			}
		}
		assert region.node.getType() == PMQuadtree.Node.BLACK;
		return (Road) region.g;
	}





	/**
	 * Helper class for nearest everything (city/road/etc)
	 */
	private class NearestSearchRegion implements Comparable<NearestSearchRegion> {
		private PMQuadtree.Node node;
		private double distance;
		private Geometry g;

		public NearestSearchRegion(PMQuadtree.Node node, double distance, Geometry g) {
			this.node = node;
			this.distance = distance;
			this.g = g;
		}

		public int compareTo(NearestSearchRegion o) {
			if (distance == o.distance) {
				if (node.getType() == PMQuadtree.Node.BLACK && 
				    o.node.getType() == PMQuadtree.Node.BLACK) {
					return g.compareTo(o.g);
				} else if (node.getType() == PMQuadtree.Node.BLACK && 
				           o.node.getType() == PMQuadtree.Node.GRAY) {
					return 1;
				} else if (node.getType() == PMQuadtree.Node.GRAY && 
				           o.node.getType() == PMQuadtree.Node.BLACK) {
					return -1;
				} else {
					return ((PMQuadtree.Gray) node).hashCode() - ((PMQuadtree.Gray) o.node).hashCode();
				}
			}
			return (distance < o.distance) ? -1 : 1;
		}
	}
	
	public void processShortestPath(final Element node) throws IOException, ParserConfigurationException, TransformerException {
		final Element commandNode = getCommandNode(node);
		final Element parametersNode = results.createElement("parameters");

		final String start = processStringAttribute(node, "start", parametersNode);
		final String end = processStringAttribute(node, "end", parametersNode);

		String saveMapName = "";
		if (!node.getAttribute("saveMap").equals("")) {
			saveMapName = processStringAttribute(node, "saveMap", parametersNode);
		}

		String saveHTMLName = "";
		if (!node.getAttribute("saveHTML").equals("")) {
			saveHTMLName = processStringAttribute(node, "saveHTML", parametersNode);
		}
		
		final City startCity = (City) citiesByName.get(start);
		final City endCity = (City) citiesByName.get(end);
		if (startCity == null) {
			addErrorNode("nonExistentStart", commandNode, parametersNode);
		} else if (endCity == null) {
			addErrorNode("nonExistentEnd", commandNode, parametersNode);
		} else {
			int zStart = startCity.getZ();
			int zEnd = endCity.getZ();
			PMQuadtree pmQuadtreeAtStartLevel = pmqts.get(new Integer(zStart));		
			PMQuadtree pmQuadtreeAtEndLevel = pmqts.get(new Integer(zEnd));
			Map<String, Portal> portalMap = new HashMap<String, Portal>();
			
			if (pmQuadtreeAtStartLevel == null || pmQuadtreeAtStartLevel.isEmpty() || !pmQuadtreeAtStartLevel.containsCity(start)) {		
				addErrorNode("nonExistentStart", commandNode, parametersNode);
			} else if (pmQuadtreeAtEndLevel == null || pmQuadtreeAtEndLevel.isEmpty() || !pmQuadtreeAtEndLevel.containsCity(end)) {		
				addErrorNode("nonExistentEnd", commandNode, parametersNode);
			} else {
				if (zStart != zEnd) {
					int bottom = Math.min(zStart, zEnd);
					int top = Math.max(zStart, zEnd);

					Map<Integer, List<String>> portalsForLevel = new HashMap<Integer, List<String>>(); 

					for (int z = bottom-1; z <= top+1; z++) {
						List<String> portalNameList = new ArrayList<String>();
						Integer level = new Integer(z);
						PMQuadtree pmQuadtree= pmqts.get(level);
						if (pmQuadtree != null && !pmQuadtree.isEmpty() && pmQuadtree.getAllPortals() != null) {
							portalNameList.addAll(pmQuadtree.getAllPortals().keySet());
						}						
						portalsForLevel.put(level, portalNameList);
						if (z >= bottom && z <= top) {
							portalMap.putAll(pmQuadtree.getAllPortals());
						}
					}
										
					for (int z = bottom; z <= top; z++) {
						Integer level = new Integer(z);
						portalsForLevel.get(level).addAll(portalsForLevel.get(level-1));
						portalsForLevel.get(level).addAll(portalsForLevel.get(level+1));
						
						PMQuadtree pmQuadtree = pmqts.get(level);
						if (pmQuadtree != null && !pmQuadtree.isEmpty()) {
							List<String> portalNames = portalsForLevel.get(level);
							if (portalNames != null && !portalNames.isEmpty()) {
								for (String portalName : portalNames) {
									Portal portal = (Portal)portalMap.get(portalName);
									final Point2D.Float portalLoc = new Point2D.Float(portal.getX(), portal.getY());	
									Road nearestRoad = nearestRoadHelper(pmQuadtree, portal.getX(), portal.getY());
									double distance = nearestRoad.toLine2D().ptSegDist(portalLoc);
									portal.setNearestRoad(z, nearestRoad);
									Road betweenPortalAndStart = new Road(portal, nearestRoad.getStart());
									betweenPortalAndStart.setDistance(distance);									
									Road betweenPortalAndEnd = new Road(portal, nearestRoad.getEnd());
									betweenPortalAndEnd.setDistance(distance);				
									roads.addRoad(betweenPortalAndStart);
									roads.addRoad(betweenPortalAndEnd);
								}				
							}
						}												
					}
				}

				final DecimalFormat decimalFormat = new DecimalFormat("#0.000");
				final Dijkstranator dijkstranator = new Dijkstranator(roads);
				final Path path = dijkstranator.getShortestPath(startCity, endCity);
				
				// clean up  fake roads
				if (zStart != zEnd) {
					Collection<Portal> portals = portalMap.values();
					for (Portal portal : portals) {
						roads.removeRoadsForCity(portal);
					}
				}

				if (path == null) {
					addErrorNode("noPathExists", commandNode, parametersNode);
				} else {
					final Element outputNode = results.createElement("output");
					final Element pathNode = results.createElement("path");
					pathNode.setAttribute("length", decimalFormat.format(path.getDistance()));
					pathNode.setAttribute("hops", Integer.toString(path.getHops()));
					final LinkedList<City> cityList = path.getCityList();
					/* if required, save the map to an image */
					if (!saveMapName.equals("")) {
						saveShortestPathMap(saveMapName, cityList);
					}
					if (!saveHTMLName.equals("")) {
						saveShortestPathMap(saveHTMLName, cityList);
					}
					if (cityList.size() > 1) {
						/* add the first road */
						City city1 = cityList.remove();
						City city2 = cityList.remove();
						Element roadNode = results.createElement("road");
						Element portalNode = results.createElement("portal");
						String startName = city1.getName();
						String endName = city2.getName();
						if (city1 instanceof Portal) { // start is portal, end is city
							int z = city2.getZ();
							Road nearestRoad = ((Portal)city1).getNearestRoad(z);
							startName = nearestRoad.getEnd().getName().equals(endName) ? nearestRoad.getStart().getName() : nearestRoad.getEnd().getName();
							portalNode.setAttribute("startZ", Integer.toString(z));
						} else if (city2 instanceof Portal) { // end is portal, start is city
							int z = city1.getZ();
							String portalName = endName;
							Road nearestRoad = ((Portal)city2).getNearestRoad(z);
							endName = nearestRoad.getStart().getName().equals(startName) ? nearestRoad.getEnd().getName() : nearestRoad.getStart().getName();
							portalNode.setAttribute("startZ", Integer.toString(z));
						}														
						roadNode.setAttribute("start", startName);
						roadNode.setAttribute("end", endName);
						pathNode.appendChild(roadNode);
	
						while (!cityList.isEmpty()) {
							City city3 = cityList.remove();
								if (city2 instanceof Portal) {
								int z = city3.getZ();
								portalNode.setAttribute("endZ", Integer.toString(z));
								pathNode.appendChild(portalNode);	
							} else if (city3 instanceof Portal) {
								int z = city2.getZ();
								portalNode.setAttribute("endZ", Integer.toString(z));
								pathNode.appendChild(portalNode);								
							} else {
								/* process the angle */
								Arc2D.Float arc = new Arc2D.Float();
								arc.setArcByTangent(city1.toPoint2D(), city2.toPoint2D(), city3.toPoint2D(), 1);

								/* print out the direction */
								double angle = arc.getAngleExtent();
								final String direction;
								while (angle < 0) {
									angle += 360;
								}
								while (angle > 360) {
									angle -= 360;
								}
								if (angle > 180 && angle <= 180 + 135) {
									direction = "left";
								} else if (angle > 45 && angle <= 180 ) {
									direction = "right";
								} else {
									direction = "straight";
								}
								Element directionNode = results.createElement(direction);
								pathNode.appendChild(directionNode);
							}

							/* print out the next road */
							roadNode = results.createElement("road");
							portalNode = results.createElement("portal");								
							startName = city2.getName();
							endName = city3.getName();
							if (city2 instanceof Portal) {
								int z = city3.getZ();
								Road nearestRoad = ((Portal)city2).getNearestRoad(z);
								startName = nearestRoad.getEnd().getName().equals(endName) ? nearestRoad.getStart().getName() : nearestRoad.getEnd().getName();
								portalNode.setAttribute("startZ", Integer.toString(z));
							} if (city3 instanceof Portal) {
								int z = city2.getZ();
								String portalName = endName;
								Road nearestRoad = ((Portal)city3).getNearestRoad(z);
								endName = nearestRoad.getStart().getName().equals(startName) ? nearestRoad.getEnd().getName() : nearestRoad.getStart().getName();
								portalNode.setAttribute("startZ", Integer.toString(z));
							}
							roadNode.setAttribute("start", startName);
							roadNode.setAttribute("end", endName);
							pathNode.appendChild(roadNode);
							/* increment city references */
							city1 = city2;
							city2 = city3;
						}
					}
					outputNode.appendChild(pathNode);
					Element successNode = addSuccessNode(commandNode, parametersNode, outputNode);
					if (!saveHTMLName.equals("")) {
						/* save shortest path to HTML */
						Document shortestPathDoc = XmlUtility.getDocumentBuilder().newDocument();
						org.w3c.dom.Node spNode = shortestPathDoc.importNode(successNode, true);
						shortestPathDoc.appendChild(spNode);
						XmlUtility.transform(shortestPathDoc, new File("shortestPath.xsl"), new File(saveHTMLName + ".html"));
					}
				}	
			}							
		}		
	}

	private void saveShortestPathMap(final String mapName,
			final List<City> cityList) throws IOException {
		final CanvasPlus map = new CanvasPlus();
		/* initialize map */
		map.setFrameSize(spatialWidth, spatialHeight);
		/* add a rectangle to show where the bounds of the map are located */
		map.addRectangle(0, 0, spatialWidth, spatialHeight, Color.BLACK, false);

		final Iterator<City> it = cityList.iterator();
		City city1 = it.next();

		/* map green starting point */
		map.addPoint(city1.getName(), city1.getX(), city1.getY(), Color.GREEN);

		if (it.hasNext()) {
			City city2 = it.next();
			/* map blue road */
			map.addLine(city1.getX(), city1.getY(), city2.getX(), city2.getY(), Color.BLUE);

			while (it.hasNext()) {
				/* increment cities */
				city1 = city2;
				city2 = it.next();

				/* map point */
				map.addPoint(city1.getName(), city1.getX(), city1.getY(), Color.BLUE);

				/* map blue road */
				map.addLine(city1.getX(), city1.getY(), city2.getX(), city2.getY(), Color.BLUE);
			}

			/* map red end point */
			map.addPoint(city2.getName(), city2.getX(), city2.getY(), Color.RED);
		}

		/* save map to image file */
		map.save(mapName);

		map.dispose();
	}


	private CanvasPlus drawPMQuadtree(int z) {
		final CanvasPlus canvas = new CanvasPlus("MeeshQuest");

		/* initialize canvas */
		canvas.setFrameSize(spatialWidth, spatialHeight);

		/* add a rectangle to show where the bounds of the map are located */
		canvas.addRectangle(0, 0, spatialWidth, spatialHeight, Color.BLACK, false);

		/* draw PM Quadtree */
		Integer level = new Integer(z);
		PMQuadtree pmQuadtree = pmqts.get(level);		
		if (pmQuadtree != null && !pmQuadtree.isEmpty()) {		
			drawPMQuadtreeHelper(pmQuadtree.getRoot(), canvas);
		}
		return canvas;
	}
	
	private void drawPMQuadtreeHelper(PMQuadtree.Node node, CanvasPlus canvas) {
		if (node.getType() == PMQuadtree.Node.BLACK) {
			PMQuadtree.Black blackNode = (PMQuadtree.Black) node;
			for (Geometry g : blackNode.getGeometry()) {
				if (g.isCity()) {
					City city = (City) g;
					canvas.addPoint(city.getName(), city.getX(), city.getY(), Color.BLACK);
				} else {
					Road road = (Road) g;
					canvas.addLine(road.getStart().getX(), road.getStart().getY(), 
					               road.getEnd().getX(), road.getEnd().getY(), Color.BLACK);
				}
			}
		} else if (node.getType() == PMQuadtree.Node.GRAY) {
			PMQuadtree.Gray grayNode = (PMQuadtree.Gray) node;
			canvas.addCross(grayNode.getCenterX(), grayNode.getCenterY(), grayNode.getHalfWidth(), Color.GRAY);
			for (int i = 0; i < 4; i++) {
				drawPMQuadtreeHelper(grayNode.getChild(i), canvas);
			}
		}
	}

}
