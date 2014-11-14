package com.emergya.mtsp.model;

/**
 *
 * @author lroman
 */
public class Vehicle {
    private String name;
    private double costPerKM;

    public Vehicle(String name, double costPerKM) {
        this.name = name;
        this.costPerKM = costPerKM;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the costPerKM
     */
    public double getCostPerKM() {
        return costPerKM;
    }

    /**
     * @param costPerKM the costPerKM to set
     */
    public void setCostPerKM(double costPerKM) {
        this.costPerKM = costPerKM;
    }
}
