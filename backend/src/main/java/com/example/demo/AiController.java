package com.example.demo;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin("*")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/generate-route-points/{simulationId}")
    public Map<String, Object> generateRoutePoints(
            @PathVariable Long simulationId,
            @RequestParam(defaultValue = "8") int numRuns,
            @RequestParam(defaultValue = "15.0") double noiseLevel,
            @RequestParam(required = false) String direction) {
        return aiService.generatePointsForSavedRoute(simulationId, numRuns, noiseLevel, direction);
    }

    @PostMapping("/generate-all-routes-points")
    public Map<String, Object> generateAllRoutesPoints(
            @RequestParam(defaultValue = "8") int numRunsPerRoute,
            @RequestParam(defaultValue = "15.0") double noiseLevel,
            @RequestParam(required = false) String direction) {
        return aiService.generatePointsForAllSavedRoutes(numRunsPerRoute, noiseLevel, direction);
    }

    @PostMapping("/train")
    public Map<String, Object> trainModel() {
        return aiService.trainModel();
    }

    @PostMapping("/predict")
    public Map<String, Object> predictSequence(@RequestBody List<CoordinateDto> points) {
        return aiService.predictSequence(points);
    }
}
