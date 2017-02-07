package dk.sdu.compbio.netgale.cynetgale.internal;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import java.util.List;

public class AlignTask extends AbstractTask {
    private final List<CyNetwork> networks;
    private final Parameters params;

    public AlignTask(List<CyNetwork> networks, Parameters params) {
        this.networks = networks;
        this.params = params;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Aligning.");

        double progress = 0.0;

        while(progress < 1.0) {
            try {
                Thread.sleep(200);
            } catch(InterruptedException ie) { }

            progress = Math.min(progress + 0.1, 1.0);
            taskMonitor.setProgress(progress);
        }
    }
}
