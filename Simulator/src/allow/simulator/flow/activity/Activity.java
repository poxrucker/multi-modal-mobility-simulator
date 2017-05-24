package allow.simulator.flow.activity;

import allow.simulator.entity.Entity;

/**
 * Abstract class representing an Activity to be executed by an entity in a
 * flow.
 * 
 * @author Andreas Poxrucker (DFKI)
 *
 */
public abstract class Activity<V extends Entity> {
  public enum ActivityType {
    
    /**
     * Bus activities.
     */
    PREPARE_TRIP,
    
    PICKUP_AND_WAIT,
    
    DRIVE_TO_NEXT_STOP,
    
    RETURN_TO_AGENCY,

    /**
     * Person activities.
     */
    USE_PUBLIC_TRANSPORT,
    
    USE_TAXI,
    
    PLAN_JOURNEY,
    
    FILTER_ALTERNATIVES,
    
    RANK_ALTERNATIVES,
    
    PREPARE_JOURNEY,
    
    DRIVE,
    
    WALK,
    
    CYCLE,
    
    CORRECT_POSITION,
    
    REPLAN,
    
    WAIT,
    
    /**
     * Taxi activities.
     */
    PREPARE_TAXI_TRIP,
    
    DRIVE_TO_NEXT_DESTINATION,
    
    PICK_UP_OR_DROP,
    
    RETURN_TO_TAXI_AGENCY,
    
    /**
     * Transportation agency activities.
     */
    SCHEDULE_NEXT_TRIPS,

    SCHEDULE_NEXT_FLEXIBUS_TRIPS,
    
    SCHEDULE_NEXT_TAXI_TRIPS,
    
    /**
     * General activities.
     */
    LEARN
  }
  
	// Indicates if Activity has finished
	private boolean finished;
	
	// Type of the Activity
	protected final ActivityType type;
	
	// Entity executing the activity
	protected final V entity;
	
	/**
	 * Constructor.
	 * 
	 * @param type Type of the Activity.
	 * @param entitiy Entity supposed to execute the Activity.
	 */
	protected Activity(ActivityType type, V entity) {
		this.type = type;
		this.entity = entity;
		finished = false;
	}
	
	/**
	 * Execute the Activity, which may require more than one call of execute().
	 * If the returned time is smaller than deltaT, activity has finished before
	 * deltaT.
	 * 
	 * @param deltaT Time to execute the activity.
	 * @return Time needed to execute the activity.
	 */
	public abstract double execute(double deltaT);
	
	/**
	 * Check, if current Activity is finished.
	 * 
	 * @return True, if Activity is finished, false otherwise.
	 */
	public boolean isFinished() {
		return finished;
	}
	
	/**
	 * Marks this Activity instance as finished.
	 */
	public void setFinished() {
		finished = true;
	}
	
	/**
	 * Returns the type of the Activity.
	 * 
	 * @return Type of the Activity.
	 */
	public ActivityType getType() {
		return type;
	}
}
