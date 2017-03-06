package dk.sdu.compbio.cytomcs.internal;

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
import java.awt.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

public class ControlPanel implements CytoPanelComponent, NetworkAddedListener, NetworkAboutToBeDestroyedListener {
    private static final int DEFAULT_ITERATIONS = 20;
    private static final int DEFAULT_PERTURBATION = 10;

    private final DefaultListModel<CyNetwork> availableListModel;
    private final DefaultListModel<CyNetwork> selectedListModel;
    private final SpinnerNumberModel exceptionsSpinnerModel;

    private JPanel rootPanel;

    private JSpinner iterationsSpinner;
    private JLabel perturbationLabel;
    private JSlider perturbationSlider;
    private JSpinner exceptionsSpinner;
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

        exceptionsSpinnerModel = new SpinnerNumberModel(0, 0, 0, 1);

        createGUI();

        alignButton.addActionListener(actionEvent -> {
            java.util.List<CyNetwork> networks = new ArrayList<>();
            Enumeration<CyNetwork> networks_enum = selectedListModel.elements();
            while (networks_enum.hasMoreElements()) networks.add(networks_enum.nextElement());
            Parameters params = getParameters();

            taskManager.execute(new TaskIterator(new AlignTask(networks, networkFactory, networkManager, params)));
        });
        includeButton.addActionListener(actionEvent -> {
            java.util.List<CyNetwork> selected = availableList.getSelectedValuesList();
            for (CyNetwork network : selected) {
                availableListModel.removeElement(network);
                selectedListModel.addElement(network);
            }
            exceptionsSpinnerModel.setMaximum(numSelected());
        });
        excludeButton.addActionListener(actionEvent -> {
            java.util.List<CyNetwork> selected = selectedList.getSelectedValuesList();
            for (CyNetwork network : selected) {
                selectedListModel.removeElement(network);
                availableListModel.addElement(network);
            }
            exceptionsSpinnerModel.setMaximum(numSelected());
            if((Integer)exceptionsSpinner.getValue() > (Integer)exceptionsSpinnerModel.getMaximum()) {
                exceptionsSpinnerModel.setValue(exceptionsSpinnerModel.getMaximum());
            }
        });
        perturbationSlider.addChangeListener(changeEvent -> perturbationLabel.setText(Integer.toString(perturbationSlider.getValue()) + "%"));
    }

    private Parameters getParameters() {
        try {
            iterationsSpinner.commitEdit();
        } catch(ParseException pe) { }
        int iterations = (Integer)iterationsSpinner.getValue();
        float perturbation = perturbationSlider.getValue() / 100f;
        int exceptions = (Integer)exceptionsSpinner.getValue();
        boolean connected = connectedCheckbox.isSelected();
        return new Parameters(iterations, perturbation, exceptions, connected);
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
        return "CytoMCS";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    private void createGUI() {
        // params panel
        JPanel paramsPanel = new JPanel(new MigLayout("wrap 3"));
        paramsPanel.setBorder(new TitledBorder("Parameters"));
        iterationsSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_ITERATIONS, 1, 100, 1));
        perturbationLabel = new JLabel(Integer.toString(DEFAULT_PERTURBATION) + "%");
        perturbationSlider = new JSlider(0, 100, DEFAULT_PERTURBATION);
        exceptionsSpinner = new JSpinner(exceptionsSpinnerModel);
        connectedCheckbox = new JCheckBox("Extract largest connected component");
        paramsPanel.add(new JLabel("Iterations"));
        paramsPanel.add(iterationsSpinner, "span 2");
        paramsPanel.add(new JLabel("Perturbation"));
        paramsPanel.add(perturbationSlider);
        paramsPanel.add(perturbationLabel);
        paramsPanel.add(new JLabel("Exceptions"));
        paramsPanel.add(exceptionsSpinner, "wrap 2");
        paramsPanel.add(connectedCheckbox, "span 3");

        // networks panel
        availableList = new JList<>(availableListModel);
        selectedList = new JList<>(selectedListModel);
        includeButton = new JButton(">");
        excludeButton = new JButton("<");

        JPanel networkButtonsPanel = new JPanel(new MigLayout("wrap 1, fillx, filly", "[grow]"));
        networkButtonsPanel.add(new JPanel());
        networkButtonsPanel.add(includeButton, "grow, center");
        networkButtonsPanel.add(excludeButton, "grow, center");
        networkButtonsPanel.add(new JPanel());

        JPanel networksPanel = new JPanel(new MigLayout("wrap 3"));
        networksPanel.setBorder(new TitledBorder("Select networks to align"));

        networksPanel.add(new JLabel("Available"));
        networksPanel.add(new JPanel());
        networksPanel.add(new JLabel("Selected"));

        networksPanel.add(new JScrollPane(availableList), "width 45%");
        networksPanel.add(networkButtonsPanel, "width 10%");
        networksPanel.add(new JScrollPane(selectedList), "width 45%");

        // run panel
        alignButton = new JButton("Start alignment");

        // put together
        rootPanel = new JPanel(new MigLayout("wrap 1"));
        rootPanel.add(paramsPanel, "growx");
        rootPanel.add(networksPanel, "growx");
        rootPanel.add(alignButton, "growx");
    }

    private int numSelected() {
        int count = 0;
        Enumeration<CyNetwork> e = selectedListModel.elements();
        while(e.hasMoreElements()) {
            e.nextElement();
            count++;
        }
        return count;
    }
}
