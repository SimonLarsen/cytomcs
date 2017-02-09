package dk.sdu.compbio.netgale.cynetgale.internal;

import net.miginfocom.swing.MigLayout;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;

public class ControlPanel implements CytoPanelComponent, NetworkAddedListener, NetworkAboutToBeDestroyedListener {
    private static final int DEFAULT_ITERATIONS = 20;
    private static final int DEFAULT_PERTURBATION = 10;

    private DefaultListModel<CyNetwork> availableListModel;
    private DefaultListModel<CyNetwork> selectedListModel;

    private JPanel rootPanel, paramsPanel, networksPanel, runPanel;

    private JSpinner iterationsSpinner;
    private JLabel perturbationLabel;
    private JSlider perturbationSlider;
    private JCheckBox connectedCheckbox;
    private JList<CyNetwork> availableList;
    private JList<CyNetwork> selectedList;
    private JButton includeButton;
    private JButton excludeButton;
    private JButton alignButton;

    public ControlPanel(
            CyNetworkManager networkManager,
            CyNetworkFactory networkFactory,
            TaskManager taskManager
    ) {
        availableListModel = new DefaultListModel<>();
        selectedListModel = new DefaultListModel<>();
        for (CyNetwork network : networkManager.getNetworkSet()) {
            availableListModel.addElement(network);
        }

        createGUI();

        alignButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                java.util.List<CyNetwork> networks = new ArrayList<>();
                Enumeration<CyNetwork> networks_enum = selectedListModel.elements();
                while (networks_enum.hasMoreElements()) networks.add(networks_enum.nextElement());
                Parameters params = getParameters();

                taskManager.execute(new TaskIterator(new AlignTask(networks, networkFactory, networkManager, params)));
            }
        });
        includeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                java.util.List<CyNetwork> selected = availableList.getSelectedValuesList();
                for (CyNetwork network : selected) {
                    availableListModel.removeElement(network);
                    selectedListModel.addElement(network);
                }
            }
        });
        excludeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                java.util.List<CyNetwork> selected = selectedList.getSelectedValuesList();
                for (CyNetwork network : selected) {
                    selectedListModel.removeElement(network);
                    availableListModel.addElement(network);
                }
            }
        });
        perturbationSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                perturbationLabel.setText(Integer.toString(perturbationSlider.getValue()) + "%");
            }
        });
    }

    private Parameters getParameters() {
        try {
            iterationsSpinner.commitEdit();
        } catch(ParseException pe) { }
        int iterations = (Integer)iterationsSpinner.getValue();
        float perturbation = perturbationSlider.getValue() / 100f;
        boolean connected = connectedCheckbox.isSelected();
        return new Parameters(iterations, perturbation, connected);
    }

    @Override
    public void handleEvent(NetworkAddedEvent e) {
        availableListModel.addElement(e.getNetwork());
    }

    @Override
    public void handleEvent(NetworkAboutToBeDestroyedEvent e) {
        availableListModel.removeElement(e.getNetwork());
        selectedListModel.removeElement(e.getNetwork());
    }

    @Override
    public Component getComponent() {
        return rootPanel;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.WEST;
    }

    @Override
    public String getTitle() {
        return "NetGALE";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    private void createGUI() {
        // params panel
        paramsPanel = new JPanel(new MigLayout("wrap 3"));
        paramsPanel.setBorder(new TitledBorder("Parameters"));
        iterationsSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_ITERATIONS, 1, 100, 1));
        perturbationLabel = new JLabel(Integer.toString(DEFAULT_PERTURBATION) + "%");
        perturbationSlider = new JSlider(0, 100, DEFAULT_PERTURBATION);
        connectedCheckbox = new JCheckBox("Extract largest connected component");
        paramsPanel.add(new JLabel("Iterations"));
        paramsPanel.add(iterationsSpinner, "span 2");
        paramsPanel.add(new JLabel("Perturbation"));
        paramsPanel.add(perturbationSlider);
        paramsPanel.add(perturbationLabel);
        paramsPanel.add(connectedCheckbox, "span 3");

        // networks panel
        availableList = new JList<>(availableListModel);
        selectedList = new JList<>(selectedListModel);
        includeButton = new JButton(">");
        excludeButton = new JButton("<");

        JPanel networkButtonsPanel = new JPanel(new GridLayout(2,1));
        networkButtonsPanel.add(includeButton);
        networkButtonsPanel.add(excludeButton);

        networksPanel = new JPanel(new GridLayout(1, 3));
        networksPanel.setBorder(new TitledBorder("Select networks to align"));
        networksPanel.add(new JScrollPane(availableList));
        networksPanel.add(networkButtonsPanel);
        networksPanel.add(new JScrollPane(selectedList));

        // run panel
        runPanel = new JPanel();
        runPanel.setBorder(new TitledBorder("Start alignment"));
        alignButton = new JButton("Align");
        runPanel.add(alignButton, BorderLayout.CENTER);

        // put together
        rootPanel = new JPanel(new MigLayout("wrap 1"));
        rootPanel.add(paramsPanel, "growx");
        rootPanel.add(networksPanel, "growx");
        rootPanel.add(runPanel, "growx");
    }
}
