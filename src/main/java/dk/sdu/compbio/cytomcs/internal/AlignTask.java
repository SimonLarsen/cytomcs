package dk.sdu.compbio.cytomcs.internal;

import dk.sdu.compbio.faithmcs.Alignment;
import dk.sdu.compbio.faithmcs.alg.DirectedIteratedLocalSearch;
import dk.sdu.compbio.faithmcs.alg.IteratedLocalSearch;
import dk.sdu.compbio.faithmcs.alg.UndirectedIteratedLocalSearch;
import dk.sdu.compbio.faithmcs.network.DirectedNetwork;
import dk.sdu.compbio.faithmcs.network.Edge;
import dk.sdu.compbio.faithmcs.network.Node;
import dk.sdu.compbio.faithmcs.network.UndirectedNetwork;
import org.cytoscape.model.*;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.jgrapht.Graph;
import sun.nio.ch.Net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Task computing the maximum common edge subgraph using an iterative local search algorithm.
 */
public class AlignTask extends AbstractTask {
    private final List<CyNetwork> networks;
    private final CyNetworkFactory networkFactory;
    private final CyNetworkManager networkManager;
    private final Parameters params;
    private CyNetwork result;

    /**
     * Create new AlignTask instance.
     * @param networks List of networks to align.
     * @param networkFactory CyNetworkFactory instance of creating the result.
     * @param networkManager CyNetworkManager instance of adding the results to the network collection.
     * @param params Parameters for the instance.
     */
    public AlignTask(List<CyNetwork> networks, CyNetworkFactory networkFactory, CyNetworkManager networkManager, Parameters params) {
        this.networks = networks;
        this.networkFactory = networkFactory;
        this.networkManager = networkManager;
        this.params = params;

        this.result = null;
    }

    /**
     * Starts the task. The task will run for a fixed number of iterations specified in the Parameters object.
     * @param taskMonitor TaskMonitor instance used for showing task progress.
     * @throws Exception
     */
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        if(networks.size() < 2) {
            throw new RuntimeException("At least two networks needed for finding MCS.");
        }

        taskMonitor.setTitle("Aligning networks");
        taskMonitor.setStatusMessage("Preparing alignment");

        IteratedLocalSearch aligner;
        if(params.getDirected()) {
            List<DirectedNetwork> in_networks = new ArrayList<>();
            for(CyNetwork network : networks) {
                DirectedNetwork out = new DirectedNetwork();
                cyNetworkToNetwork(out, network);
                in_networks.add(out);
            }
            aligner = new DirectedIteratedLocalSearch(in_networks, params.getPerturbation());
        } else {
            List<UndirectedNetwork> in_networks = new ArrayList<>();
            for(CyNetwork network : networks) {
                UndirectedNetwork out = new UndirectedNetwork();
                cyNetworkToNetwork(out, network);
                in_networks.add(out);
            }
            aligner = new UndirectedIteratedLocalSearch(in_networks, params.getPerturbation());
        }

        int iteration = 1;
        int nonimproving = 0;
        while(nonimproving < params.getMaxNonimprovingIterations() && !cancelled) {
            iteration++;
            nonimproving++;
            if(aligner.step()) {
                nonimproving = 0;
            }
            taskMonitor.setStatusMessage(String.format("Iteration: %d. Conserved edges: %d.", iteration, aligner.getBestNumberOfEdges()));
        }

        taskMonitor.setStatusMessage("Finalizing");
        taskMonitor.setProgress(1.0);

        Alignment alignment = aligner.getAlignment();
        result = alignmentToCyNetwork(alignment);
        result.getRow(result).set(CyNetwork.NAME, "Aligned network");
        networkManager.addNetwork(result);
    }

    /**
     * Returns the computed MCS.
     * @return The computed subgraph. Returns null if called before the task has been run.
     */
    public CyNetwork getResult() {
        return result;
    }

    private void cyNetworkToNetwork(Graph<Node,Edge> out, CyNetwork network) {
        Map<Long,Node> nodeMap = new HashMap<>();
        for(CyNode cynode : network.getNodeList()) {
            Node node = new Node(Long.toString(cynode.getSUID()));
            nodeMap.put(cynode.getSUID(), node);
            out.addVertex(node);
        }

        for(CyEdge cyedge : network.getEdgeList()) {
            Node source = nodeMap.get(cyedge.getSource().getSUID());
            Node target = nodeMap.get(cyedge.getTarget().getSUID());
            String name = network.getRow(cyedge).get(CyNetwork.NAME, String.class);
            Edge edge = new Edge(source, target, name);
            out.addEdge(source, target, edge);
        }
    }

    private CyNetwork alignmentToCyNetwork(Alignment alignment) {
        Graph<Node,Edge> network = alignment.buildNetwork(params.getExceptions(), params.getRemoveLeafExceptions());
        List<List<Node>> nodes = alignment.getAlignment();
        CyNetwork out = networkFactory.createNetwork();

        CyTable nodeTable = out.getDefaultNodeTable();
        CyTable edgeTable = out.getDefaultEdgeTable();

        // Add network columns to node table
        HashMap<String,Integer> collisionMap = new HashMap<>();
        List<String> networkColumns = new ArrayList<>();
        for(CyNetwork n : networks) {
            String name = n.getRow(n).get(CyNetwork.NAME, String.class);
            if(collisionMap.containsKey(name)) {
                int count = collisionMap.get(name);
                collisionMap.put(name, count+1);
                name += "_" + count;
            } else {
                collisionMap.put(name, 1);
            }
            nodeTable.createColumn(name, String.class, true);
            networkColumns.add(name);
        }

        // Create exceptions column
        edgeTable.createColumn("exceptions", Integer.class, false);

        // Create nodes
        Map<String,CyNode> nodeMap = new HashMap<>();
        for(Node node : network.vertexSet()) {
            if(network.edgesOf(node).size() == 0) continue;
            CyNode cynode = out.addNode();
            nodeMap.put(node.getLabel(), cynode);
            String label = getAlignmentNodeLabel(nodes, node.getPosition());
            CyRow row = out.getRow(cynode);
            row.set(CyNetwork.NAME, label);

            for(int i = 0; i < networks.size(); ++i) {
                if(nodes.get(i).get(node.getPosition()).isFake()) continue;
                Long suid = Long.parseLong(nodes.get(i).get(node.getPosition()).getLabel());
                CyNode from_cynode = networks.get(i).getNode(suid);
                String cylabel = networks.get(i).getRow(from_cynode).get(CyNetwork.NAME, String.class);
                row.set(networkColumns.get(i), cylabel);
            }
        }

        // Create edges
        for(Edge edge : network.edgeSet()) {
            CyNode cysource = nodeMap.get(edge.getSource().getLabel());
            CyNode cytarget = nodeMap.get(edge.getTarget().getLabel());

            if(cysource == null || cytarget == null) {
                throw new RuntimeException("Edge connected to missing node.");
            }

            CyEdge e = out.addEdge(cysource, cytarget, false);
            int exceptions = networks.size() - edge.getConservation();
            out.getRow(e).set(CyNetwork.NAME, edge.getLabel());
            out.getRow(e).set("exceptions", exceptions);
        }

        return out;
    }

    private String getAlignmentNodeLabel(List<List<Node>> nodes, int pos) {
        List<String> names = new ArrayList<>();
        for(int i = 0; i < networks.size(); ++i) {
            if(nodes.get(i).get(pos).isFake()) continue;
            Long suid = Long.parseLong(nodes.get(i).get(pos).getLabel());
            CyNode node = networks.get(i).getNode(suid);
            names.add(networks.get(i).getRow(node).get(CyNetwork.NAME, String.class));
        }
        return String.join(",", names);
    }
}
