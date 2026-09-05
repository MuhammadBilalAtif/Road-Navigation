package com.example.demo;

import java.util.List;

public class SimulationResponse {
    private Long id;
    private String name;
    private CoordinateDto startPoint;
    private CoordinateDto endPoint;
    private String direction;
    private List<VehicleResponse> vehicles;
    private List<SimulationPointResponse> points;

    public SimulationResponse() {
    }

    public SimulationResponse(SavedSimulation simulation) {
        this.id = simulation.getId();
        this.name = simulation.getName();
        this.direction = simulation.getDirection() != null ? simulation.getDirection() : "BOTH";
        this.startPoint = new CoordinateDto(
                simulation.getStartPoint().getX(),
                simulation.getStartPoint().getY()
        );
        this.endPoint = new CoordinateDto(
                simulation.getEndPoint().getX(),
                simulation.getEndPoint().getY()
        );
        this.vehicles = simulation.getVehicles().stream()
                .map(VehicleResponse::new)
                .toList();
        this.points = simulation.getPoints().stream()
                .map(SimulationPointResponse::new)
                .toList();
    }

    public String getDirection() {
        return direction;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CoordinateDto getStartPoint() {
        return startPoint;
    }

    public CoordinateDto getEndPoint() {
        return endPoint;
    }

    public List<VehicleResponse> getVehicles() {
        return vehicles;
    }

    public List<SimulationPointResponse> getPoints() {
        return points;
    }
}
