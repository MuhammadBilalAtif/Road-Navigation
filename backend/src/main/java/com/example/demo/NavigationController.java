package com.example.demo;

import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class NavigationController {

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private SimulationService simulationService;

    @GetMapping("/roads")
    public List<Road> getRoads() {
        return navigationService.getRoads();
    }

    @GetMapping("/path")
    public List<Coordinate> getPath(
            @RequestParam double startX, @RequestParam double startY,
            @RequestParam double endX, @RequestParam double endY) {
        return navigationService.getPath(startX, startY, endX, endY);
    }

    @GetMapping("/simulations")
    public List<SimulationResponse> getSimulations() {
        return simulationService.listSimulations();
    }

    @GetMapping("/simulations/{id}")
    public SimulationResponse getSimulation(@PathVariable Long id) {
        return simulationService.getSimulation(id);
    }

    @PostMapping("/simulations")
    public SimulationResponse createSimulation(@RequestBody SimulationRequest request) {
        return simulationService.createSimulation(request);
    }

    @DeleteMapping("/simulations/{id}")
    public void deleteSimulation(@PathVariable Long id) {
        simulationService.deleteSimulation(id);
    }

    @PostMapping("/simulations/{id}/points")
    public SimulationResponse replaceSimulationPoints(
            @PathVariable Long id,
            @RequestBody SimulationPointsRequest request) {
        return simulationService.replaceSimulationPoints(id, request);
    }
}
