package com.cjie.storm.trident.financial.status;

import backtype.storm.task.IMetricsContext;
import com.cjie.storm.trident.financial.druid.StormFirehoseFactory;
import com.metamx.common.lifecycle.Lifecycle;
import com.metamx.druid.realtime.RealtimeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import storm.trident.state.State;
import storm.trident.state.StateFactory;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-7-1
 * Time: 下午6:40
 * To change this template use File | Settings | File Templates.
 */
public class DruidStateFactory implements
        StateFactory {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG =
            LoggerFactory.getLogger(DruidStateFactory.class);
    private static RealtimeNode rn = null;

    private static synchronized void startRealtime() {
        if (rn == null) {
            final Lifecycle lifecycle = new Lifecycle();
            rn = RealtimeNode.builder().build();
            lifecycle.addManagedInstance(rn);
            rn.registerJacksonInjectable("storm", StormFirehoseFactory.class);
            try {
                lifecycle.start();
            } catch (Throwable t) {
            }
        }
    }

    @Override
    public State makeState(Map conf, IMetricsContext
            metrics,
                           int partitionIndex, int numPartitions) {
        DruidStateFactory.startRealtime();
        return new DruidState(partitionIndex);
    }
}
