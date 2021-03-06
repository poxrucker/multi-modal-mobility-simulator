package allow.simulator.mobility.planner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import allow.simulator.util.Coordinate;

/**
 * Collection of parameters for a journey request to the planner service.
 * 
 * @author Andreas Poxrucker (DFKI)
 *
 */
public class JourneyRequest {
	/**
	 * Request id to identify requests belonging together
	 */
	public long ReqId;
	
	/**
	 * Request number identifying individual requests sharing the same request Id
	 */
	public int ReqNumber;
	
	/**
	 * Specifies the OTP router to use
	 */
	public String OTPRouterID;
	
	/**
	 * Arrival date of the journey
	 */
	public LocalDate Date;
	
	/**
	 * Departure time of the journey
	 */
	public LocalTime DepartureTime;
	
	/**
	 * Arrival time of the journey
	 */
	public LocalTime ArrivalTime;
	
	/**
	 * Starting position
	 */
	public Coordinate From;
	
	/**
	 * Starting points
	 */
	public List<Coordinate> StartingPoints;
	
	/**
	 * Destination
	 */
	public Coordinate To;
		
	/**
	 * Destinations in case of a shared taxi request
	 */
	public List<Coordinate> Destinations;
	
	/**
	 * Type of route to optimize for
	 */
	public RType RouteType;
	
	/**
	 * Modes of transportation to use
	 */
	public TType TransportTypes[];
	
	/**
	 * Number of results to return
	 */
	public int ResultsNumber;
	
	/**
	 * Maximum distance to walk.
	 */
	public int MaximumWalkDistance;
	
	private JourneyRequest() {}

	public static JourneyRequest createRequest(Coordinate from, Coordinate to, 
			LocalDateTime date, boolean arriveBy, TType modes[], RequestId reqId) {
		JourneyRequest s = new JourneyRequest();
		s.ReqId = reqId.getRequestId();
		s.ReqNumber = reqId.getNextRequestNumber();
		s.Date = date.toLocalDate();
		
		if (arriveBy) {
			s.ArrivalTime = date.toLocalTime();
			
		} else {
			s.DepartureTime = date.toLocalTime();
		}
		s.From = from;
		s.To = to;
		s.RouteType = RType.QUICK;
		s.TransportTypes = modes;
		s.ResultsNumber = 1;
		s.MaximumWalkDistance = 1000;
		return s;
	}
	
	public static JourneyRequest createSharedRequest(Coordinate pickupPoint,
			List<Coordinate> from, List<Coordinate> to,
			LocalDateTime date, boolean arriveBy, TType[] types, RequestId reqId) {
		
		if (from.size() != to.size())
			throw new IllegalArgumentException("Error: Number of starting points and destinations does not match.");
		
		JourneyRequest s = new JourneyRequest();
		s.ReqId = reqId.getRequestId();
		s.ReqNumber = reqId.getNextRequestNumber();
		s.Date = date.toLocalDate();

		if (arriveBy)
			s.ArrivalTime = date.toLocalTime();
		else
			s.DepartureTime = date.toLocalTime();
		// Set pickup point
		s.From = pickupPoint;
		
		// Set starting positions and destinations
		s.StartingPoints = from;
		s.Destinations = to;

		// Set route type.
		s.RouteType = RType.QUICK;

		// Set predefined choice of means of transportation.
		s.TransportTypes = types;
		s.ResultsNumber = 1;
		s.MaximumWalkDistance = 0;
		return s;
	}
}