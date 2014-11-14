package com.emergya.mtsp.jsprit;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import jsprit.analysis.toolbox.AlgorithmSearchProgressChartListener;
import jsprit.analysis.toolbox.GraphStreamViewer;
import jsprit.analysis.toolbox.GraphStreamViewer.Label;
import jsprit.analysis.toolbox.StopWatch;
import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListeners.Priority;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import jsprit.core.problem.job.Service;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.vehicle.VehicleImpl;
import jsprit.core.problem.vehicle.VehicleType;
import jsprit.core.problem.vehicle.VehicleTypeImpl;
import jsprit.core.reporting.SolutionPrinter;
import jsprit.core.reporting.SolutionPrinter.Print;
import jsprit.core.util.Coordinate;
import jsprit.core.util.Solutions;

import org.springframework.stereotype.Component;

import com.emergya.mtsp.ga.MTSPRoutes;
import com.emergya.mtsp.model.Stop;
import com.emergya.mtsp.model.Vehicle;

/**
*
* @author marcos
*/
@Component
public class MTSPJspritHandler {
	
	final int WEIGHT_INDEX = 0;

	public MTSPRoutes calculateMTSP(List<Stop> origin, final List<Stop> stops, final List<Vehicle> vehicles) {
		
		VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType").addCapacityDimension(WEIGHT_INDEX,2);
		VehicleType vehicleType = vehicleTypeBuilder.build();
		
		List<VehicleImpl> vehicle_list = getVehicleList(origin, vehicleType);
		List<Service> services = getServiceList(stops, WEIGHT_INDEX);
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		
		for (VehicleImpl v:vehicle_list){
			vrpBuilder.addVehicle(v);
		}
		
		for (Service service: services){
			vrpBuilder.addJob(service);
		}
		
		vrpBuilder.setFleetSize(FleetSize.FINITE);
		VehicleRoutingProblem problem = vrpBuilder.build();
		
		/*
		 * solve the problem
		 */
		VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem, "algorithmConfig.xml");
		vra.getAlgorithmListeners().addListener(new StopWatch(),Priority.HIGH);
		Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();
		
		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
		SolutionPrinter.print(problem,bestSolution,Print.VERBOSE);
		
		new GraphStreamViewer(problem, bestSolution).labelWith(Label.ID).setRenderDelay(200).display();
		
		return null;
	}

	private List<Service> getServiceList(List<Stop> stops, int wEIGHT_INDEX2) {
		List<Service> services = new LinkedList<Service>();
		
		for (int i=0; i<stops.size(); i++){
			Service service = Service.Builder.newInstance(Integer.toString(i)).addSizeDimension(WEIGHT_INDEX,1).setCoord(Coordinate.newInstance(stops.get(i).getLongitude(), stops.get(i).getLatitude())).build();
			services.add(service);
		}
		
		return services;
	}

	private List<VehicleImpl> getVehicleList(List<Stop> origin, VehicleType vehicleType) {
		List<VehicleImpl> vehicles = new LinkedList<VehicleImpl>();
		
		for (Stop s:origin){
			VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance("vehicle");
			vehicleBuilder.setStartLocationCoordinate(Coordinate.newInstance(s.getLongitude(), s.getLatitude()));
			vehicleBuilder.setType(vehicleType); 
			VehicleImpl vehicle = vehicleBuilder.build();
			vehicles.add(vehicle);
		};
		
		return vehicles;
	}
	
}
