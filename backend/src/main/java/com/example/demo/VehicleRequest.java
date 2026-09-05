package com.example.demo;

public class VehicleRequest {
    private double averageSpeed;
    private boolean loop;
    private double speedNoise;
    private double startDelaySeconds;
    private double timeNoiseSeconds;
    private double xCoordinateNoise;
    private double yCoordinateNoise;

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public double getSpeedNoise() {
        return speedNoise;
    }

    public void setSpeedNoise(double speedNoise) {
        this.speedNoise = speedNoise;
    }

    public double getStartDelaySeconds() {
        return startDelaySeconds;
    }

    public void setStartDelaySeconds(double startDelaySeconds) {
        this.startDelaySeconds = startDelaySeconds;
    }

    public double getTimeNoiseSeconds() {
        return timeNoiseSeconds;
    }

    public void setTimeNoiseSeconds(double timeNoiseSeconds) {
        this.timeNoiseSeconds = timeNoiseSeconds;
    }

    public double getXCoordinateNoise() {
        return xCoordinateNoise;
    }

    public void setXCoordinateNoise(double xCoordinateNoise) {
        this.xCoordinateNoise = xCoordinateNoise;
    }

    public double getYCoordinateNoise() {
        return yCoordinateNoise;
    }

    public void setYCoordinateNoise(double yCoordinateNoise) {
        this.yCoordinateNoise = yCoordinateNoise;
    }
}
