/*
 Authors : Umberto Griffo <umberto.griffo@gmail.com>
 Linkedin : it.linkedin.com/pub/umberto-griffo/31/768/99
 Twitter : @UmbertoGriffo
 
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 The contents of this file are subject to the terms of either the GNU
 General Public License Version 3 only ("GPL") or the Common
 Development and Distribution License("CDDL") (collectively, the
 "License"). You may not use this file except in compliance with the
 License. 
 You can obtain a copy of the License at http://www.gnu.org/licenses/gpl-3.0.txt.

 */
package umberto.WeightedClusterCoefficient;

import java.awt.GridLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.gephi.graph.api.GraphController;
import org.jdesktop.swingx.JXHeader;
import org.openide.util.Lookup;

/**
 * Settings panel for the {@link weightedClusterCoefficient} statistic. It uses
 * a nice
 * <code>JXHeader</code> at the top of the panel.
 *
 * @author Umberto Griffo.
 */
public class weightedClusterCoefficientPanel extends JPanel {
    // Variables declaration - do not modify                     

    private JXHeader jXHeader1;
    private ButtonGroup directedButtonGroup;
    private JRadioButton directedRadioButton;
    private JRadioButton undirectedRadioButton;

    /**
     * Creates new form weightedClusterCoefficientPanel
     */
    public weightedClusterCoefficientPanel() {
        initComponents();
        //Disable directed if the graph is undirecteds
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (graphController.getModel().isUndirected()) {
            directedRadioButton.setEnabled(false);
        }
    }

    public boolean isDirected() {
        return directedRadioButton.isSelected();
    }

    public void setDirected(boolean directed) {
        directedButtonGroup.setSelected(directed ? directedRadioButton.getModel() : undirectedRadioButton.getModel(), true);
        if (!directed) {
            directedRadioButton.setEnabled(false);
        }
    }

    private void initComponents() {

        jXHeader1 = new JXHeader();
        jXHeader1.setDescription(" Reference: \"A. Barrat and M. Barthelemy and R. Pastor-Satorras and A. Vespignani (2004). \"The architecture of complex weighted networks\". Proceedings of the National Academy of Sciences 101 (11): 3747–3752.");
        jXHeader1.setTitle("Weighted Cluster Coefficient");
        this.add(jXHeader1);

        directedButtonGroup = new ButtonGroup();
        directedRadioButton = new JRadioButton();
        undirectedRadioButton = new JRadioButton();
        directedRadioButton.setText("Directed");
        undirectedRadioButton.setText("Undirected");
        directedButtonGroup.add(directedRadioButton);
        directedButtonGroup.add(undirectedRadioButton);

        this.add(directedRadioButton);
        this.add(undirectedRadioButton);

        GridLayout experimentLayout = new GridLayout(0, 1);
        this.setLayout(experimentLayout);

    }// </editor-fold>                 
}
