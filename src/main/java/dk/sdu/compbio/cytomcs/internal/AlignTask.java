package dk.sdu.compbio.cytomcs.internal;

import dk.sdu.compbio.faithmcs.Model;
import dk.sdu.compbio.faithmcs.alg.Aligner;
import dk.sdu.compbio.faithmcs.alg.IteratedLocalSearch;
import dk.sdu.compbio.faithmcs.network.Edge;
import dk.sdu.compbio.faithmcs.network.Network;
import dk.sdu.compbio.faithmcs.network.Node;
import org.cytoscape.model.*;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlignTask extends AbstractTask {
    private final List<CyNetwork> networks;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final Parameters params;

    public AlignTask(List<CyNetwork> networks, CyNetworkFactory networkFactory, CyNetworkManager networkManager, Parameters params) {
        this.networks = networks;
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.params = params;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Aligning networks");
        taskMonitor.setStatusMessage("Preparing alignment");
        taskMonitor.setProgress(0.0);

        System.err.println("Selected networks:");
        networks.stream().map(CyNetwork::toString).collect(Collectors.joining(", "));
        System.err.println("Converting CyNetworks");
        List<Network> in_networks = networks.stream().map(this::cyNetworkToNetwork).collect(Collectors.toList());
        Aligner aligner = new IteratedLocalSearch(in_networks, new Model(), params.getPerturbation());

        for(int iteration = 0; iteration < params.getIterations() && !cancelled; ++iteration) {
            System.err.println("iteration: " + iteration+1);
            taskMonitor.setStatusMessage(String.format("Iteration: %d. Conserved edges: %d.", iteration+1, aligner.getCurrentNumberOfEdges()));
            aligner.step();
            taskMonitor.setProgress((float)iteration / params.getIterations());
        }

        taskMonitor.setStatusMessage("Finalizing");
        taskMonitor.setProgress(1.0);

        CyNetwork out = networkToCyNetwork(aligner.getAlignment().buildNetwork(0, params.getConnected()));
        out.getRow(out).set(CyNetwork.NAME, "Aligned network");
        networkManager.addNetwork(out);
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

    private CyNetwork networkToCyNetwork(Network network) {
        CyNetwork out = networkFactory.createNetwork();

        Map<String,CyNode> nodeMap = new HashMap<>();

        for(Edge edge : network.edgeSet()) {
            Node source = edge.getSource();
            Node target = edge.getTarget();

            CyNode cysource = nodeMap.get(edge.getSource().getLabel());
            CyNode cytarget = nodeMap.get(edge.getTarget().getLabel());

            if(cysource == null) {
                cysource = out.addNode();
                nodeMap.put(source.getLabel(), cysource);
                out.getRow(cysource).set(CyNetwork.NAME, source.getLabel());
            }
            if(cytarget == null) {
                cytarget = out.addNode();
                nodeMap.put(target.getLabel(), cytarget);
                out.getRow(cytarget).set(CyNetwork.NAME, target.getLabel());
            }

            out.addEdge(cysource, cytarget, false);
        }

        return out;
    }
}