package com.example.demo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class AiService {

    private final NavigationService navigationService;
    private final SavedSimulationRepository repository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final RestTemplate restTemplate = new RestTemplate();
    private final String PYTHON_AI_URL = "http://127.0.0.1:8000";

    public AiService(NavigationService navigationService, SavedSimulationRepository repository) {
        this.navigationService = navigationService;
        this.repository = repository;
    }

    @Transactional
    public Map<String, Object> generatePointsForSavedRoute(Long simulationId, int numRuns, double noiseLevel, String direction) {
        SavedSimulation sim = repository.findById(simulationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saved route simulation not found."));

        List<Coordinate> routeCoords = navigationService.getPath(
                sim.getStartPoint().getX(), sim.getStartPoint().getY(),
                sim.getEndPoint().getX(), sim.getEndPoint().getY()
        );

        if (routeCoords.isEmpty() || routeCoords.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not find valid route geometry between start and end points.");
        }

        String effectiveDirection = (direction != null && !direction.isBlank())
                ? direction.trim().toUpperCase()
                : (sim.getDirection() != null ? sim.getDirection().trim().toUpperCase() : "BOTH");

        List<SimulationPoint> organicPoints = generateOrganicRuns(sim, routeCoords, sim.getId().intValue(), numRuns, noiseLevel, 1, effectiveDirection);
        sim.replacePoints(organicPoints);
        repository.save(sim);

        return Map.of(
            "status", "success",
            "simulationId", sim.getId(),
            "simulationName", sim.getName(),
            "direction", effectiveDirection,
            "generatedPoints", organicPoints.size(),
            "totalRuns", numRuns
        );
    }

    @Transactional
    public Map<String, Object> generatePointsForSavedRoute(Long simulationId, int numRuns, double noiseLevel) {
        return generatePointsForSavedRoute(simulationId, numRuns, noiseLevel, null);
    }

    @Transactional
    public Map<String, Object> generatePointsForAllSavedRoutes(int numRunsPerRoute, double noiseLevel, String direction) {
        List<SavedSimulation> simulations = repository.findAll();
        if (simulations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No user-saved routes exist yet. Please create and save at least one route first.");
        }

        Random rand = new Random();
        int totalGenerated = 0;

        for (SavedSimulation sim : simulations) {
            if ("Off-Road Noise Dataset".equalsIgnoreCase(sim.getName())) continue;

            List<Coordinate> routeCoords = navigationService.getPath(
                    sim.getStartPoint().getX(), sim.getStartPoint().getY(),
                    sim.getEndPoint().getX(), sim.getEndPoint().getY()
            );

            if (routeCoords.size() < 2) continue;

            String effectiveDirection = (direction != null && !direction.isBlank())
                    ? direction.trim().toUpperCase()
                    : (sim.getDirection() != null ? sim.getDirection().trim().toUpperCase() : "BOTH");

            List<SimulationPoint> points = generateOrganicRuns(sim, routeCoords, sim.getId().intValue(), numRunsPerRoute, noiseLevel, 1, effectiveDirection);
            sim.replacePoints(points);
            repository.save(sim);
            totalGenerated += points.size();
        }

        SavedSimulation offRoadSim = repository.findByNameIgnoreCase("Off-Road Noise Dataset").orElse(null);
        if (offRoadSim == null) {
            offRoadSim = new SavedSimulation();
            offRoadSim.setName("Off-Road Noise Dataset");
            offRoadSim.setStartPoint(geometryFactory.createPoint(new Coordinate(73.00, 33.65)));
            offRoadSim.setEndPoint(geometryFactory.createPoint(new Coordinate(73.05, 33.70)));
            offRoadSim.setDirection("NONE");
        }

        List<SimulationPoint> offRoadPoints = new ArrayList<>();
        int globalRunIndex = 1000;

        int offRoadRuns = Math.max(10, numRunsPerRoute * 2);
        for (int r = 0; r < offRoadRuns; r++) {
            globalRunIndex++;
            double startX = 73.00 + rand.nextDouble() * 0.12;
            double startY = 33.65 + rand.nextDouble() * 0.10;
            double dx = (rand.nextDouble() - 0.5) * 0.0006;
            double dy = (rand.nextDouble() - 0.5) * 0.0006;
            double timeSec = 0.0;
            double speed = 15.0 + rand.nextDouble() * 25.0;

            int steps = 12 + rand.nextInt(12);
            double currX = startX;
            double currY = startY;

            for (int s = 0; s < steps; s++) {
                currX += dx + (rand.nextGaussian() * 0.00025);
                currY += dy + (rand.nextGaussian() * 0.00025);

                SimulationPoint p = new SimulationPoint();
                p.setSimulation(offRoadSim);
                p.setPointType("sample");
                p.setRunIndex(globalRunIndex);
                p.setStepIndex(s);
                p.setTimeSeconds(round(timeSec, 2));
                p.setSpeed(round(speed, 2));
                p.setRoadId(0);
                p.setDirection("NONE");

                org.locationtech.jts.geom.Point geomPt = geometryFactory.createPoint(new Coordinate(currX, currY));
                geomPt.setSRID(4326);
                p.setCoordinate(geomPt);
                offRoadPoints.add(p);

                timeSec += 4.0;
            }
        }

        offRoadSim.replacePoints(offRoadPoints);
        repository.save(offRoadSim);

        return Map.of(
            "status", "success",
            "totalSavedRoutes", simulations.size(),
            "totalGeneratedPoints", totalGenerated + offRoadPoints.size()
        );
    }

    @Transactional
    public Map<String, Object> generatePointsForAllSavedRoutes(int numRunsPerRoute, double noiseLevel) {
        return generatePointsForAllSavedRoutes(numRunsPerRoute, noiseLevel, null);
    }

    private List<SimulationPoint> generateOrganicRuns(SavedSimulation sim, List<Coordinate> routeCoords, int targetRouteId, int numRuns, double noiseLevel, int startRunIndex, String direction) {
        Random rand = new Random();
        List<SimulationPoint> points = new ArrayList<>();
        double sigmaDrift = Math.max(0.00005, (noiseLevel / 100.0) * 0.0004);

        List<Coordinate> reverseRouteCoords = new ArrayList<>(routeCoords);
        Collections.reverse(reverseRouteCoords);

        for (int r = 1; r <= numRuns; r++) {
            int runIdx = startRunIndex + r - 1;
            double baseSpeed = 35.0 + rand.nextDouble() * 45.0;
            double timeSec = rand.nextDouble() * 5.0;

            boolean isReverse = "REVERSE".equalsIgnoreCase(direction)
                    || ("BOTH".equalsIgnoreCase(direction) && (r % 2 == 0));
            String runDirection = isReverse ? "REVERSE" : "FORWARD";
            List<Coordinate> activeCoords = isReverse ? reverseRouteCoords : routeCoords;

            for (int i = 0; i < activeCoords.size(); i++) {
                Coordinate curr = activeCoords.get(i);
                double perpX = 0.0;
                double perpY = 0.0;

                if (i < activeCoords.size() - 1) {
                    Coordinate next = activeCoords.get(i + 1);
                    double dx = next.x - curr.x;
                    double dy = next.y - curr.y;
                    double len = Math.hypot(dx, dy);
                    if (len > 0) {
                        perpX = -dy / len;
                        perpY = dx / len;
                    }
                }

                double offsetDist = rand.nextGaussian() * sigmaDrift;
                double longJitter = rand.nextGaussian() * (sigmaDrift * 0.3);

                double noisyX = curr.x + (perpX * offsetDist) + longJitter;
                double noisyY = curr.y + (perpY * offsetDist) + longJitter;

                double speedJitter = baseSpeed * (1.0 + (rand.nextGaussian() * (noiseLevel / 100.0) * 0.3));
                speedJitter = Math.max(5.0, speedJitter);

                SimulationPoint p = new SimulationPoint();
                p.setSimulation(sim);
                p.setPointType("sample");
                p.setRunIndex(runIdx);
                p.setStepIndex(i);
                p.setTimeSeconds(round(timeSec, 2));
                p.setSpeed(round(speedJitter, 2));
                p.setRoadId(targetRouteId);
                p.setDirection(runDirection);

                org.locationtech.jts.geom.Point geomPt = geometryFactory.createPoint(new Coordinate(noisyX, noisyY));
                geomPt.setSRID(4326);
                p.setCoordinate(geomPt);
                points.add(p);

                timeSec += (4.0 + rand.nextDouble() * 3.0);
            }
        }
        return points;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> trainModel() {
        List<SavedSimulation> simulations = repository.findAll();
        if (simulations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No user-saved routes found in database. Create and save routes first.");
        }

        List<Map<String, Object>> routeInfos = new ArrayList<>();
        List<Map<String, Object>> samples = new ArrayList<>();

        for (SavedSimulation sim : simulations) {
            if ("Off-Road Noise Dataset".equalsIgnoreCase(sim.getName())) continue;

            List<Coordinate> routeCoords = navigationService.getPath(
                    sim.getStartPoint().getX(), sim.getStartPoint().getY(),
                    sim.getEndPoint().getX(), sim.getEndPoint().getY()
            );

            List<Map<String, Double>> coordsList = new ArrayList<>();
            for (Coordinate c : routeCoords) {
                coordsList.add(Map.of("x", c.x, "y", c.y));
            }

            routeInfos.add(Map.of(
                "route_id", sim.getId().intValue(),
                "route_name", sim.getName(),
                "direction", sim.getDirection() != null ? sim.getDirection() : "BOTH",
                "coordinates", coordsList
            ));

            Map<String, List<SimulationPoint>> groupedRuns = new HashMap<>();
            for (SimulationPoint pt : sim.getPoints()) {
                String key = pt.getRunIndex() + "_" + pt.getPointType();
                groupedRuns.computeIfAbsent(key, k -> new ArrayList<>()).add(pt);
            }

            for (List<SimulationPoint> runPoints : groupedRuns.values()) {
                if (runPoints.size() < 2) continue;

                runPoints.sort(Comparator.comparingInt(SimulationPoint::getStepIndex));

                String runDir = runPoints.get(0).getDirection();
                if (runDir == null || runDir.isBlank()) {
                    runDir = sim.getDirection() != null ? sim.getDirection() : "FORWARD";
                }

                List<Map<String, Object>> ptsList = new ArrayList<>();
                for (SimulationPoint p : runPoints) {
                    ptsList.add(Map.of(
                        "x", p.getCoordinate().getX(),
                        "y", p.getCoordinate().getY(),
                        "speed", p.getSpeed(),
                        "time_seconds", p.getTimeSeconds()
                    ));
                }

                samples.add(Map.of(
                    "route_id", sim.getId().intValue(),
                    "route_name", sim.getName(),
                    "direction", runDir,
                    "points", ptsList
                ));
            }
        }

        SavedSimulation offRoadSim = repository.findByNameIgnoreCase("Off-Road Noise Dataset").orElse(null);
        if (offRoadSim != null) {
            Map<String, List<SimulationPoint>> groupedRuns = new HashMap<>();
            for (SimulationPoint pt : offRoadSim.getPoints()) {
                String key = pt.getRunIndex() + "_" + pt.getPointType();
                groupedRuns.computeIfAbsent(key, k -> new ArrayList<>()).add(pt);
            }

            for (List<SimulationPoint> runPoints : groupedRuns.values()) {
                if (runPoints.size() < 2) continue;
                runPoints.sort(Comparator.comparingInt(SimulationPoint::getStepIndex));

                List<Map<String, Object>> ptsList = new ArrayList<>();
                for (SimulationPoint p : runPoints) {
                    ptsList.add(Map.of(
                        "x", p.getCoordinate().getX(),
                        "y", p.getCoordinate().getY(),
                        "speed", p.getSpeed(),
                        "time_seconds", p.getTimeSeconds()
                    ));
                }

                samples.add(Map.of(
                    "route_id", 0,
                    "route_name", "Not on any route",
                    "direction", "NONE",
                    "points", ptsList
                ));
            }
        }

        if (samples.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No simulation points found in database. Run simulation point generation first.");
        }

        try {
            Map<String, Object> body = Map.of(
                "routes", routeInfos,
                "samples", samples
            );
            ResponseEntity<Map> response = restTemplate.postForEntity(PYTHON_AI_URL + "/train", body, Map.class);
            return response.getBody();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to call AI Service training endpoint: " + e.getMessage());
        }
    }

    public Map<String, Object> predictSequence(List<CoordinateDto> points) {
        if (points == null || points.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sequence of points is required.");
        }

        List<Map<String, Object>> ptsList = new ArrayList<>();
        double t = 0.0;
        for (CoordinateDto pt : points) {
            ptsList.add(Map.of(
                "x", pt.getX(),
                "y", pt.getY(),
                "speed", 50.0,
                "time_seconds", t
            ));
            t += 5.0;
        }

        try {
            Map<String, Object> body = Map.of("points", ptsList);
            ResponseEntity<Map> response = restTemplate.postForEntity(PYTHON_AI_URL + "/predict", body, Map.class);
            Map<String, Object> resBody = response.getBody();
            if (resBody == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Empty response from AI engine.");
            }

            int predictedRouteId = ((Number) resBody.get("route_id")).intValue();
            boolean isOnRoad = (Boolean) resBody.get("is_on_road");
            String routeName = (String) resBody.get("route_name");

            Map<String, Object> result = new HashMap<>(resBody);

            if (isOnRoad && predictedRouteId > 0) {
                SavedSimulation matchedSim = repository.findById((long) predictedRouteId).orElse(null);
                if (matchedSim != null) {
                    List<Coordinate> routeCoords = navigationService.getPath(
                            matchedSim.getStartPoint().getX(), matchedSim.getStartPoint().getY(),
                            matchedSim.getEndPoint().getX(), matchedSim.getEndPoint().getY()
                    );
                    List<Map<String, Double>> coordsList = new ArrayList<>();
                    for (Coordinate c : routeCoords) {
                        coordsList.add(Map.of("x", c.x, "y", c.y));
                    }
                    result.put("routeName", matchedSim.getName());
                    result.put("routeDirection", matchedSim.getDirection() != null ? matchedSim.getDirection() : "BOTH");
                    result.put("matchedRoadGeom", Map.of("type", "LineString", "coordinates", coordsList));
                }
            } else {
                result.put("routeName", "Not on any route");
                result.put("matchedRoadGeom", null);
            }

            return result;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to call AI inference service: " + e.getMessage());
        }
    }

    private double round(double val, int places) {
        double scale = Math.pow(10, places);
        return Math.round(val * scale) / scale;
    }
}
