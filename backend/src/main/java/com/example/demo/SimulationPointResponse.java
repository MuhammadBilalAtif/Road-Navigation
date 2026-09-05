package com.example.demo;

public class SimulationPointResponse {
    private String pointType;
    private int runIndex;
    private int stepIndex;
    private double timeSeconds;
    private double speed;
    private int roadId;
    private String direction;
    private CoordinateDto coordinate;

    public SimulationPointResponse() {
    }

    public SimulationPointResponse(SimulationPoint point) {
        this.pointType = point.getPointType();
        this.runIndex = point.getRunIndex();
        this.stepIndex = point.getStepIndex();
        this.timeSeconds = point.getTimeSeconds();
        this.speed = point.getSpeed();
        this.roadId = point.getRoadId();
        this.direction = point.getDirection();
        this.coordinate = new CoordinateDto(point.getCoordinate().getX(), point.getCoordinate().getY());
    }

    public String getDirection() {
        return direction;
    }

    public int getRoadId() {
        return roadId;
    }

    public String getPointType() {
        return pointType;
    }

    public int getRunIndex() {
        return runIndex;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public double getTimeSeconds() {
        return timeSeconds;
    }

    public double getSpeed() {
        return speed;
    }

    public CoordinateDto getCoordinate() {
        return coordinate;
    }
}
