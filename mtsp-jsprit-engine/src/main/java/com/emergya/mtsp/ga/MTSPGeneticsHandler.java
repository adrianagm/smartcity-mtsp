package com.emergya.mtsp.ga;

import com.emergya.mtsp.ga.genes.StopChromosome;
import com.emergya.mtsp.ga.genes.StopGene;
import com.emergya.mtsp.model.Stop;
import com.emergya.mtsp.model.Vehicle;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopperAPI;
import com.graphhopper.http.GraphHopperWeb;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jenetics.BoltzmannSelector;
import org.jenetics.Crossover;
import org.jenetics.ExponentialRankSelector;
import org.jenetics.GeneticAlgorithm;
import org.jenetics.Genotype;
import org.jenetics.LinearRankSelector;
import org.jenetics.MonteCarloSelector;
import org.jenetics.MultiPointCrossover;
import org.jenetics.Mutator;
import org.jenetics.Optimize;
import org.jenetics.RouletteWheelSelector;
import org.jenetics.SinglePointCrossover;
import org.jenetics.SwapMutator;
import org.jenetics.TournamentSelector;
import org.jenetics.util.Factory;
import org.jenetics.util.Function;
import org.jenetics.util.MSeq;
import org.springframework.stereotype.Component;

/**
 *
 * @author lroman
 */
@Component
public class MTSPGeneticsHandler {

    public MTSPRoutes calculateMTSP(Stop origin, final List<Stop> stops, final List<Vehicle> vehicles) {

        final GHResponse[][] distances = new GHResponse[stops.size()+1][stops.size()+1];
        
       
        
        final int ORIGIN_IDX = distances.length -1;
        
        Factory<Genotype<StopGene>> gtf = Genotype.of(
                StopChromosome.fromStops(stops, vehicles)
        );
        
        final List<Stop> distanceStops = new ArrayList<>(stops);
        
        distanceStops.add(origin);

        Function<Genotype<StopGene>, Double> ff = new Function<Genotype<StopGene>, Double>() {

            @Override
            public Double apply(Genotype<StopGene> t) {
              
                Integer [] previousIdx = new Integer[vehicles.size()];
                Double [] costs = new Double[vehicles.size()];
                
                for (StopGene g : t.getChromosome()) {
                    Integer previous = previousIdx[g.getVehicle()];                    
                    Double count = costs[g.getVehicle()];  
                    
                    if(count==null) {
                        count = 0.0;
                    }
                    
                    if (previous != null) {
                        count += this.calculateDistance(previous, g.getStop());
                    } else {
                        // The last stop contains the origin.
                        count += this.calculateDistance(ORIGIN_IDX, g.getStop());
                    }

                    
                    costs[g.getVehicle()] = count;
                    previousIdx[g.getVehicle()] = g.getStop();
                    
                }
                
                for(int  v =0; v < costs.length; v++) {
                    Double cost = costs[v];
                    if(cost==null) {
                        // No route for the vehicle;
                        continue;
                    }
                    cost+= this.calculateDistance(previousIdx[v], ORIGIN_IDX);
                    costs[v]+= cost;
                }
                
                
                
                double maxCost = 0;
                for(Double cost : costs) {
                    if(cost != null && cost > maxCost) {
                        maxCost = cost;
                    }
//                    if(cost!=null) {
//                        maxCost+=cost;
//                    }
                }

                return maxCost;
            }

            private GraphHopperAPI gh;

            public final double calculateDistance(int stopIdx, int stopIdx2) {
                if (distances[stopIdx][stopIdx2] != null) {
                    return distances[stopIdx][stopIdx2].getDistance();
                }

                if (gh == null) {
                    gh = new GraphHopperWeb();
                    gh.load("http://localhost:8989/route");
                }

                Stop startStop = distanceStops.get(stopIdx);
                Stop endStop = distanceStops.get(stopIdx2);

                GHRequest request = new GHRequest(startStop.getLatitude(), startStop.getLongitude(), endStop.getLatitude(), endStop.getLongitude());
                try {
                    GHResponse response = gh.route(request);
                    distances[stopIdx][stopIdx2] = response;

                    return response.getMillis();
                } catch (Exception e) {
                    return Double.MAX_VALUE;
                }

            }
        };

        GeneticAlgorithm<StopGene, Double> ga = new GeneticAlgorithm<>(gtf, ff, Optimize.MINIMUM);

        ga.setAlterers(new SwapMutator<StopGene>(.5), new Mutator<StopGene>(.2));
        ga.setSurvivorSelector(new BoltzmannSelector<StopGene, Double>());
        ga.setup();
        
        int plateauCount = 0;
        double previousFitness= 0;
        double maxIters = stops.size()*vehicles.size();
        double maxPlateau = maxIters*0.1;
        int generationsPerIter = (int) Math.pow(1.5, stops.size()*vehicles.size());
        int i;
        for (i=0; i < maxIters; i++) {
            ga.evolve(generationsPerIter);           
           
            
            
            if(ga.getBestPhenotype().getFitness()== previousFitness) {
                plateauCount++;
                if(plateauCount> maxPlateau) {
                    break;
                }
            } else {
                System.out.println(String.format("iter: %s/%s, fitness: %s, streak: %s, %.2f%%",
                    i, maxIters,
                    ga.getBestPhenotype().getFitness(),
                    plateauCount, 100*plateauCount/maxPlateau));
            
                plateauCount=0;
            }
            
            
            previousFitness = ga.getBestPhenotype().getFitness();
        }
        
        System.out.println(String.format("iter: %s, fitness: %s, streak: %s, %.2f%%",
                    i,
                    ga.getBestPhenotype().getFitness(),
                    plateauCount, 100*plateauCount/maxPlateau));

        
        MTSPRoute[] routesByVehicle = new MTSPRoute[vehicles.size()];

        for (StopGene g : ga.getBestPhenotype().getGenotype().getChromosome()) {

            int v = g.getVehicle();
            MTSPRoute route;
            if(routesByVehicle[v]==null) {
                routesByVehicle[v] = new MTSPRoute(vehicles.get(v));
            }
            
            routesByVehicle[v].getStops().add(stops.get(g.getStop()));
        }

        MTSPRoutes routes = new MTSPRoutes();

        for (MTSPRoute r : routesByVehicle) {
            if(r!=null){
                r.createGeometry(distances);
                routes.getRoutes().add(r);
            }
        }

        return routes;
    }

}
