package de.dfki.parking.knowledge;

import java.util.Collection;
import java.util.List;

import allow.simulator.util.Coordinate;
import allow.simulator.world.Street;
import de.dfki.parking.model.Parking;
import de.dfki.parking.model.ParkingIndex;
import de.dfki.parking.model.ParkingIndex.ParkingIndexEntry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class ParkingKnowledge {
  
  public static final class ParkingKnowledgeEntry {
    // Referenced parking index entry
    private ParkingIndexEntry parkingIndexEntry;
   
    // Number of parking spots
    private int nParkingSpots;
    
    // Number of free parking spots as known at last update time
    private int nFreeParkingSpots;
    
    // Price per hour as known at last update time
    private double pricePerHour;
    
    // Last time this entry was updated
    private long lastUpdate;
    
    private ParkingKnowledgeEntry(ParkingIndexEntry parkingMapEntry,
        int nParkingSpots,
        int nFreeParkingSpots,
        double pricePerHour,
        long lastUpdate) {
      this.parkingIndexEntry = parkingMapEntry;
      this.nParkingSpots = nFreeParkingSpots;
      this.nFreeParkingSpots = nFreeParkingSpots;
      this.pricePerHour = pricePerHour;
      this.lastUpdate = lastUpdate;
    }
    
    /**
     * Returns the referenced ParkingIndexEntry.
     * 
     * @return Referenced ParkingIndexEntry
     */
    public ParkingIndexEntry getParkingIndexEntry() {
      return parkingIndexEntry;
    }
    
    /**
     * Returns the total number of parking spots.
     *  
     * @return Total number parking spots
     */
    public int getNParkingSpots() {
      return nParkingSpots;
    }
    
    /**
     * Returns the number of free parking spots as known at last update time.
     *  
     * @return Number of free parking spots
     */
    public int getNFreeParkingSpots() {
      return nFreeParkingSpots;
    }
    
    /**
     * Returns the price per hour as known at last update time.
     * 
     * @return Price per hour
     */
    public double getPricePerHour() {
      return pricePerHour;
    }
    
    /**
     * Returns the timestamp the entry was updated last.
     * 
     * @return Last update time
     */
    public long getLastUpdate() {
      return lastUpdate;
    }
    
    public void update(int nFreeParkingSpots, long ts) {
      this.nFreeParkingSpots = nFreeParkingSpots;
      this.lastUpdate = ts;
    }
  }

  private final ParkingIndex parkingIndex;
  private final Int2ObjectMap<ParkingKnowledgeEntry> parkingKnowledge;
  
  public ParkingKnowledge(ParkingIndex parkingMap) {
    this.parkingIndex = parkingMap;
    this.parkingKnowledge = new Int2ObjectOpenHashMap<>();
  }
  
  public void update(Parking parking, int nParkingSpots, int nFreeParkingSpots, double pricePerHour, long time) {
    // Check if entry is known
    ParkingKnowledgeEntry ret = parkingKnowledge.get(parking.getId());
    
    if (ret == null) {
      // Get ParkingMapEntry from ParkingMap
      ParkingIndexEntry entry = parkingIndex.getForParking(parking);
      ret = new ParkingKnowledgeEntry(entry, nParkingSpots, nFreeParkingSpots, pricePerHour, -1);
      parkingKnowledge.put(parking.getId(), ret);
    }
    ret.update(nFreeParkingSpots, time);
  }
  
  public List<ParkingKnowledgeEntry> findParkingInStreet(Street street) {
    // Get parking possibilities in street from ParkingMap
    Collection<ParkingIndexEntry> indexEntries = parkingIndex.getParkingsInStreet(street);
    
    if (indexEntries == null)
      return new ObjectArrayList<>(0);
    
    return filterUnknownFromIndex(indexEntries);
  }
  
  public List<ParkingKnowledgeEntry> findParkingNearby(Coordinate position, double maxDistance) {
    // Get nearby parking possibilities from ParkingMap
    Collection<ParkingIndexEntry> indexEntries = parkingIndex.getParkingsWithMaxDistance(position, maxDistance);
    
    if (indexEntries.size() == 0)
      return new ObjectArrayList<>(0);
    
    return filterUnknownFromIndex(indexEntries);
  } 
  
  private List<ParkingKnowledgeEntry> filterUnknownFromIndex(Collection<ParkingIndexEntry> indexEntries) {
    List<ParkingKnowledgeEntry> ret = new ObjectArrayList<>(indexEntries.size());
    
    for (ParkingIndexEntry entry : indexEntries) {
      ParkingKnowledgeEntry temp = parkingKnowledge.get(entry.getParking().getId());
      
      if (temp == null)
        continue;
      
      ret.add(temp);
    }
    return ret;
  }
}