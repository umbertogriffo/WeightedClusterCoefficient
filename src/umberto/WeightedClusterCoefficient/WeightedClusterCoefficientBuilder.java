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

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * Builder for the {@link WeightedClusterCoefficient} statistic.
 *
 * @author Umberto Griffo
 */
@ServiceProvider(service = StatisticsBuilder.class)
public class WeightedClusterCoefficientBuilder implements StatisticsBuilder {

    public static final String NAME = "Weighted Cluster Coefficient";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Statistics getStatistics() {
        return new WeightedClusterCoefficient();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return WeightedClusterCoefficient.class;
    }
}
