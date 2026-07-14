package com.ole.turapp.service;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the {@code <rte>} routes out of a GPX file.
 *
 * <p>Only track-planning routes are read: each {@code <rte>} becomes one
 * {@link ParsedRoute}, and its {@code <rtept lat lon>} children become the
 * ordered points. Waypoints ({@code <wpt>}) and tracks ({@code <trk>}) are
 * ignored for now.
 *
 * <p>The XML parser is hardened against XXE: DTDs and external entities are
 * disabled, so a malicious upload cannot read local files or make network calls.
 */
@Component
public class GpxRouteParser {

    /** One route parsed from GPX: a name, optional description, and its coordinates. */
    public record ParsedRoute(String name, String description, List<Coordinate> points) {}

    /** A latitude/longitude pair in WGS84 decimal degrees. */
    public record Coordinate(double latitude, double longitude) {}

    /**
     * Reads every {@code <rte>} from the given GPX stream.
     *
     * @throws IllegalArgumentException if the stream is not parseable GPX/XML
     */
    public List<ParsedRoute> parse(InputStream gpx) {
        Document document = parseDocument(gpx);

        List<ParsedRoute> routes = new ArrayList<>();
        NodeList rteNodes = document.getElementsByTagName("rte");
        for (int i = 0; i < rteNodes.getLength(); i++) {
            Element rte = (Element) rteNodes.item(i);
            routes.add(parseRoute(rte));
        }
        return routes;
    }

    private ParsedRoute parseRoute(Element rte) {
        String name = firstChildText(rte, "name");
        String description = firstChildText(rte, "desc");

        List<Coordinate> points = new ArrayList<>();
        NodeList children = rte.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && "rtept".equals(child.getNodeName())) {
                Element rtept = (Element) child;
                points.add(new Coordinate(
                        parseCoord(rtept.getAttribute("lat"), "lat"),
                        parseCoord(rtept.getAttribute("lon"), "lon")));
            }
        }
        return new ParsedRoute(name, description, points);
    }

    private double parseCoord(String value, String attr) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Rutepunkt mangler påkrevd attributt '" + attr + "'");
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ugyldig " + attr + "-verdi i rutepunkt: '" + value + "'");
        }
    }

    /** Text of the first direct child element with the given tag name, or {@code null}. */
    private String firstChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                String text = child.getTextContent();
                if (text == null || text.isBlank()) {
                    return null;
                }
                return text.trim();
            }
        }
        return null;
    }

    private Document parseDocument(InputStream gpx) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Harden against XXE: no DTDs, no external entities.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setExpandEntityReferences(false);
            // Tag names are matched without namespaces, e.g. "rte" and "rtept".
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(gpx);
            document.getDocumentElement().normalize();
            return document;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("Kunne ikke lese GPX-filen: " + e.getMessage(), e);
        }
    }
}
