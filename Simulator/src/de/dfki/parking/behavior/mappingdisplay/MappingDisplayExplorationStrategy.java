package de.dfki.parking.behavior.mappingdisplay;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import allow.simulator.util.Coordinate;
import allow.simulator.util.Geometry;
import allow.simulator.util.Pair;
import de.dfki.parking.behavior.IExplorationStrategy;
import de.dfki.parking.index.ParkingIndex;
import de.dfki.parking.index.ParkingIndexEntry;
import de.dfki.parking.knowledge.ParkingMap;
import de.dfki.parking.knowledge.ParkingMapEntry;
import de.dfki.parking.utility.ParkingParameters;
import de.dfki.parking.utility.ParkingPreferences;
import de.dfki.parking.utility.ParkingUtility;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MappingDisplayExplorationStrategy implements IExplorationStrategy {
  // Local knowledge of the entity
  private final ParkingMap localKnowledge;
  
  // Global knowledge of the system
  private final ParkingMap globalKnowledge;
  
  // ParkingIndex in case no 
  private final ParkingIndex parkingIndex;

  // Preferences to rank parking possibilities
  private final ParkingPreferences preferences;
 
  // Function for evaluating utility of parking spot
  private final ParkingUtility utility;
  
  // Timespan during which information from knowledge is considered relevant
  private long validTime;

 public MappingDisplayExplorationStrategy(ParkingMap localKnowledge, ParkingMap globalKnowledge, 
     ParkingPreferences prefs, ParkingUtility utility, ParkingIndex parkingIndex, long validTime) {
   this.localKnowledge = localKnowledge;
   this.globalKnowledge = globalKnowledge;
   this.preferences = prefs;
   this.utility = utility;
   this.parkingIndex = parkingIndex;
   this.validTime = validTime;
 }

 @Override
 public Coordinate findNextPossibleParking(Coordinate position, Coordinate destination, long currentTime) {
   // Find all parking possibilities in range from knowledge
   Collection<ParkingMapEntry> fromLocalKnowledge = localKnowledge.findParkingNearby(position, 500);
   Collection<ParkingMapEntry> fromGlobalKnowledge = globalKnowledge.findParkingNearby(position, 500);
   List<ParkingMapEntry> fromKnowledge = mergeByTime(fromLocalKnowledge, fromGlobalKnowledge);
   
   // See if recent relevant entries in knowledge exist and return them ranked by utility
   List<ParkingIndexEntry> relevant = getRelevantEntriesFromKnowlede(fromKnowledge, position, destination, currentTime);
   
   if (relevant.size() > 0) {
     // If there is an entry, return position
     ParkingIndexEntry entry = relevant.get(0);
     
     // Remove current position from list of possible positions
     List<Coordinate> temp = new ObjectArrayList<>(entry.getAllAccessPositions());
     while (temp.remove(position)) {}

     // Sample random position to reach
     Coordinate ret = temp.get(ThreadLocalRandom.current().nextInt(temp.size()));
     return ret;
   }   
   List<ParkingIndexEntry> fromMap = getPossibleParkingFromIndex(fromKnowledge, position, destination, currentTime);
   
   if (fromMap.size() > 0) {
     // If there is an entry, return position
     ParkingIndexEntry entry = fromMap.get(0);
     
     // Remove current position from list of possible positions
     List<Coordinate> temp = new ObjectArrayList<>(entry.getAllAccessPositions());
     while (temp.remove(position)) {}

     // Sample random position to reach
     Coordinate ret = temp.get(ThreadLocalRandom.current().nextInt(temp.size()));
     return ret;
   }
  return null;
 }
 
 private List<ParkingMapEntry> mergeByTime(Collection<ParkingMapEntry> local, Collection<ParkingMapEntry> global) {
   // Create a map to merge entries 
   List<ParkingMapEntry> merged = new ObjectArrayList<>(local.size() + global.size());
   merged.addAll(local);
   merged.addAll(global);
   merged.sort((e1, e2) -> Long.compare(e2.getLastUpdate(), e1.getLastUpdate()));
   
   List<ParkingMapEntry> ret = new ObjectArrayList<>();
   IntSet added = new IntOpenHashSet();
   
   for (ParkingMapEntry entry : merged) {
     
     if (added.contains(entry.getParkingIndexEntry().getParking().getId()))
       continue;
     ret.add(entry);
     added.add(entry.getParkingIndexEntry().getParking().getId());
   }
   return ret;
 }
 
 private List<ParkingIndexEntry> getRelevantEntriesFromKnowlede(List<ParkingMapEntry> fromKnowledge, 
     Coordinate position, Coordinate destination, long currentTime) {
   List<ParkingMapEntry> filtered = new ObjectArrayList<>(fromKnowledge.size());
   
   for (ParkingMapEntry entry : fromKnowledge) {
     // Filter by free parking spots
     if (((currentTime - entry.getLastUpdate()) / 1000.0 <= validTime) && entry.getNFreeParkingSpots() == 0)
       continue;
     
     if (entry.getParkingIndexEntry().getParking().getNumberOfParkingSpots() == 0)
       continue;
     
     filtered.add(entry);
   }
   return rankFromKnowledge(filtered, position, destination);
 }
 
 private List<ParkingIndexEntry> rankFromKnowledge(List<ParkingMapEntry> parkings, Coordinate currentPosition, Coordinate destination) {
   List<Pair<ParkingMapEntry, Double>> temp = new ObjectArrayList<>();
   
   for (ParkingMapEntry parking : parkings) {
     double c = parking.getParkingIndexEntry().getParking().getCurrentPricePerHour();
     double wd = Geometry.haversineDistance(parking.getParkingIndexEntry().getReferencePosition(), destination);
     double st = (Geometry.haversineDistance(parking.getParkingIndexEntry().getReferencePosition(), currentPosition) / 3.0);
     temp.add(new Pair<>(parking, utility.computeUtility(new ParkingParameters(c, wd, st), preferences)));
   }
   temp.sort((t1, t2) -> Double.compare(t2.second, t1.second));
   
   if (temp.size() > 0 && temp.get(0).second == 0.0)
     return new ObjectArrayList<>(0);
   
   List<ParkingIndexEntry> ret = new ObjectArrayList<>(temp.size());
   
   for (Pair<ParkingMapEntry, Double> p : temp) {
     ret.add(p.first.getParkingIndexEntry());
   }
   return ret;
 }
 
 private List<ParkingIndexEntry> getPossibleParkingFromIndex(List<ParkingMapEntry> fromKnowledge, Coordinate position, Coordinate destination, long currentTime) {
   // Filter those which are valid and which have free parking spots
   Collection<ParkingIndexEntry> fromIndex = parkingIndex.getParkingsWithMaxDistance(position, 200);
   // Collection<ParkingIndexEntry> fromIndex = parkingIndex.getAllGarageParkingEntries();
   IntSet knowledgeIds = new IntOpenHashSet();
   
   for (ParkingMapEntry entry : fromKnowledge) {
     
     if (((currentTime - entry.getLastUpdate()) / 1000.0 <= validTime)) {
       knowledgeIds.add(entry.getParkingIndexEntry().getParking().getId());
       continue;
     }
     
     if (entry.getParkingIndexEntry().getParking().getNumberOfParkingSpots() == 0) {
       knowledgeIds.add(entry.getParkingIndexEntry().getParking().getId());
       continue;
     }
   }   
   List<ParkingIndexEntry> ret = new ObjectArrayList<>();
   
   for (ParkingIndexEntry entry : fromIndex) {
     
     if (knowledgeIds.contains(entry.getParking().getId()))
       continue;
     ret.add(entry);
   }
   return rankFromIndex(ret, position, destination);
 }
 
 private List<ParkingIndexEntry> rankFromIndex(List<ParkingIndexEntry> parkings, Coordinate currentPosition, Coordinate destination) {
   List<Pair<ParkingIndexEntry, Double>> temp = new ObjectArrayList<>();

   for (ParkingIndexEntry parking : parkings) {
     double c = preferences.getCMax();
     double wd = Geometry.haversineDistance(parking.getReferencePosition(), destination);
     double st = (Geometry.haversineDistance(parking.getReferencePosition(), currentPosition) / 3.0);
     temp.add(new Pair<>(parking, utility.computeUtility(new ParkingParameters(c, wd, st), preferences)));
   }
   temp.sort((t1, t2) -> Double.compare(t2.second, t1.second));

   if (temp.size() > 0 && temp.get(0).second == 0.0)
     return new ObjectArrayList<>(0);
   
   List<ParkingIndexEntry> ret = new ObjectArrayList<>(temp.size());

   for (Pair<ParkingIndexEntry, Double> p : temp) {
     ret.add(p.first);
   }
   return ret;
 }
}
