package de.dfki.parking.model;

import allow.simulator.util.Coordinate;

public final class ParkingPossibility {
  // Parking instance
  private final Parking parking;
  
  // Position of parking
  private final Coordinate position;
  
  // Estimated utility
  private double estimatedUtility;
  
  public ParkingPossibility(Parking parking, Coordinate position, double estimatedUtility) {
    this.parking = parking;
    this.position = position;
    this.estimatedUtility = estimatedUtility;
  }
  
  public Parking getParking() {
    return parking;
  }
  
  public Coordinate getPosition() {
    return position;
  }
  
  public double getEstimatedUtility() {
    return estimatedUtility;
  }
}