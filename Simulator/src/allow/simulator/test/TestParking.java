package allow.simulator.test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import allow.simulator.world.StreetMap;
import de.dfki.parking.data.ParkingDataRepository;
import de.dfki.parking.model.ParkingIndex;
import de.dfki.parking.model.ParkingRepository;

public final class TestParking {

  public static void main(String[] args) throws IOException {
    Path streetMapPath = Paths.get("/Users/Andi/Documents/DFKI/VW simulation/models/data/world/trento_merged.world");
    Path streetParkingPath = Paths.get("/Users/Andi/Documents/DFKI/VW simulation/models/parking_spot/street_parking.csv");
    Path garageParkingPath = Paths.get("/Users/Andi/Documents/DFKI/VW simulation/models/parking_spot/garage_parking.csv");

    StreetMap streetMap = new StreetMap(streetMapPath);
    ParkingDataRepository parkingDataRepository = ParkingDataRepository.load(streetParkingPath, garageParkingPath);
    ParkingRepository parkingRepository = ParkingRepository.initialize(parkingDataRepository, 1.0);
    ParkingIndex index = ParkingIndex.build(streetMap, parkingRepository);
    System.out.println("Done");
  }
  
  
}
