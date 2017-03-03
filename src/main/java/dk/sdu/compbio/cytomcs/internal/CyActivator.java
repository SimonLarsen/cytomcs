package dk.sdu.compbio.cytomcs.internal;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;

import java.awt.event.ActionEvent;
import java.util.Properties;

public class CyActivator extends AbstractCyActivator {
    public CyActivator() {
        super();
    }

    public void start(BundleContext bc) {
        CyNetworkManager networkManager = getService(bc, CyNetworkManager.class);
        CyNetworkFactory networkFactory = getService(bc, CyNetworkFactory.class);
        TaskManager taskManager = getService(bc, TaskManager.class);

        ControlPanel controlPanel = new ControlPanel(networkManager, networkFactory, taskManager);

        AbstractCyAction loadControlPanelAction = new AbstractCyAction("Load CytoMCS") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                registerService(bc, controlPanel, CytoPanelComponent.class, new Properties());
            }
        };
        loadControlPanelAction.setPreferredMenu("Apps");
        registerService(bc, loadControlPanelAction, CyAction.class, new Properties());

        registerService(bc, controlPanel, NetworkAddedListener.class, new Properties());
        registerService(bc, controlPanel, NetworkAboutToBeDestroyedListener.class, new Properties());
    }
}