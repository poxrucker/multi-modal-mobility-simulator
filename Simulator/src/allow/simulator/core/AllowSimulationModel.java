package allow.simulator.core;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import allow.simulator.core.Configuration.KnowledgeConfiguration;
import allow.simulator.core.Configuration.ServiceConfiguration;
import allow.simulator.entity.Entity;
import allow.simulator.entity.EntityTypes;
import allow.simulator.entity.Person;
import allow.simulator.entity.PlanGenerator;
import allow.simulator.knowledge.EvoKnowledge;
import allow.simulator.knowledge.Experience;
import allow.simulator.knowledge.IExchangeStrategy;
import allow.simulator.mobility.data.IDataService;
import allow.simulator.mobility.data.OfflineDataService;
import allow.simulator.mobility.data.OnlineDataService;
import allow.simulator.mobility.data.RoutingData;
import allow.simulator.mobility.data.TransportationRepository;
import allow.simulator.mobility.data.gtfs.GTFSData;
import allow.simulator.mobility.planner.BikeRentalPlanner;
import allow.simulator.mobility.planner.JourneyPlanner;
import allow.simulator.mobility.planner.OTPPlanner;
import allow.simulator.mobility.planner.TaxiPlanner;
import allow.simulator.statistics.Statistics;
import allow.simulator.util.Coordinate;
import allow.simulator.world.StreetMap;
import allow.simulator.world.Weather;
import allow.simulator.world.overlay.DistrictOverlay;
import allow.simulator.world.overlay.RasterOverlay;
import de.dfki.crf.CRFKnowledgeFactory;
import de.dfki.crf.IKnowledgeModel;
import de.dfki.simulator.SimulationModel;

/**
 * Main class of simulator for collaborative learning in the scenario of
 * personalized, multi-modal urban mobility.
 * 
 * @author Andreas Poxrucker (DFKI)
 *
 */
public final class AllowSimulationModel extends SimulationModel {
	// Simulation context
	private Context context;

	// Threadpool for executing multiple tasks in parallel
	private ExecutorService threadpool;
	
	public static final String PARAM_CONFIG = "configuration";
	public static final String PARAM_SIMULATION_PARAMETER = "simulation_parameter";
	
	public static final String OVERLAY_DISTRICTS = "partitioning";
	public static final String OVERLAY_RASTER = "raster";

	public void setup(Map<String, Object> configuration) {
	  // Initialize threadpool
		threadpool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 8);

		// Get parameters from configuration
		Configuration config = (Configuration) configuration.get(PARAM_CONFIG);
		SimulationParameter params = (SimulationParameter) configuration.get(PARAM_SIMULATION_PARAMETER);
		
		// Setup world.
		System.out.println("Loading world...");
		StreetMap world;
		
    try {
      world = new StreetMap(config.getMapPath());
      
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
		
		System.out.println("  Adding layer \"" + OVERLAY_DISTRICTS + "\"...");
		Path l = config.getLayerPath(OVERLAY_DISTRICTS);
		if (l == null)
			throw new IllegalStateException("Error: Missing layer with key \"" + OVERLAY_DISTRICTS + "\".");
		DistrictOverlay districtOverlay = null;
		
    try {
      districtOverlay = DistrictOverlay.parse(l, world);
      
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
		world.setDistricts(districtOverlay);
		
		RasterOverlay rasterOverlay = new RasterOverlay(world.getDimensions(), params.GridResX, params.GridResY);
    world.setRaster(rasterOverlay);

		// Create data services.
		System.out.println("Creating data services...");
		List<IDataService> dataServices = new ArrayList<IDataService>();
		List<ServiceConfiguration> dataConfigs = config.getDataServiceConfiguration();
		
		for (ServiceConfiguration dataConfig : dataConfigs) {

			if (dataConfig.isOnline()) {
				// For online queries create online data services. 
				dataServices.add(new OnlineDataService(dataConfig.getURL(), dataConfig.getPort()));
		
			} else {
				// For offline queries create mobility repository and offline service.
				GTFSData gtfs = null;
				RoutingData routing = null;
				
        try {
          gtfs = GTFSData.parse(Paths.get(dataConfig.getURL()));
          routing = RoutingData.parse(Paths.get(dataConfig.getURL()), world, gtfs);
          
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }
				dataServices.add(new OfflineDataService(gtfs, routing));
			}
		}
		
		// Create time and weather.
		Time time = new Time(config.getStartingDate(), 5);
				
		// Create planner services.
		System.out.println("Creating planner services...");
		List<OTPPlanner> plannerServices = new ArrayList<OTPPlanner>();
		List<ServiceConfiguration> plannerConfigs = config.getPlannerServiceConfiguration();
		
		for (int i = 0; i < plannerConfigs.size(); i++) {
			ServiceConfiguration plannerConfig = plannerConfigs.get(i);
			plannerServices.add(new OTPPlanner(plannerConfig.getURL(), plannerConfig.getPort(), world, dataServices.get(0), time));
		}		
		
		// Create taxi planner service
		Coordinate taxiRank = new Coordinate(11.1198448, 46.0719489);
		TaxiPlanner taxiPlannerService = new TaxiPlanner(plannerServices, taxiRank);
		
		// Create bike rental service
		Coordinate bikeRentalStation = new Coordinate(11.1248895,46.0711398);
		BikeRentalPlanner bikeRentalPlanner = new BikeRentalPlanner(plannerServices, bikeRentalStation);
		System.out.println("Loading weather model...");
		Weather weather = null;
		
    try {
      weather = new Weather(config.getWeatherPath(), time);
      
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
		
		JourneyPlanner planner = new JourneyPlanner(plannerServices, taxiPlannerService,
				bikeRentalPlanner, threadpool);
		
		// Create global context from world, time, planner and data services, and weather.
		context = new Context(world, new EntityManager(), time, planner, 
				dataServices.get(0), weather, new Statistics(400), params);
		
		// Setup entities.
		System.out.println("Loading entities from file...");
		
		try {
      loadEntitiesFromFile(config.getAgentConfigurationPath());
      
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
		
		// Create public transportation.
		System.out.println("Creating public transportation system...");
		TransportationRepository repos = new TransportationRepository(context);
		context.setTransportationRepository(repos);
		
		// Initialize EvoKnowlegde
		String prefix = "evo_" + params.BehaviourSpaceRunNumber + "_tbl_";
		
		try {
      initializeKnowledge(config.getEvoKnowledgeConfiguration(), prefix, params.KnowledgeModel);
      
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return;
    }
		
		// Update world
		world.update(context);
	}
	
	private void loadEntitiesFromFile(Path config) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		
		List<String> lines = Files.readAllLines(config, Charset.defaultCharset());
		
		for (String line : lines) {
			Person p = mapper.readValue(line, Person.class);
			p.setContext(context);
			PlanGenerator.generateDayPlan(p);
			context.getEntityManager().addEntity(p);
		}
	}
	
	private void initializeKnowledge(KnowledgeConfiguration dbConfig, String prefix, String knowledgeModel) throws ClassNotFoundException {
	  EvoKnowledge.initialize(threadpool);
	  CRFKnowledgeFactory factory = CRFKnowledgeFactory.create(dbConfig.getModelPath(), dbConfig.getModelName(),
	      dbConfig.getUser(), dbConfig.getPassword(), prefix);
	  
	  Collection<Entity> persons = context.getEntityManager().getEntitiesOfType("Person");
	  
	  for (Entity e : persons) {
	    Person p = (Person)e;
	    IKnowledgeModel<Experience> knowledge = null;
	    IExchangeStrategy<IKnowledgeModel<Experience>> exchangeStrategy = null;
	    
	    switch (knowledgeModel) {
	    
	    case "without":
	      knowledge = factory.createIdentityCRFModel();
	      break;
	      
	    case "local":
	      knowledge = factory.createLocalCRFModel(String.valueOf(p.getId()));
        break;
        
	    case "local (with exchange)":
        knowledge = factory.createLocalCRFModel(String.valueOf(p.getId()));
        exchangeStrategy = factory.createLocalCRFStrategy();
        break;
        
	    case "global":
        knowledge = factory.createGlobalCRFModel("global");
        break;
        
	    case "regional":
        knowledge = factory.createGlobalCRFModel(p.getHomeArea());
        break;
        
      default:
        throw new IllegalArgumentException("Unknown knowledge model");
	    }
	    p.setKnowledge(new EvoKnowledge(knowledge, exchangeStrategy));
	  }
	}
	/**
	 * Advances the simulation by one step.
	 * 
	 * @param deltaT Time interval for this step.
	 */
	public void step() {
		// Save current day to trigger routine scheduling
		int days = context.getTime().getDays();
		
		// Update time
		context.getTime().tick();
		
		// Update world
		context.getWorld().update(context);
		
		// Trigger routine scheduling.
		if (days != context.getTime().getDays()) {
			Collection<Entity> persons = context.getEntityManager().getEntitiesOfType(EntityTypes.PERSON);

			for (Entity p : persons) {
				PlanGenerator.generateDayPlan((Person) p);
				p.getRelations().resetBlackList();
			}
		}
		
		if (context.getTime().getCurrentTime().getHour() == 3
				&& context.getTime().getCurrentTime().getMinute() == 0
				&& context.getTime().getCurrentTime().getSecond() == 0) {
			context.getStatistics().reset();
		}
		
		//context.getWorld().getStreetMap().getNBusiestStreets(20);
		//EvoKnowledge.invokeRequest();
		//EvoKnowledge.cleanModel();
	}
	
	/**
	 * Returns the context associated with this Simulator instance.
	 * 
	 * @return Context of this Simulator instance
	 */
	public Context getContext() {
		return context;
	}
	
	public void finish() {
		threadpool.shutdown();

		try {
			threadpool.awaitTermination(10, TimeUnit.SECONDS);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}