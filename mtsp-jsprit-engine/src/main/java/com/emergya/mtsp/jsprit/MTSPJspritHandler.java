package com.emergya.mtsp.jsprit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import jsprit.analysis.toolbox.StopWatch;
import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.io.VehicleRoutingAlgorithms;
import jsprit.core.algorithm.listener.VehicleRoutingAlgorithmListeners.Priority;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import jsprit.core.problem.job.Service;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.TourActivities;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.VehicleImpl;
import jsprit.core.problem.vehicle.VehicleType;
import jsprit.core.problem.vehicle.VehicleTypeImpl;
import jsprit.core.reporting.SolutionPrinter;
import jsprit.core.reporting.SolutionPrinter.Print;
import jsprit.core.util.Coordinate;
import jsprit.core.util.Solutions;

import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.emergya.mtsp.model.Stop;
import com.emergya.mtsp.model.Vehicle;
import com.emergya.mtsp.response.MTSPRoute;
import com.emergya.mtsp.response.MTSPRoutes;
import com.emergya.mtsp.util.MTSPUtil;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;

/**
*
* @author marcos
*/
@Component
public class MTSPJspritHandler {
	
	final int WEIGHT_INDEX = 0;
	
	@Value("${smc.mtsp.osmFilePath}")
	private String OSM_FILE_PATH;
	@Value("${smc.mtsp.graphPath}")
	private String GRAPH_PATH;
	@Value("${smc.mtsp.vehicle}")
	private String VEHICLE;
	@Value("${smc.mtsp.weighting}")
	private String WEIGHTING;
	
	private GraphHopper hopper;

	public MTSPRoutes calculateMTSP(List<Stop> origin, final List<Stop> stops, final List<Vehicle> vehicles) {
		MTSPRoutes res = new MTSPRoutes();
		
		VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("car").addCapacityDimension(WEIGHT_INDEX,stops.size());
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
		
//		MTSPRoutingCosts mtspcosts = new MTSPRoutingCosts(hopper);
//		vrpBuilder.setRoutingCost(mtspcosts);
		
		vrpBuilder.setFleetSize(FleetSize.FINITE);
		VehicleRoutingProblem problem = vrpBuilder.build();
		
		/*
		 * solve the problem
		 */
		VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem, "com/emergya/mtsp/resource/algorithmConfig.xml");
		
		vra.getAlgorithmListeners().addListener(new StopWatch(), Priority.HIGH);
		Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();
		
		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
		
		List<VehicleRoute> routes = (List<VehicleRoute>) bestSolution.getRoutes();
		GHPoint endPoint = null;
		for(VehicleRoute r:routes){
			MTSPRoute route = new MTSPRoute(new Vehicle(r.getVehicle().getId(), 0.0));
			TourActivities activities = r.getTourActivities();
			Iterator<TourActivity> it = activities.iterator();
			List<GHPoint> points = new LinkedList<GHPoint>();
			// Start point
			GHPoint startPoint = MTSPUtil.getCoordinatesFromString(r.getStart().getLocationId());
			points.add(startPoint);
			// End point
			endPoint = MTSPUtil.getCoordinatesFromString(r.getEnd().getLocationId());
			//points.add(endPoint);
			// Intermediate points
			while(it.hasNext()){
				// Get the coordinates string
				TourActivity ta = it.next();
				String index = ta.getLocationId();
				// Get Coordinates point
				GHPoint point = MTSPUtil.getCoordinatesFromString(index);
				points.add(point);
			}
			
			GHRequest request = new GHRequest(points).setVehicle(this.VEHICLE).setWeighting(this.WEIGHTING);
			GHResponse response = hopper.route(request);
			PointList result_points = response.getPoints();
			
			GHRequest last_request = new GHRequest(points.get(points.size()-1).getLat(), points.get(points.size()-1).getLon(), endPoint.getLat(), endPoint.getLon());
			GHResponse last_response = hopper.route(last_request);
			PointList last_result_points = last_response.getPoints();
			// Get geometry linestring
			LineString geom = getRoute(result_points);
			LineString last_geom = getRoute(last_result_points);
			for(LngLatAlt el:last_geom.getCoordinates()){
				geom.add(el);
			}
			route.setGeometry(geom);
			res.getRoutes().add(route);
		}
		
		//SolutionPrinter.print(problem,bestSolution,Print.VERBOSE);
		
		return res;
	}
	
	private LineString getRoute(PointList points) {
		LineString route = new LineString();
		for (int p = 0; p < points.getSize(); p++) {
			Double latitude = points.getLatitude(p);
			Double longitude = points.getLongitude(p);
			route.add(new LngLatAlt(longitude, latitude));
		}
		return route;
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
		
		int i=0;
		for (Stop s:origin){
			VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance("vehicle" + i);
			vehicleBuilder.setStartLocationCoordinate(Coordinate.newInstance(s.getLongitude(), s.getLatitude()));
			vehicleBuilder.setType(vehicleType); 
			VehicleImpl vehicle = vehicleBuilder.build();
			vehicles.add(vehicle);
			i++;
		};
		
		return vehicles;
	}
	
	@PostConstruct
	public void init() {
		hopper = new GraphHopper().forServer();
		hopper.setInMemory(true);
		hopper.disableCHShortcuts();
		hopper.setOSMFile(this.OSM_FILE_PATH);
		hopper.setGraphHopperLocation(this.GRAPH_PATH);
		hopper.setEncodingManager(new EncodingManager(this.VEHICLE));
		
		hopper.importOrLoad();
	}
	
}
