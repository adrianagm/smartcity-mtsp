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

import com.emergya.mtsp.ga.MTSPRoute;
import com.emergya.mtsp.ga.MTSPRoutes;
import com.emergya.mtsp.model.Stop;
import com.emergya.mtsp.model.Vehicle;
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
		for(VehicleRoute r:routes){
			MTSPRoute route = new MTSPRoute(new Vehicle(r.getVehicle().getId(), 0.0));
			TourActivities activities = r.getTourActivities();
			Iterator<TourActivity> it = activities.iterator();
			List<GHPoint> points = new LinkedList<GHPoint>();
			// Start point
			Double[] startCoordinate = getCoordinates(r.getStart().getLocationId());
			GHPoint startPoint = new GHPoint(startCoordinate[1], startCoordinate[0]);
			points.add(startPoint);
			// End point
			Double[] endCoordinate = getCoordinates(r.getEnd().getLocationId());
			GHPoint endPoint = new GHPoint(endCoordinate[1], endCoordinate[0]);
			points.add(endPoint);
			// Intermediate points
			while(it.hasNext()){
				// Get the coordinates string
				TourActivity ta = it.next();
				String index = ta.getLocationId();
				// Get Coordinates point
				Double[] coord = getCoordinates(index);
				GHPoint point = new GHPoint(coord[1], coord[0]);
				points.add(point);
			}
			
			GHRequest request = new GHRequest(points).setVehicle(this.VEHICLE).setWeighting(this.WEIGHTING);
			GHResponse response = hopper.route(request);
			PointList result_points = response.getPoints();
			// Get geometry linestring
			LineString geom = getRoute(result_points);
			route.setGeometry(geom);
			res.getRoutes().add(route);
		}
		
		SolutionPrinter.print(problem,bestSolution,Print.VERBOSE);
		
		//new GraphStreamViewer(problem, bestSolution).labelWith(Label.ID).setRenderDelay(200).display();
		
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

	private Double[] getCoordinates(String coordinate) {
		// [x=-22.2][y=33.3]
		Double[] d = new Double[2];
		String[] splitted = null;
		
		while(coordinate.indexOf("[") > -1){
			coordinate = coordinate.replace("[", "");
		}
		
		while(coordinate.indexOf("]") > -1){
			if(coordinate.indexOf("]") < coordinate.length()-1){
				coordinate = coordinate.replace("]", ",");
			}
		}
		
		if(coordinate.indexOf(",") > -1){
			splitted = coordinate.split(",");
		}
		Map<String, String> coord_kv = new HashMap<String, String>();
		for(int i=0; i<splitted.length; i++){
			if(splitted[i].indexOf("=") > -1){
				coord_kv.put(splitted[i].split("=")[0], splitted[i].split("=")[1]);
			}
		}
		
		d[0] = Double.parseDouble(coord_kv.get("x"));
		d[1] = Double.parseDouble(coord_kv.get("y"));

		return d;
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
