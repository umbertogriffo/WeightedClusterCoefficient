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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.data.attributes.api.*;
import org.gephi.graph.api.*;
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.openide.util.Lookup;

/**
 * Calculates the Weighted Cluster Coefficient for every node and the average
 * value.
 *
 * @see WeightedClusterCoefficientBuilder
 * @see WeightedClusterCoefficientUI
 * @author Umberto Griffo
 */
public class WeightedClusterCoefficient implements Statistics, LongTask {
    //public cons for the result column name if reused by others

    public static final String AVG_WEIGHTED_CLUSTER_COEFFICIENT = "avg_weighted_cluster_coefficient";
    public static final String AVERAGE_WEIGHTED_CLUSTER_COEFFICIENT = "average_weighted_cluster_coefficient";
    private final static Logger LOGGER = Logger.getLogger("org.umberto.weighted_cluster_coefficient");
    private String report = "";
    /**
     * Remembers if the Cancel function has been called.
     */
    private boolean cancel;
    private ProgressTicket progressTicket;
    /**
     * Indicates should treat graph as undirected.
     */
    private boolean isDirected;
    /**
     * Keep track of the work done.
     */
    private int N;
    private double[] nodeClustering;
    private double avgClusteringCoeff;
    private double avgStrenght;
    private SortedMap<Float, Integer> strenght_distribution = new TreeMap<Float, Integer>();
    private SortedMap<Float, Float> cumulated_strenght_distribution = new TreeMap<Float, Float>();// to plot cumulate distribution
    private SortedMap<Float, Integer> cc_distribution = new TreeMap<Float, Integer>();

    public WeightedClusterCoefficient() {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (graphController != null && graphController.getModel() != null) {
            isDirected = graphController.getModel().isDirected();
        }
    }

    @Override
    public void execute(GraphModel graphModel, AttributeModel attributeModel) {
        HierarchicalGraph hgraph = null;
        if (isDirected) {
            hgraph = graphModel.getHierarchicalDirectedGraphVisible();
        } else {
            hgraph = graphModel.getHierarchicalUndirectedGraphVisible();
        }

        execute(hgraph, attributeModel);
    }

    public void execute(HierarchicalGraph hgraph, AttributeModel attributeModel) {
        cancel = false;
        //The atrributes computed by the statistics
        AttributeTable nodeTable = attributeModel.getNodeTable();
        AttributeColumn clusteringCol = nodeTable.getColumn("weightedclustering");
        if (clusteringCol == null) {
            clusteringCol = nodeTable.addColumn("weightedclustering", "Weighted Clustering Coefficient", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        AttributeColumn strenghtCol = nodeTable.getColumn("strenght");
        if (strenghtCol == null) {
            strenghtCol = nodeTable.addColumn("strenght", "Strenght", AttributeType.DOUBLE, AttributeOrigin.COMPUTED, new Double(0));
        }
        float totalCC = 0;
        float totalStrenght = 0;

        hgraph.readLock();

        N = hgraph.getNodeCount();
        nodeClustering = new double[N];
        Progress.start(progressTicket, N);

        int node_count = 0;

        for (Node node : hgraph.getNodes()) {
            float strenght = 0;
            float nodeCC = 0;
            float cc = 0;
            int degree = hgraph.getDegree(node);
            //Calculate the node strenght
            if (isDirected) {
                float totalInWeight = 0;
                float totalOutWeight = 0;
                for (Iterator it = ((HierarchicalDirectedGraph) hgraph).getEdgesAndMetaEdges(node).iterator(); it.hasNext();) {
                    Edge e = (Edge) it.next();
                    if (e.getSource().equals(node)) {
                        totalOutWeight += (float) e.getWeight();
                    }
                    if (e.getTarget().equals(node)) {
                        totalInWeight += (float) e.getWeight();
                    }
                }
                strenght = totalInWeight + totalOutWeight;
                //populate strenght distribution
                int count = strenght_distribution.containsKey(strenght) ? strenght_distribution.get(strenght) : 0;
                strenght_distribution.put(strenght, count + 1);

            } else {
                for (Iterator it = hgraph.getEdgesAndMetaEdges(node).iterator(); it.hasNext();) {
                    Edge e = (Edge) it.next();
                    strenght += (float) e.getWeight();
                }
                //populate strenght distribution
                int count = strenght_distribution.containsKey(strenght) ? strenght_distribution.get(strenght) : 0;
                strenght_distribution.put(strenght, count + 1);
            }
            //if a node have degree < 2 means that this node can't form a triangle.
            if (degree < 2) {
                cc = 0;
            } else {
                //Search Triangle
                NodeIterable neighbors1 = hgraph.getNeighbors(node);
                //browse the pairs of neighbors
                for (Node neighbor1 : neighbors1) {
                    NodeIterable neighbors2 = hgraph.getNeighbors(node);
                    for (Node neighbor2 : neighbors2) {
                        if (neighbor1 == neighbor2) {
                            continue;
                        }
                        if (isDirected) {
                            float w1 = 0;
                            float w2 = 0;
                            //find a triangle
                            if (((HierarchicalDirectedGraph) hgraph).getEdge(neighbor1, neighbor2) != null) {
                                if (((HierarchicalDirectedGraph) hgraph).getEdge(node, neighbor1) != null) {
                                    w1 = hgraph.getEdge(node, neighbor1).getWeight();
                                } else if (((HierarchicalDirectedGraph) hgraph).getEdge(neighbor1, node) != null) {
                                    w1 = hgraph.getEdge(neighbor1, node).getWeight();
                                }
                                if (((HierarchicalDirectedGraph) hgraph).getEdge(node, neighbor2) != null) {
                                    w2 = hgraph.getEdge(node, neighbor2).getWeight();
                                } else if (((HierarchicalDirectedGraph) hgraph).getEdge(neighbor2, node) != null) {
                                    w2 = hgraph.getEdge(neighbor2, node).getWeight();
                                }
                                nodeCC += (w1 + w2) / 2;
                            }
                            //find a triangle
                            if (((HierarchicalDirectedGraph) hgraph).getEdge(neighbor2, neighbor1) != null) {
                                if (((HierarchicalDirectedGraph) hgraph).getEdge(node, neighbor1) != null) {
                                    w1 = hgraph.getEdge(node, neighbor1).getWeight();
                                } else if (((HierarchicalDirectedGraph) hgraph).getEdge(neighbor1, node) != null) {
                                    w1 = hgraph.getEdge(neighbor1, node).getWeight();
                                }
                                if (((HierarchicalDirectedGraph) hgraph).getEdge(node, neighbor2) != null) {
                                    w2 = hgraph.getEdge(node, neighbor2).getWeight();
                                } else if (((HierarchicalDirectedGraph) hgraph).getEdge(neighbor2, node) != null) {
                                    w2 = hgraph.getEdge(neighbor2, node).getWeight();
                                }
                                nodeCC += (w1 + w2) / 2;
                            }
                        } else {
                            //find a triangle
                            if (hgraph.isAdjacent(neighbor1, neighbor2)) {
                                nodeCC += (hgraph.getEdge(node, neighbor1).getWeight() + hgraph.getEdge(node, neighbor2).getWeight()) / 2;
                            }
                        }
                    }
                }
                nodeCC /= 2.0;//because a triangle is seen 2 times
                //Calculate local cluster coefficient
                cc = (2 / (strenght * (degree - 1))) * nodeCC;
                if (isDirected) {
                    cc = (1 / (strenght * (degree - 1))) * nodeCC;
                }
            }

            nodeClustering[node_count] = cc;
            //populate cc distribution
            int count = cc_distribution.containsKey(cc) ? cc_distribution.get(cc) : 0;
            cc_distribution.put(cc, count + 1);
            //Save the node values
            AttributeRow row = (AttributeRow) node.getNodeData().getAttributes();
            row.setValue(clusteringCol, cc);
            row.setValue(strenghtCol, strenght);

            totalCC += cc;
            totalStrenght += strenght;
            if (cancel) {
                break;
            }
            node_count++;
            Progress.progress(progressTicket, node_count);
        }
        avgClusteringCoeff = totalCC / hgraph.getNodeCount();
        avgStrenght = totalStrenght / hgraph.getNodeCount();
        hgraph.readUnlockAll();
    }

    public double getAverageWeightedClusteringCoefficient() {
        return avgClusteringCoeff;
    }

    public double getAverageNodeStrenght() {
        return avgStrenght;
    }

    @Override
    public String getReport() {
        /*
         * SAVE IN FILE FOR MULTIPLE PLOT
         */
        //current directory
        boolean success;
        String currentDir = System.getProperty("user.dir");
        String saveDir = currentDir + "\\savedata";
        boolean exist = (new File(saveDir)).exists();
        if (!exist) {
            success = (new File(saveDir)).mkdir();
        } else {
            success = true;
        }
        Set<SortedMap.Entry<Float, Integer>> set_strenght2 = strenght_distribution.entrySet();
        for (SortedMap.Entry<Float, Integer> element : set_strenght2) {
            float cumulated = 0;
            for (SortedMap.Entry<Float, Integer> element2 : set_strenght2) {
                //F(s) = nodes fraction with (strenght/strenght max) >= s
                if (element2.getKey() >= element.getKey()) {
                    cumulated += element2.getValue();
                }
            }
            cumulated_strenght_distribution.put(element.getKey(), (cumulated / strenght_distribution.lastKey()));
        }
        System.out.println(strenght_distribution);
        System.out.println(cumulated_strenght_distribution);

        //Transform to Map
        Map<Float, Float> strenght_distribution_map = new HashMap<Float, Float>();
        Set<SortedMap.Entry<Float, Float>> set_strenght = cumulated_strenght_distribution.entrySet();
        for (SortedMap.Entry<Float, Float> element : set_strenght) {
            Float num_occorrenze = element.getValue();
            Float strenght = element.getKey();
            strenght_distribution_map.put(strenght, num_occorrenze);
        }

        System.out.println(cc_distribution);

        Map<Float, Integer> cc_distribution_map = new HashMap<Float, Integer>();

        Set<SortedMap.Entry<Float, Integer>> set_cc = cc_distribution.entrySet();
        for (SortedMap.Entry<Float, Integer> element : set_cc) {
            Integer num_occorrenze = element.getValue();
            Float cc = element.getKey();
            //double prob = (double) num_occorrenze / N;
            cc_distribution_map.put(cc, num_occorrenze);
        }
        //Save file
        if (success) {
            String completeDirIntra = saveDir + "\\StrengthDistribution.txt";
            String finalDir = completeDirIntra.replace("\\", "\\\\");
            LOGGER.log(Level.INFO, "Intra path: {0}", new Object[]{finalDir});
            generateSaveFile(finalDir, strenght_distribution_map);
        } else {
            LOGGER.log(Level.INFO, "Don't create a new directory");
        }
        System.out.println(strenght_distribution_map);
        System.out.println(cc_distribution_map);
        //Distribution series
        XYSeries strenghtSeries = ChartUtils.createXYSeries(strenght_distribution_map, "Strength Distribution");
        XYSeries ccSeries = ChartUtils.createXYSeries(cc_distribution_map, "Weighted cluster coefficient Distribution");

        XYSeriesCollection dataset1 = new XYSeriesCollection();
        dataset1.addSeries(strenghtSeries);

        XYSeriesCollection dataset2 = new XYSeriesCollection();
        dataset2.addSeries(ccSeries);

        JFreeChart chart1 = ChartFactory.createXYLineChart(
                "Strenght Distribution",
                "Strength",
                "F(Strength)",
                dataset1,
                PlotOrientation.VERTICAL,
                true,
                false,
                false);
        ChartUtils.decorateChart(chart1);
        ChartUtils.scaleLogChart(chart1, strenghtSeries, false);
        String strenghtImageFile = ChartUtils.renderChart(chart1, "Strength-distribution.png");

        JFreeChart chart2 = ChartFactory.createXYLineChart(
                "Weighted Cluster Coefficient Distribution",
                "Weighted Cluster Coefficient",
                "# Nodes",
                dataset2,
                PlotOrientation.VERTICAL,
                true,
                false,
                false);


        ChartUtils.decorateChart(chart2);
        ChartUtils.scaleChart(chart2, ccSeries, false);
        String ccImageFile = ChartUtils.renderChart(chart2, "ccdistribution.png");

        NumberFormat f = new DecimalFormat("#0.000");

        return "<HTML> <BODY> <h1> Weighted Clustering Coefficient Metric Report </h1> "
                + "<hr>"
                + "<br />" + "<h2> Parameters: </h2>"
                + "Network Interpretation:  " + (isDirected ? "directed" : "undirected") + "<br />"
                + "<br>" + "<h2> Results: </h2>"
                + "Average Clustering Coefficient: " + f.format(avgClusteringCoeff) + "<br />"
                + "The Average Clustering Coefficient is the mean value of individual coefficients.<br /><br />"
                + ccImageFile
                + "<br /><br />"
                + "Average Node Strength: " + f.format(avgStrenght) + "<br />"
                + "The Average Node Strength is the mean value of individual node Strength.<br /><br />"
                + strenghtImageFile
                + "<br /><br />"
                + "Where F(Strength) = fraction of nodes with (Strength/Max Strength)>= Strength"
                + "<br />"
                + "<h2> Algorithm: </h2>"
                + "Simple and slow brute force.<br />"
                + "Reference: \"A. Barrat and M. Barthelemy and R. Pastor-Satorras and A. Vespignani (2004). \"The architecture of complex weighted networks\". Proceedings of the National Academy of Sciences 101 (11): 3747–3752."
                + "</BODY> </HTML>";
    }

    @Override
    public boolean cancel() {
        this.cancel = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progressTicket = progressTicket;
    }

    public void setDirected(boolean isDirected) {
        this.isDirected = isDirected;
    }

    public boolean isDirected() {
        return isDirected;
    }

    /**
     * Generate distribution file.
     *
     * @param sFileName file path.
     */
    public void generateSaveFile(String sFileName, Map<Float, Float> map) {
        try {
            FileWriter writer = new FileWriter(sFileName);
            Set<Map.Entry<Float, Float>> set = map.entrySet();
            int k = 0;
            for (Map.Entry<Float, Float> element : set) {
                writer.append(element.getKey().toString());
                writer.append(' ');
                writer.append(element.getValue().toString());
                k = k + 1;
                if (k != map.size()) {
                    writer.append('\n');
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
