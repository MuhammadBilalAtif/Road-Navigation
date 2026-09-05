package com.example.demo;

import java.util.ArrayList;
import java.util.List;

public class SimulationRequest {
    private String name;
    private CoordinateDto startPoint;
    private CoordinateDto endPoint;
    private String direction = "BOTH";
    private List<VehicleRequest> vehicles = new ArrayList<>();

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CoordinateDto getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(CoordinateDto startPoint) {
        this.startPoint = startPoint;
    }

    public CoordinateDto getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(CoordinateDto endPoint) {
        this.endPoint = endPoint;
    }

    public List<VehicleRequest> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<VehicleRequest> vehicles) {
        this.vehicles = vehicles;
    }
}
