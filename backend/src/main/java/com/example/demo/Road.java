package com.example.demo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Road {
    private Integer id;

    @JsonIgnore
    private LineString geom;

    @JsonProperty("geom")
    public Map<String, Object> getSimpleGeom() {
        if (geom == null) return null;
        List<Map<String, Double>> coords = new ArrayList<>();
        for (Coordinate c : geom.getCoordinates()) {
            coords.add(Map.of("x", c.x, "y", c.y));
        }
        return Map.of("type", "LineString", "coordinates", coords);
    }

    public LineString getGeom() { return geom; }
    public void setGeom(LineString geom) { this.geom = geom; }
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
}