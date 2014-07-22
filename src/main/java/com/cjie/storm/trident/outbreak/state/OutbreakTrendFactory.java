package com.cjie.storm.trident.outbreak.state;

import backtype.storm.task.IMetricsContext;
import storm.trident.state.State;
import storm.trident.state.StateFactory;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-20
 * Time: 下午7:54
 * To change this template use File | Settings | File Templates.
 */
public class OutbreakTrendFactory implements
        StateFactory {
    @Override
    public State makeState(Map conf, IMetricsContext
            metrics,
                           int partitionIndex, int numPartitions) {
        return new OutbreakTrendState(new
                OutbreakTrendBackingMap());
    }
}
