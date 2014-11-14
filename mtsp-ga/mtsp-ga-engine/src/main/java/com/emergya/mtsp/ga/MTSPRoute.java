/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emergya.mtsp.ga;

import com.emergya.mtsp.model.Stop;
import com.emergya.mtsp.model.Vehicle;
import com.graphhopper.GHResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.geojson.Feature;
import org.geojson.LineString;
import org.geojson.LngLatAlt;

/**
 *
 * @author lroman
 */
public class MTSPRoute implements Serializable {

    private final Vehicle vehicle;
    private final List<Stop> stops;

    private final LineString geometry;

    public MTSPRoute(Vehicle vehicle) {
        this.vehicle = vehicle;
        this.stops = new ArrayList<>();

        geometry = new LineString();
    }

    public Feature getFeature() {
        Feature f = new Feature();
        f.setId(vehicle.getName());
        f.setGeometry(geometry);
        return f;
    }

    /**
     * @return the vehicle
     */
    public Vehicle getVehicle() {
        return vehicle;
    }

    /**
     * @return the stops
     */
    public List<Stop> getStops() {
        return stops;
    }

    void createGeometry(GHResponse[][] distances) {
        addGeometry(distances.length-1, stops.get(0).getIndex(), distances);
        for (int i = 1; i < stops.size(); i++) {
            addGeometry(stops.get(i-1).getIndex(), stops.get(i).getIndex(), distances);
        }
        addGeometry(stops.get(stops.size()-1).getIndex(), distances.length-1, distances);
    }

    private void addGeometry(int idx1, int idx2, GHResponse[][] distances) {
        GHResponse route = distances[idx1][idx2];
        List<Double[]> points = route.getPoints().toGeoJson();

        for (Double[] p : points) {
            geometry.add(new LngLatAlt(p[0], p[1]));
        }
    }
}
