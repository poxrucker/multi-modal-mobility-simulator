package allow.simulator.flow.activity.taxi;

import java.util.List;

import allow.simulator.entity.Person;
import allow.simulator.entity.Taxi;
import allow.simulator.flow.activity.ActivityType;
import allow.simulator.flow.activity.MovementActivity;
import allow.simulator.knowledge.Experience;
import allow.simulator.knowledge.Experience;
import allow.simulator.mobility.planner.TType;
import allow.simulator.mobility.planner.TaxiPlanner;
import allow.simulator.util.Coordinate;
import allow.simulator.util.Geometry;
import allow.simulator.world.Street;
import allow.simulator.world.StreetSegment;

public class DriveToNextDestination extends MovementActivity {
	
	public DriveToNextDestination(Taxi taxi, List<Street> path) {
		// Constructor of super class.
		super(ActivityType.DRIVE_TO_NEXT_DESTINATION, taxi, path);
		
		if (!path.isEmpty()) {
			currentSegment.addVehicle();
		}
	}

	@Override
	public double execute(double deltaT) {
		if (currentSegment != null) currentSegment.removeVehicle();

		if (isFinished()) {
			return 0;
		}
		// Note tStart.
		if (tStart == -1) {
			tStart = entity.getContext().getTime().getTimestamp();
		}
				
		// Transportation entity.
		Taxi taxi = (Taxi) entity;
		
		// Move public transportation and passengers.
		double rem = travel(deltaT);
		taxi.setPosition(getCurrentPosition());
		
		for (Person pass : taxi.getPassengers()) {
			pass.setPosition(taxi.getPosition());
		}
				
		if (isFinished()) {
					
			for (Experience ex : experiences) {
				//taxi.getKnowledge().collect(ex);
				
				for (Person pass : taxi.getPassengers()) {
					pass.getExperienceBuffer().add(ex);
				}
			}
		} else {
			currentSegment = getCurrentSegment();
			currentSegment.addVehicle();
		}
		return rem;
	}

	/**
	 * 
	 * 
	 * @param travelTime Time interval for travelling.
	 * @return Time used to travel which may be less than travelTime,
	 * if journey finishes before travelTime is over.
	 */
	private double travel(double travelTime) {
		double deltaT = 0.0;
		
		while (deltaT < travelTime && !isFinished()) {
			// Get current state.
			StreetSegment s = getCurrentSegment();
			double v = s.getBusDrivingSpeed();
			Coordinate p = getCurrentPosition();
			
			// Compute distance to next segment (i.e. end of current segment).
			double distToNextSeg = Geometry.haversineDistance(p, s.getEndPoint());

			// Compute distance to travel within deltaT seconds.
			double distToTravel = (travelTime - deltaT) * v;
					
			if (distToTravel >= distToNextSeg) {
				// If distance to travel is bigger than distance to next segment,
				// a new log entry needs to be created.
				double tNextSegment = distToNextSeg / v;
				streetTravelTime += tNextSegment;

				distOnSeg = 0.0;
				segmentIndex++;
				
				Street street = getCurrentStreet();

				if (segmentIndex == street.getNumberOfSubSegments()) {
					double sumTravelTime = streetTravelTime; // + tNextSegment;
					tEnd = tStart + (long) sumTravelTime;
					
					Experience newEx = new Experience(street,
							sumTravelTime,
							street.getLength() * TaxiPlanner.COST_PER_METER,
							TType.TAXI, 
							tStart,
							tEnd,
							s.getNumberOfVehicles(),
							-1.0,
							((Taxi) entity).getCurrentTrip().getTripId(),
							entity.getContext().getWeather().getCurrentState());
					experiences.add(newEx);
					streetTravelTime = 0.0;
					distOnStreet = 0.0;
					streetIndex++;
					segmentIndex = 0;
					tStart = tEnd;
				}
				deltaT += tNextSegment;
				
			} else {
				// If distance to next segment is bigger than distance to travel,
				// update time on segment, travelled distance, and reset deltaT.
				streetTravelTime += (travelTime - deltaT);
				distOnSeg += distToTravel;
				distOnStreet += distToTravel;
				deltaT += (travelTime - deltaT);
			}
			if (experiences.size() == path.size())
				setFinished();
		}
		return deltaT;
	}
}
