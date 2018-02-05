package de.dfki.parking.behavior.guidance;

import de.dfki.parking.behavior.IUpdateStrategy;
import de.dfki.parking.knowledge.ParkingMap;
import de.dfki.parking.model.GuidanceSystem;
import de.dfki.parking.model.Parking;
import de.dfki.parking.model.ParkingState;

public final class GuidanceSystemUpdateStrategy implements IUpdateStrategy {
  // Local ParkingMap instance to update
  private final ParkingMap localMap;
  
  // Guidance system instance to update
  private final GuidanceSystem guidanceSystem;
  
  private final ParkingState parkingState;
  
  /**
   * Creates a new instance updating the given local ParkingMap and GuidanceSystem.
   * 
   * @param localMap Local ParkingMap
   * @param globalMap Global ParkingMap
   * @param sensorCar True to indicate that a sensor car is used to update the
   * Maps, false otherwise
   */
  public GuidanceSystemUpdateStrategy(ParkingMap localMap, GuidanceSystem guidanceSystem, ParkingState parkingState) {
    this.localMap = localMap;
    this.guidanceSystem = guidanceSystem;
    this.parkingState = parkingState;
  }
  
  @Override
  public void update(Parking parking, int nSpots, int nFreeSpots, double price, long time, boolean parked) {
    // Always update 
    localMap.update(parking, nSpots, nFreeSpots, price, time);

    if (parked) {
      guidanceSystem.update(parkingState.getParkingReservationId(), parking, nSpots, nFreeSpots, price, time);
      parkingState.setParkingReservationId(-1);
      
    } else if (parkingState.hasSensorCar()) {
      guidanceSystem.update(parking, nSpots, nFreeSpots, price, time);

    }
  }
}