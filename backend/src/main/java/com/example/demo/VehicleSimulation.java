package com.example.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "simulation_vehicles")
public class VehicleSimulation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    private SavedSimulation simulation;

    @Column(nullable = false)
    private double averageSpeed;

    @Column(nullable = false)
    private boolean loop;

    @Column(nullable = false)
    private double speedNoise;

    @Column(nullable = false)
    private double startDelaySeconds;

    @Column(nullable = false)
    private double timeNoiseSeconds;

    @Column(nullable = false)
    private double xCoordinateNoise;

    @Column(nullable = false)
    private double yCoordinateNoise;

    public Long getId() {
        return id;
    }

    public SavedSimulation getSimulation() {
        return simulation;
    }

    public void setSimulation(SavedSimulation simulation) {
        this.simulation = simulation;
    }

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
