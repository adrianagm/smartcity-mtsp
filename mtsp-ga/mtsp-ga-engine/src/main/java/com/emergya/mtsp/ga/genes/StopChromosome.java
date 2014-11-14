/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.emergya.mtsp.ga.genes;

import com.emergya.mtsp.model.Stop;
import com.emergya.mtsp.model.Vehicle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jenetics.AbstractChromosome;
import org.jenetics.Chromosome;
import org.jenetics.util.Array;
import org.jenetics.util.ISeq;

/**
 *
 * @author lroman
 */
public class StopChromosome extends AbstractChromosome<StopGene>{
    
    private List<Stop> avalaibleStops;    
    private List<Vehicle> avalailbeVehicles;
    
    public StopChromosome(ISeq<StopGene> genes) {
        super(genes);
    }
    
     /**
     * @return the avalaibleStops
     */
    public List<Stop> getAvalaibleStops() {
        return avalaibleStops;
    }

    public static StopChromosome fromStops(List<Stop> stops, List<Vehicle> vehicles) {
        StopChromosome c = new StopChromosome(createRandomStopGenes(stops, vehicles));
        c.avalaibleStops = stops;
        c.avalailbeVehicles = vehicles;
        return c;
    }

    @Override
    public Chromosome<StopGene> newInstance(ISeq<StopGene> genes) {
        StopChromosome c =  new StopChromosome(genes);
        c.avalaibleStops = this.avalaibleStops;
        c.avalailbeVehicles = this.avalailbeVehicles;
        return c;
    }

    @Override
    public Chromosome<StopGene> newInstance() {
        
        StopChromosome c = new StopChromosome(createRandomStopGenes(this.avalaibleStops, this.avalailbeVehicles));
        c.avalaibleStops = this.avalaibleStops;
        c.avalailbeVehicles = this.avalailbeVehicles;
        return c;
    }
    
    public static ISeq<StopGene> createRandomStopGenes(List<Stop> stops, List<Vehicle> vehicles) {
        List<StopGene> genes = new ArrayList<>();
        
        for(int i=0; i< stops.size(); i++) {
            stops.get(i).setIndex(i);
            genes.add((new StopGene(i, stops, vehicles)).newInstance());
        }
        
        Collections.shuffle(genes);
        
        ISeq<StopGene> seq = Array.of(genes).toISeq();
        return seq;
    }
}
