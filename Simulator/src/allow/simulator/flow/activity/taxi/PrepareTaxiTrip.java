package allow.simulator.flow.activity.taxi;

import java.time.LocalTime;
import java.util.Iterator;
import java.util.List;

import allow.simulator.entity.Taxi;
import allow.simulator.flow.activity.Activity;
import allow.simulator.flow.activity.person.Learn;
import allow.simulator.mobility.data.Stop;
import allow.simulator.mobility.data.Trip;
import allow.simulator.world.Street;

public final class PrepareTaxiTrip extends Activity<Taxi> {
	// Trip to execute
	private Trip trip;

	/**
	 * Creates a new instance of the activity specifying the entity
	 * to execute the trip and the trip to execute.
	 * 
	 * @param entity Taxi entity to execute the trip
	 * @param trip Trip to execute.
	 */
	public PrepareTaxiTrip(Taxi entity, Trip trip) {
		super(ActivityType.PREPARE_TAXI_TRIP, entity);
		this.trip = trip;
	}

	/**
	 * Creates a sequence of activities to execute the given trip and adds it to
	 * the flow of the given taxi entity.
	 * 
	 * @param deltaT Time interval
	 */
	@Override
	public double execute(double deltaT) {
		// Check trip.
		List<Stop> tripStops = trip.getStops();
		List<LocalTime> tripStopTimes = trip.getStopTimes();

		if ((tripStops.size() != tripStopTimes.size()) || tripStops.size() == 0
				|| (trip.getTraces().size() != (tripStops.size()))) {
			throw new IllegalStateException("Error: Trip is inconsistent. Number of stops: "
							+ tripStops.size() + ", number of times: "
							+ tripStopTimes.size() + ", number of traces: "
							+ trip.getTraces().size());
		}
		// Prepare trip by creating a PickUpAndWait activity for each stop and
		// a DriveToNextStop for each trace, and finally set transport trip.
		Iterator<Stop> stopIterator = trip.getStops().iterator();
		Iterator<LocalTime> timesIterator = trip.getStopTimes().iterator();
		Iterator<List<Street>> tracesIterator = trip.getTraces().iterator();

		while (stopIterator.hasNext()) {
			// Add activity to drive to next stop.
			entity.getFlow().addActivity(new DriveToNextDestination(entity, tracesIterator.next()));

			// Add activity to wait and pick up passengers at next stop.
			entity.getFlow().addActivity(new PickupOrDrop(entity, stopIterator.next(), timesIterator.next()));
		}
		// Add return activity.
		entity.getFlow().addActivity(new ReturnToTaxiAgency(entity));
		entity.getFlow().addActivity(new Learn(entity));

		// Set trip.
		entity.setCurrentTrip(trip);
		entity.setCurrentDelay(0);
		setFinished();
		return 0;
	}

}
