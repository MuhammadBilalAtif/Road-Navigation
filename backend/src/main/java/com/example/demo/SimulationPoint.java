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
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "simulation_points")
public class SimulationPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    private SavedSimulation simulation;

    @Column(nullable = false)
    private String pointType;

    @Column(nullable = false)
    private int runIndex;

    @Column(nullable = false)
    private int stepIndex;

    @Column(nullable = false)
    private double timeSeconds;

    @Column(nullable = false)
    private double speed;

    @Column(name = "road_id", nullable = false, columnDefinition = "integer default 0")
    private int roadId = 0;

    @Column(name = "direction", columnDefinition = "varchar(32) default 'FORWARD'")
    private String direction = "FORWARD";

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point coordinate;

    public int getRoadId() {
        return roadId;
    }

    public void setRoadId(int roadId) {
        this.roadId = roadId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public SavedSimulation getSimulation() {
        return simulation;
    }

    public void setSimulation(SavedSimulation simulation) {
        this.simulation = simulation;
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType;
    }

    public int getRunIndex() {
        return runIndex;
    }

    public void setRunIndex(int runIndex) {
        this.runIndex = runIndex;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public double getTimeSeconds() {
        return timeSeconds;
    }

    public void setTimeSeconds(double timeSeconds) {
        this.timeSeconds = timeSeconds;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public Point getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Point coordinate) {
        this.coordinate = coordinate;
    }
}
