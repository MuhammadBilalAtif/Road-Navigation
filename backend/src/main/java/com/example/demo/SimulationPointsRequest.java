package com.example.demo;

import java.util.ArrayList;
import java.util.List;

public class SimulationPointsRequest {
    private List<SimulationPointRequest> points = new ArrayList<>();

    public List<SimulationPointRequest> getPoints() {
        return points;
    }

    public void setPoints(List<SimulationPointRequest> points) {
        this.points = points;
    }
}
