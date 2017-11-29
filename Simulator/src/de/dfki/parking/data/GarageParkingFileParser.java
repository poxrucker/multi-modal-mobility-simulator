package de.dfki.parking.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class GarageParkingFileParser implements IParkingDataFileParser {
  
  @Override
  public List<ParkingData> parseFile(Path fqfn) throws IOException {
    List<ParkingData> ret = new ObjectArrayList<>();

    try (BufferedReader reader = Files.newBufferedReader(fqfn)) {
      String line = null;

      while ((line = reader.readLine()) != null) {
        ParkingData parking = parseGarageParking(line);
        ret.add(parking);
      }
    }
    return ret;
  }

  private static ParkingData parseGarageParking(String str) {
    String[] tokens = str.split(";");
    String name = tokens[0].trim();
    name = name.substring(0, 1).toUpperCase() + name.substring(1);
    String address = tokens[1].substring(0, 1).toUpperCase() + tokens[1].substring(1).trim();
    int numberOfParkingSpots = !tokens[2].equals("") ? Integer.parseInt(tokens[2]) : 0;
    double pricePerHour = !tokens[3].equals("") ? Double.parseDouble(tokens[3].replaceAll(",", ".")) : 0;
    
    if (tokens.length != 5)
      System.out.println(name);
    
    List<String> osmNodes = new ObjectArrayList<>((tokens.length > 4) ? tokens[4].split(",") : new String[0]);
    return new ParkingData(name, address, pricePerHour, numberOfParkingSpots, osmNodes);
  }
}
