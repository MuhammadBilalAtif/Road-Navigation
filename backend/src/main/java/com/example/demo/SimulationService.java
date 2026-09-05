package com.example.demo;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SimulationService {
    private final SavedSimulationRepository repository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public SimulationService(SavedSimulationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SimulationResponse> listSimulations() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(SavedSimulation::getName, String.CASE_INSENSITIVE_ORDER))
                .map(SimulationResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public SimulationResponse getSimulation(Long id) {
        return repository.findById(id)
                .map(SimulationResponse::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulation not found."));
    }

    @Transactional
    public SimulationResponse createSimulation(SimulationRequest request) {
        validate(request);

        String name = request.getName().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A simulation with this name already exists.");
        }

        SavedSimulation simulation = new SavedSimulation();
        simulation.setName(name);
        simulation.setStartPoint(createPoint(request.getStartPoint()));
        simulation.setEndPoint(createPoint(request.getEndPoint()));
        if (request.getDirection() != null && !request.getDirection().trim().isEmpty()) {
            simulation.setDirection(request.getDirection().trim().toUpperCase());
        } else {
            simulation.setDirection("BOTH");
        }

        for (VehicleRequest vehicleRequest : request.getVehicles()) {
            VehicleSimulation vehicle = new VehicleSimulation();
            vehicle.setAverageSpeed(vehicleRequest.getAverageSpeed());
            vehicle.setLoop(vehicleRequest.isLoop());
            vehicle.setSpeedNoise(vehicleRequest.getSpeedNoise());
            vehicle.setStartDelaySeconds(vehicleRequest.getStartDelaySeconds());
            vehicle.setTimeNoiseSeconds(vehicleRequest.getTimeNoiseSeconds());
            vehicle.setXCoordinateNoise(vehicleRequest.getXCoordinateNoise());
            vehicle.setYCoordinateNoise(vehicleRequest.getYCoordinateNoise());
            simulation.addVehicle(vehicle);
        }

        return new SimulationResponse(repository.save(simulation));
    }

    @Transactional
    public void deleteSimulation(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulation not found.");
        }
        repository.deleteById(id);
    }

    @Transactional
    public SimulationResponse replaceSimulationPoints(Long id, SimulationPointsRequest request) {
        SavedSimulation simulation = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Simulation not found."));
        if (request == null || request.getPoints() == null || request.getPoints().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one generated point is required.");
        }

        List<SimulationPoint> points = new ArrayList<>();
        for (SimulationPointRequest pointRequest : request.getPoints()) {
            validatePoint(pointRequest);
            SimulationPoint point = new SimulationPoint();
            point.setPointType(pointRequest.getPointType().trim());
            point.setRunIndex(pointRequest.getRunIndex());
            point.setStepIndex(pointRequest.getStepIndex());
            point.setTimeSeconds(pointRequest.getTimeSeconds());
            point.setSpeed(pointRequest.getSpeed());
            point.setRoadId(pointRequest.getRoadId());
            point.setDirection(pointRequest.getDirection() != null ? pointRequest.getDirection().trim().toUpperCase() : "FORWARD");
            point.setCoordinate(createPoint(pointRequest.getCoordinate()));
            points.add(point);
        }

        simulation.replacePoints(points);
        return new SimulationResponse(repository.save(simulation));
    }

    private void validate(SimulationRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Simulation payload is required.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Simulation name is required.");
        }
        if (request.getStartPoint() == null || request.getEndPoint() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start and end points are required.");
        }
        if (request.getVehicles() == null || request.getVehicles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one vehicle is required.");
        }
        for (VehicleRequest vehicle : request.getVehicles()) {
            if (vehicle.getAverageSpeed() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle average speed must be greater than zero.");
            }
            if (vehicle.getSpeedNoise() < 0 || vehicle.getSpeedNoise() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Speed noise must be between 0 and 100.");
            }
            if (vehicle.getStartDelaySeconds() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start delay cannot be negative.");
            }
            if (vehicle.getTimeNoiseSeconds() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time noise cannot be negative.");
            }
            if (vehicle.getXCoordinateNoise() < 0 || vehicle.getYCoordinateNoise() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinate noise cannot be negative.");
            }
        }
    }

    private void validatePoint(SimulationPointRequest point) {
        if (point == null || point.getCoordinate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Generated point coordinates are required.");
        }
        if (!"sample".equals(point.getPointType()) && !"median".equals(point.getPointType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Point type must be sample or median.");
        }
        if (point.getRunIndex() < 0 || point.getStepIndex() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Point indexes cannot be negative.");
        }
        if (point.getTimeSeconds() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Point time cannot be negative.");
        }
        if (point.getSpeed() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Point speed cannot be negative.");
        }
    }

    private org.locationtech.jts.geom.Point createPoint(CoordinateDto coordinate) {
        org.locationtech.jts.geom.Point point = geometryFactory.createPoint(new Coordinate(coordinate.getX(), coordinate.getY()));
        point.setSRID(4326);
        return point;
    }
}
