package com.emergya.mtsp.ga.genes;

import com.emergya.mtsp.model.Stop;
import com.emergya.mtsp.model.Vehicle;
import java.util.List;
import org.jenetics.Gene;
import org.jenetics.util.RandomRegistry;

/**
 *
 * @author lroman
 */
public class StopGene implements Gene<Integer, StopGene> {
    
    private int stopIdx;    
    private int vehicleIdx;
    
    private List<Stop> stops;
    private List<Vehicle> vehicles;
    

    public  StopGene(int i, List<Stop> stops, List<Vehicle> vehicles) {
        this.stopIdx= i;
        this.stops = stops;
        this.vehicles = vehicles;
    }

    @Override
    public Integer getAllele() {
        return getVehicle();
    }

    @Override
    public StopGene newInstance() {
        StopGene g = new StopGene(this.stopIdx, this.stops, this.vehicles);
        int idx = RandomRegistry.getRandom().nextInt(vehicles.size());
        g.setVehicle(idx);
        return g;
    }

    @Override
    public StopGene newInstance(Integer value) {
        StopGene g = new StopGene(stopIdx, this.stops, this.vehicles);
        g.setVehicle(value);
        return g;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * @return the stop
     */
    public int getStop() {
        return stopIdx;
    }

    /**
     * @param stop the stop to set
     */
    public void setStop(int stop) {
        this.stopIdx = stop;
    }

    /**
     * @return the vehicle
     */
    public int getVehicle() {
        return vehicleIdx;
    }

    /**
     * @param vehicle the vehicle to set
     */
    public void setVehicle(int vehicle) {
        this.vehicleIdx = vehicle;
    }

    
    @Override
    public String toString() {
        return String.format("{Stop: %s, Vehicle: %s}", stopIdx, vehicleIdx);
    }
}
