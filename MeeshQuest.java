//package cmsc420.meeshquest.part2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
import cmsc420.command.Command;
import cmsc420.drawing.CanvasPlus;
import cmsc420.xml.XmlUtility;
*/

public class MeeshQuest {

	/* input stream/file */
//	private final InputStream xmlInput = System.in;
	private File xmlInput;

	private File xmlOutput;

	/* output DOM Document tree */
	private Document results;

	/* processes each command */
	private Command command;

	public static void main(String[] args) {
		final MeeshQuest m = new MeeshQuest();
		m.processInput();
	}
	

	public void processInput() {
		try {
			
			// testing purposes
//			final String testName = "primary";
//			final String testName = "heptaTrieDeletePublic";
			final String testName = "portal";
			xmlInput = new File(testName + ".input.xml");
			xmlOutput = new File("my."+testName + ".output.xml");

			/* validate document */
			Document doc = XmlUtility.validateNoNamespace(xmlInput);
			
			
			int leafOrder;
			//Read the order values.
			Element docElement = doc.getDocumentElement();
			leafOrder = Integer.parseInt(docElement.getAttribute("leafOrder"));
			command = new Command(leafOrder);
			
			/* create output */
			results = XmlUtility.getDocumentBuilder().newDocument();
			command.setResults(results);

			

			/* process commands element */
			Element commandNode = doc.getDocumentElement();
			processCommand(commandNode);

			/* process each command */
			final NodeList nl = commandNode.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				if (nl.item(i).getNodeType() == Document.ELEMENT_NODE) {
					/* need to check if Element (ignore comments) */
					commandNode = (Element) nl.item(i);
					processCommand(commandNode);
				}
			}
		} catch (SAXException e) {
			e.printStackTrace();
			addFatalError();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			addFatalError();
		} catch (IOException e) {
			e.printStackTrace();
			addFatalError();
		} catch (TransformerException e) {
			e.printStackTrace();
			addFatalError();
		} finally {
			try {
				/* print results to XML */
				XmlUtility.write(results, xmlOutput);
				XmlUtility.print(results);
			} catch (TransformerException e) {
				System.exit(-1);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private void addFatalError() {
		try {
			results = XmlUtility.getDocumentBuilder().newDocument();
			final Element fatalError = results.createElement("fatalError");
			results.appendChild(fatalError);
		} catch (ParserConfigurationException e) {
			System.exit(-1);
		}
	}

	private void processCommand(final Element commandNode) throws IOException,
			ParserConfigurationException, TransformerException {
		final String name = commandNode.getNodeName();

		if (name.equals("commands")) {
			command.processCommands(commandNode);
		} else if (name.equals("createCity")) {
			command.processCreateCity(commandNode);
		} else if (name.equals("deleteCity")) {
			command.processDeleteCity(commandNode);
		} else if (name.equals("clearAll")) {
			command.processClearAll(commandNode);
		} else if (name.equals("listCities")) {
			command.processListCities(commandNode);
		} else if (name.equals("printHeptaTrie")) {
			command.processPrintHeptaTrie(commandNode);
		} else if (name.equals("mapRoad")) {
			command.processMapRoad(commandNode);
		} else if (name.equals("mapPortal")) {
			command.processMapPortal(commandNode);
		} else if (name.equals("unmapRoad")) {
			command.processUnmapRoad(commandNode);
		} else if (name.equals("unmapPortal")) {
			command.processUnmapPortal(commandNode);
		} else if (name.equals("printPMQuadtree")) {
			command.processPrintPMQuadtree(commandNode);
		} else if (name.equals("saveMap")) {
			command.processSaveMap(commandNode);
		} else if (name.equals("rangeCities")) {
			command.processRangeCities(commandNode);
		} else if (name.equals("rangeRoads")) {
			command.processRangeRoads(commandNode);
		} else if (name.equals("nearestCity")) {
			command.processNearestCity(commandNode);
		} else if (name.equals("nearestPortal")) {
			command.processNearestPortal(commandNode);
		} else if (name.equals("shortestPath")) {
			command.processShortestPath(commandNode);
		} else {
			/* problem with the Validator */
			System.exit(-1);
		}
	}
}
