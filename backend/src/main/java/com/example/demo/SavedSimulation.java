package com.example.demo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "saved_simulations")
public class SavedSimulation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point startPoint;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point endPoint;

    @Column(name = "direction", nullable = false, columnDefinition = "varchar(32) default 'BOTH'")
    private String direction = "BOTH";

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<VehicleSimulation> vehicles = new ArrayList<>();

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("pointType ASC, runIndex ASC, stepIndex ASC")
    private List<SimulationPoint> points = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Point startPoint) {
        this.startPoint = startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public List<VehicleSimulation> getVehicles() {
        return vehicles;
    }

    public void addVehicle(VehicleSimulation vehicle) {
        vehicles.add(vehicle);
        vehicle.setSimulation(this);
    }

    public List<SimulationPoint> getPoints() {
        return points;
    }

    public void replacePoints(List<SimulationPoint> newPoints) {
        points.clear();
        for (SimulationPoint point : newPoints) {
            points.add(point);
            point.setSimulation(this);
        }
    }
}
