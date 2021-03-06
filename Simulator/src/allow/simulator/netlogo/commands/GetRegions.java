package allow.simulator.netlogo.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.Syntax;

import allow.simulator.core.Simulator;
import allow.simulator.netlogo.agent.NetLogoWrapper;
import allow.simulator.util.Coordinate;
import allow.simulator.world.StreetMap;
import allow.simulator.world.Transformation;
import allow.simulator.world.overlay.Area;
import allow.simulator.world.overlay.DistrictOverlay;
import allow.simulator.world.overlay.DistrictType;

public class GetRegions extends DefaultReporter
{
	@Override
	public Object report(Argument[] args, Context context) throws ExtensionException, LogoException {
		LogoListBuilder bldr = new LogoListBuilder();
		StreetMap map = (StreetMap) Simulator.Instance().getContext().getWorld();
		
		if (map == null) 
			throw new ExtensionException("Error: Simulator is not initialized.");
		
		DistrictOverlay l = (DistrictOverlay) map.getOverlay(Simulator.OVERLAY_DISTRICTS);
		Set<String> areas = new HashSet<String>();
		HashMap<String, List<String>> types = new HashMap<String, List<String>>();
		HashMap<String, Coordinate> centers = new HashMap<String, Coordinate>();

		for (DistrictType type : DistrictType.values()) {
			List<Area> dAreas = l.getAreasOfType(type);
			
			for (Area a : dAreas) {
				
				if (!areas.contains(a.getName())) {
					areas.add(a.getName());
					types.put(a.getName(), new ArrayList<String>());
					centers.put(a.getName(), a.getCenter());
				}
				List<String> temp = types.get(a.getName());
				temp.add(type.toString());
			}
		}
		Transformation t = NetLogoWrapper.Instance().getTransformation();
		
		for (String area : areas) {
			if (area.equals("default")) {
				continue;
			}
			LogoListBuilder bldr2 = new LogoListBuilder();
			bldr2.add(area);
			System.out.println(centers.get(area));
			Coordinate center = t.transform(centers.get(area));
			bldr2.add(center.x);
			bldr2.add(center.y);
			
			LogoListBuilder bldr3 = new LogoListBuilder();
			
			for (String type : types.get(area)) {
				bldr3.add(type);
			}
			bldr2.add(bldr3.toLogoList());
			bldr.add(bldr2.toLogoList());
		}
		return bldr.toLogoList();
	}
	
	public Syntax getSyntax() {
		return Syntax.reporterSyntax(Syntax.ListType());
	}
}
