package dk.sdu.compbio.netgale.cynetgale.internal;

import dk.sdu.compbio.netgale.Model;
import dk.sdu.compbio.netgale.alg.Aligner;
import dk.sdu.compbio.netgale.alg.LocalSearch;
import dk.sdu.compbio.netgale.network.Edge;
import dk.sdu.compbio.netgale.network.Network;
import dk.sdu.compbio.netgale.network.Node;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlignTask extends AbstractTask {
    private final List<CyNetwork> networks;
    private final Parameters params;

    public AlignTask(List<CyNetwork> networks, Parameters params) {
        this.networks = networks;
        this.params = params;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Preparing alignment.");

        double progress = 0.0;

        List<Network> in_networks = networks.stream().map(n -> cyNetworkToNetwork(n)).collect(Collectors.toList());
        Aligner aligner = new LocalSearch(in_networks, new Model());

        for(int iteration = 0; iteration < params.getIterations(); ++iteration) {
            aligner.step();
            progress = (float)iteration / params.getIterations();
            taskMonitor.setProgress(progress);
        }
    }

    private Network cyNetworkToNetwork(CyNetwork network) {
        Network out = new Network();

        Map<Long,Node> nodeMap = new HashMap<>();
        for(CyNode cynode : network.getNodeList()) {
            Node node = new Node(network.getRow(cynode).get(CyNetwork.NAME, String.class));
            nodeMap.put(cynode.getSUID(), node);
            out.addVertex(node);
        }

        for(CyEdge cyedge : network.getEdgeList()) {
            Node source = nodeMap.get(cyedge.getSource().getSUID());
            Node target = nodeMap.get(cyedge.getTarget().getSUID());
            Edge edge = new Edge(source, target);
            out.addEdge(source, target, edge);
        }

        return out;
    }
}
