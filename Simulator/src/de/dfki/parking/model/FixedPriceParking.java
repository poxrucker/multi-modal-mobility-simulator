package de.dfki.parking.model;

public final class FixedPriceParking extends Parking {

  public FixedPriceParking(int id, Type type, String name, String address, double defaultPricePerHour, int numberOfParkingSpots) {
    super(id, type, name, address, defaultPricePerHour, numberOfParkingSpots);
  }

  @Override
  public double getCurrentPricePerHour() {
    return defaultPricePerHour;
  }

  @Override
  public String toString() {
    return "[FixedPriceParking " + name + "]";
  }
}