package allow.simulator.netlogo.agent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nlogo.agent.Link;
import org.nlogo.agent.Turtle;
import org.nlogo.agent.World;
import org.nlogo.api.AgentException;

import allow.simulator.core.EntityManager;
import allow.simulator.entity.Entity;
import allow.simulator.entity.EntityTypes;
import allow.simulator.util.Coordinate;
import allow.simulator.world.Street;
import allow.simulator.world.StreetMap;
import allow.simulator.world.StreetNode;
import allow.simulator.world.StreetSegment;
import allow.simulator.world.Transformation;
import de.dfki.simulation.AbstractSimulationModel;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class NetLogoSimulationModelWrapper implements ISimulationModelWrapper {
	
	public static final double LINK_COLOR_DEFAULT = 5.0;
	public static final double LINK_COLOR_BLOCKED = 15.0;
	public static final double LINK_COLOR_BUSY = 15.0;

	// NetLogo world instance
	private final World netLogoWorld;
	
	// Mapping of node labels to 
	private final Map<String, Link> linkMapping;
	
	// Transformation to convert coordinates
	private Transformation transformation;
	
	// SimulationModel instance
	private AbstractSimulationModel simulationModel;
	
	public NetLogoSimulationModelWrapper(World netLogoWorld) {
		this.netLogoWorld = netLogoWorld;
		linkMapping = new Object2ObjectOpenHashMap<String, Link>();
	}

	public Transformation getTransformation() {
		return transformation;
	}
	
	public World getWorld() {
		return netLogoWorld;
	}
	
	public AbstractSimulationModel getSimulationModel() {
		return simulationModel;
	}
	
	public Map<String, Link> getLinkMapping() {
		return Collections.unmodifiableMap(linkMapping);
	}
	
	@Override
	public void wrap(AbstractSimulationModel simulationModel) {
	  // Assign model
	  this.simulationModel = simulationModel;
	  
		// Wrap world
		wrapWorld((StreetMap) simulationModel.getContext().getWorld());

		// Wrap entities
		try {
			wrapEntities(simulationModel.getContext().getEntityManager());
			
		} catch (AgentException e) {
			e.printStackTrace();
		}
	}

	private void wrapWorld(StreetMap world) {
		// Wrap world
		double worldEnvelope[] = new double[] { netLogoWorld.minPxcor(),
				netLogoWorld.maxPxcor(), netLogoWorld.minPycor(),
				netLogoWorld.maxPxcor() };

		// Get envelope of loaded world.
		double gisEnvelope[] = world.getDimensions();

		// Set transformation between NetLogo and loaded world.
		transformation = new Transformation(gisEnvelope, worldEnvelope);

		// Create NetLogo bindings for streets and nodes
		Collection<Street> streets = world.getStreets();
		Long2ObjectOpenHashMap<Turtle> nodes = new Long2ObjectOpenHashMap<Turtle>();

		for (Street street : streets) {		
			List<StreetSegment> segs = street.getSubSegments();
			double color = street.isBlocked() ? LINK_COLOR_BLOCKED : LINK_COLOR_DEFAULT;
			
			for (StreetSegment seg : segs) {
				StreetNode segStart = seg.getStartingNode();
				Turtle startNode = nodes.get(segStart.getId());
				
				if (startNode == null) {
					Coordinate pos = transformation.transform(seg.getStartingPoint());
					startNode = new Turtle(netLogoWorld, netLogoWorld.getBreed("NODES"), pos.x, pos.y);
					nodes.put(seg.getStartingNode().getId(), startNode);
					netLogoWorld.turtles().add(startNode);
					startNode.hidden(true);
				}
				StreetNode segEnd = seg.getEndingNode();
				Turtle endNode = nodes.get(segEnd.getId());
				
				if (endNode == null) {
					Coordinate pos = transformation.transform(seg.getStartingPoint());
					endNode = new Turtle(netLogoWorld, netLogoWorld.getBreed("NODES"), pos.x, pos.y);
					nodes.put(seg.getEndingNode().getId(), endNode);
					netLogoWorld.turtles().add(endNode);
					endNode.hidden(true);
				}
				// Pair<StreetNode, StreetNode> in = world.getIncidentNodes(seg);
				Link newLink = netLogoWorld.linkManager.createLink(startNode, endNode, netLogoWorld.links());
				netLogoWorld.links().add(newLink);
				linkMapping.put(segStart.getId() + "," + segEnd.getId(), newLink);
				newLink.colorDouble(color);
				newLink.lineThickness(0.05);
				newLink.hidden(false);
			}
		}
	}

	private void wrapEntities(EntityManager entityManager) throws AgentException {
	
		for (String type : entityManager.getEntityTypes()) {
			// Get all entities of certain type
			Collection<Entity> entities = entityManager.getEntitiesOfType(type);

			if ((entities == null) || (entities.size() == 0))
				continue;
			
			NetLogoAgent<?> newAgent = null;
			
			for (Entity entity : entities) {
				
				switch (type) {
				case EntityTypes.BUS:
				case EntityTypes.PERSON:
				case EntityTypes.TAXI:
				case EntityTypes.PUBLIC_TRANSPORT_AGENCY:
				case EntityTypes.FLEXIBUS_AGENCY:
				case EntityTypes.TAXI_AGENCY:
					newAgent = NetLogoAgent.createNetLogoAgent(this, entity);
					netLogoWorld.turtles().add(newAgent);
					break;

				default:
					break;
				}
			}
		}
	}
}