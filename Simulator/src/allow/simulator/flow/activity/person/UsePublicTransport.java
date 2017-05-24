package allow.simulator.flow.activity.person;

import java.time.LocalTime;
import java.util.List;

import allow.simulator.entity.Bus;
import allow.simulator.entity.Person;
import allow.simulator.entity.TransportationEntity;
import allow.simulator.exchange.Relation;
import allow.simulator.flow.activity.Activity;
import allow.simulator.mobility.data.Stop;
import allow.simulator.mobility.data.Trip;

/**
 * Represents an activity to use public transport (bus).
 * 
 * @author Andreas Poxrucker (DFKI)
 *
 */
public class UsePublicTransport extends Activity<Person> {
	// The stops to get in and out
	private Stop in;
	private Stop out;
	private String agencyId;
	private Trip trip;
	
	// The bus a person entered
	private Bus b;
	
	// Earliest starting time of the activity
	private LocalTime earliestStartingTime;
	
	// Utility state variables
	private boolean reachedStop;
	private boolean enteredBus;
	private boolean leftBus;
	
	/**
	 * Creates new activity to use a bus given start and end stop.
	 * 
	 * @param person Person to execute the activity
	 * @param start Starting stop
	 * @param dest Destination stop
	 * @param departure Time when public transportation departs from stop
	 *        according to schedule
	 */
	public UsePublicTransport(Person person, Stop start, Stop dest, String agencyId, Trip trip, LocalTime departure) {
		super(ActivityType.USE_PUBLIC_TRANSPORT, person);
		earliestStartingTime = departure;
		reachedStop = false;
		enteredBus = false;
		this.agencyId = agencyId;
		leftBus = false;
		this.trip = trip;
		in = start;
		out = dest;
	}

	/**
	 * Executes one step of this activity.
	 */
	@Override
	public double execute(double deltaT) {
		// Register relations update.
		entity.getRelations().addToUpdate(Relation.Type.DISTANCE);

		if (!reachedStop) {
			// If person has not reached stop yet, set position, add person to waiting passengers, and set flag.
			entity.setPosition(in.getPosition());
			in.addWaitingPerson(entity);
			reachedStop = true;
			return 0.0;
			
		} else if (!enteredBus) {
			// Try to get transportation mean.
			if (b == null) {
				b = entity.getContext().getTransportationRepository().getBusAgency(agencyId).getVehicleOfTrip(trip.getTripId());
			}
			// Reaching a stop needs zero time.
			if (b != null && entity.getContext().getTime().getCurrentTime().isAfter(earliestStartingTime.plusSeconds(b.getCurrentDelay() + 300))) {
				entity.getFlow().clear();
				entity.getFlow().addActivity(new Replan(entity));
				setFinished();
				return 0.0;
				
			} else if (entity.getContext().getTime().getCurrentTime().isAfter(earliestStartingTime.plusSeconds(1800))) {
				// Dirty error handling for missed busses
				entity.getFlow().clear();
				entity.getExperienceBuffer().clear();
				entity.getExperienceBuffer().trimToSize();
				entity.setPosition(entity.getCurrentItinerary().to);
				entity.setCurrentItinerary(null);

				/*person.getFlow().clear();
				person.getFlow().addActivity(new Replan(person));*/
				setFinished();
				return 0.0;
				
			}
			
			// If person has not entered the correct means yet, check in stop for waiting vehicles.
			if (in.hasWaitingTransportationEntity()) {
				List<TransportationEntity> waiting = in.getTransportationEntities();

				for (int i = 0; i < waiting.size(); i++) {
					Bus transport = (Bus) waiting.get(i);
					
					if (transport.getCurrentTrip().getTripId().equals(trip.getTripId())) {
						boolean success = transport.addPassenger(entity);
						
						if (success) {
							in.removeWaitingPerson(entity);
							enteredBus = true;
							b = transport;
							break;
							
						} else {
							entity.getFlow().clear();
							entity.getFlow().addActivity(new Replan(entity));
							setFinished();
						}
					}
				}
			}
			return deltaT;

		} else if (enteredBus && !leftBus){
			// If on bus, trigger knowledge exchange.
			//entity.getRelations().addToUpdate(Relation.Type.BUS);
			
			// If person entered bus, update position.
			if ((b.getCurrentStop() != null)) {
				if (b.getCurrentStop().getStopId().equals(out.getStopId())) {
					b.removePassenger(entity);
					leftBus = true;
					setFinished();
				}
			}
		}
		return deltaT;
	}
	
	public Bus getMeansOfTransportation() {
		return b;
	}
	
	public LocalTime getEarliestStartingTime() {
		return earliestStartingTime;
	}
	
	public String toString() {
		return "UsePublicTransport " + entity.toString() + " from " + in.getStopId() + " to " + out.getStopId() + " trip " + trip.getTripId() + " " + enteredBus + " " + leftBus + "earliest starting: " + earliestStartingTime.toString(); 
	}
}
