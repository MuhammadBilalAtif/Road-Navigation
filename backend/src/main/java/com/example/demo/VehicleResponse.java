package com.example.demo;

public class VehicleResponse {
    private double averageSpeed;
    private boolean loop;
    private double speedNoise;
    private double startDelaySeconds;
    private double timeNoiseSeconds;
    private double xCoordinateNoise;
    private double yCoordinateNoise;

    public VehicleResponse() {
    }

    public VehicleResponse(VehicleSimulation vehicle) {
        this.averageSpeed = vehicle.getAverageSpeed();
        this.loop = vehicle.isLoop();
        this.speedNoise = vehicle.getSpeedNoise();
        this.startDelaySeconds = vehicle.getStartDelaySeconds();
        this.timeNoiseSeconds = vehicle.getTimeNoiseSeconds();
        this.xCoordinateNoise = vehicle.getXCoordinateNoise();
        this.yCoordinateNoise = vehicle.getYCoordinateNoise();
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public boolean isLoop() {
        return loop;
    }

    public double getSpeedNoise() {
        return speedNoise;
    }

    public double getStartDelaySeconds() {
        return startDelaySeconds;
    }

    public double getTimeNoiseSeconds() {
        return timeNoiseSeconds;
    }

    public double getXCoordinateNoise() {
        return xCoordinateNoise;
    }

    public double getYCoordinateNoise() {
        return yCoordinateNoise;
    }
}
