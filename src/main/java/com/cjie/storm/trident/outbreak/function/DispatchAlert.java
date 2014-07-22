package com.cjie.storm.trident.outbreak.function;

import com.cjie.storm.trident.outbreak.function.CityAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-20
 * Time: 上午9:55
 * To change this template use File | Settings | File Templates.
 */
public class DispatchAlert extends BaseFunction {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG =
            LoggerFactory.getLogger(CityAssignment.class);
    @Override
    public void execute(TridentTuple tuple,
                        TridentCollector collector) {
        String alert = (String) tuple.getValue(0);
        LOG.error("ALERT RECEIVED [" + alert + "]");
        LOG.error("Dispatch the national guard!");
        System.exit(0);
    }
}
