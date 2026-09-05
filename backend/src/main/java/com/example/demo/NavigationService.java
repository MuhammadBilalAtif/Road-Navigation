package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class NavigationService {

    private final List<Road> roads = new ArrayList<>();
    private final GeometryFactory gf = new GeometryFactory();
    private final Graph<Coordinate, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    private DijkstraShortestPath<Coordinate, DefaultWeightedEdge> dijkstra;

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream("/islamabad_roads.json");
            if (is == null) {
                System.err.println("islamabad_roads.json not found in resources!");
                return;
            }
            JsonNode root = mapper.readTree(is);
            JsonNode elements = root.get("elements");
            if (elements != null && elements.isArray()) {
                int roadId = 1;
                for (JsonNode elem : elements) {
                    if ("way".equals(elem.path("type").asText())) {
                        JsonNode geomNode = elem.get("geometry");
                        if (geomNode != null && geomNode.isArray() && geomNode.size() > 1) {
                            List<Coordinate> coordsList = new ArrayList<>();
                            for (JsonNode pt : geomNode) {
                                double lat = pt.path("lat").asDouble();
                                double lon = pt.path("lon").asDouble();
                                coordsList.add(new Coordinate(lon, lat));
                            }
                            Coordinate[] coordsArray = coordsList.toArray(new Coordinate[0]);
                            LineString lineString = gf.createLineString(coordsArray);

                            Road road = new Road();
                            road.setId(roadId++);
                            road.setGeom(lineString);
                            roads.add(road);
                        }
                    }
                }
            }
            System.out.println("Loaded " + roads.size() + " roads from islamabad_roads.json");

            buildGraph();
            dijkstra = new DijkstraShortestPath<>(graph);
            System.out.println("Graph built with " + graph.vertexSet().size() + " vertices and " + graph.edgeSet().size() + " edges.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildGraph() {
        for (Road r : roads) {
            Coordinate[] coords = r.getGeom().getCoordinates();
            for (int i = 0; i < coords.length - 1; i++) {
                Coordinate p1 = coords[i];
                Coordinate p2 = coords[i + 1];
                
                graph.addVertex(p1);
                graph.addVertex(p2);
                
                DefaultWeightedEdge e = graph.addEdge(p1, p2);
                if (e != null) {
                    graph.setEdgeWeight(e, p1.distance(p2));
                }
            }
        }
    }

    public List<Road> getRoads() {
        return roads;
    }

    public List<Coordinate> getPath(double startX, double startY, double endX, double endY) {
        Coordinate clickStart = new Coordinate(startX, startY);
        Coordinate clickEnd = new Coordinate(endX, endY);

        Coordinate snappedStart = findNearestVertex(clickStart);
        Coordinate snappedEnd = findNearestVertex(clickEnd);

        if (snappedStart == null || snappedEnd == null) return new ArrayList<>();

        try {
            var pathData = dijkstra.getPath(snappedStart, snappedEnd);
            return (pathData != null) ? pathData.getVertexList() : new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private Coordinate findNearestVertex(Coordinate click) {
        Coordinate best = null;
        double minFound = Double.MAX_VALUE;
        for (Coordinate v : graph.vertexSet()) {
            double dist = v.distance(click);
            if (dist < minFound) {
                minFound = dist;
                best = v;
            }
        }
        return best;
    }
}
