package com.cjie.storm.trident.financial.status;

import com.cjie.storm.trident.financial.dto.FixMessageDto;
import storm.trident.operation.TridentCollector;
import storm.trident.operation.TridentOperationContext;
import storm.trident.state.StateUpdater;
import storm.trident.tuple.TridentTuple;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-7-1
 * Time: 下午6:40
 * To change this template use File | Settings | File Templates.
 */
public class DruidStateUpdater implements
        StateUpdater<DruidState> {
    @Override
    public void updateState(DruidState state,
                            List<TridentTuple> tuples, TridentCollector collector) {
        for (TridentTuple tuple : tuples) {
            FixMessageDto message = (FixMessageDto) tuple.getValue(0);
            state.aggregateMessage(message);
        }
    }

    @Override
    public void prepare(Map conf, TridentOperationContext context) {
    }

    @Override
    public void cleanup() {
    }
}
