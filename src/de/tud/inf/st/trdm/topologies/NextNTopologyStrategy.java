package de.tud.inf.st.trdm.topologies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import de.tud.inf.st.trdm.Link;
import de.tud.inf.st.trdm.Mirror;
import de.tud.inf.st.trdm.Network;
import de.tud.inf.st.trdm.util.IDGenerator;

/**A topology connecting each mirror with its next N mirrors.
 * N can be set using the <i>numLinks</i> configuration parameter.
 *
 * @author Sebastian Götz (sebastian.goetz@acm.org)
 */
public class NextNTopologyStrategy implements TopologyStrategy {

	@Override
	public Set<Link> initNetwork(Network n, Properties props) {
		int numMirrors = n.getMirrors().size();
		int numLinks = n.getNumTargetLinksPerMirror();
		Set<Link> ret = new HashSet<>();

		for (int i = 0; i < numMirrors; i++) {
			for (int j = 1; j <= numLinks; j++) {
				// get a random mirror
				Mirror source = n.getMirrors().get(i);
				if (i + j >= n.getMirrors().size())
					continue;
				Mirror target = n.getMirrors().get(i + j);
				ret.add(new Link(IDGenerator.getInstance().getNextID(), source, target, 0, props));
			}
		}
		return ret;
	}

	@Override
	public void restartNetwork(Network n, Properties props) {
		//close all existing links
		for(Link l : n.getLinks()) {
			l.shutdown();
		}
		//establish new links
		n.getLinks().addAll(initNetwork(n, props));
	}

	@Override
	public void handleAddNewMirrors(Network n, int newMirrors, Properties props, int simTime) {
		List<Mirror> mirrors = n.getMirrors();
		int numTargetLinksPerMirror = n.getNumTargetLinksPerMirror();
		
		//first add the mirrors
		List<Mirror> mirrorsToAdd = new ArrayList<>();
		for (int i = 0; i < newMirrors; i++) {
			mirrorsToAdd.add(new Mirror(IDGenerator.getInstance().getNextID(), simTime, props));
		}
		// get last N mirrors to connect to the new mirrors (numTargetedLinksPerMirror)
		List<Mirror> lastMirrors = new ArrayList<>();
		for (int i = numTargetLinksPerMirror; i > 0; i--) {
			lastMirrors.add(mirrors.get(mirrors.size() - i));
		}
		mirrors.addAll(mirrorsToAdd);
		// add links from old mirrors
		for (int i = 0; i < lastMirrors.size(); i++) {
			Mirror source = lastMirrors.get(i);
			for (int j = 1; j <= numTargetLinksPerMirror; j++) {
				Mirror target;
				if (i + j < lastMirrors.size()) {
					target = lastMirrors.get(i + j);
				} else {
					target = mirrorsToAdd.get(i + j - lastMirrors.size());
				}
				Link l = new Link(IDGenerator.getInstance().getNextID(), source, target, simTime, props);
				n.getLinks().add(l);
			}
		}

		// add links for new mirrors
		for (int i = 0; i < mirrorsToAdd.size(); i++) {
			for (int j = 1; j <= numTargetLinksPerMirror; j++) {
				if (i + j < mirrorsToAdd.size())
					n.getLinks().add(new Link(IDGenerator.getInstance().getNextID(), mirrorsToAdd.get(i),
							mirrorsToAdd.get(i + j), simTime, props));
			}
		}

	}

	@Override
	public void handleRemoveMirrors(Network n, int removeMirrors, Properties props, int simTime) {
		List<Mirror> mirrors = n.getMirrors();
		
		for (int i = 0; i < removeMirrors; i++) {
			Mirror m = mirrors.get(i);
			m.shutdown(simTime);
			for (Link l : m.getLinks()) {
				l.shutdown();
			}
		}
		// note: the number of closed links can vary due to shared links between
	}
	
	@Override
	public int getNumTargetLinks(Network n) {
		int ret = n.getNumTargetMirrors() * n.getNumTargetLinksPerMirror();
		for(int i = 1; i <= n.getNumTargetLinksPerMirror(); i++)
			ret -= i;
		return ret;
	}

}
