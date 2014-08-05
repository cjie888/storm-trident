package com.cjie.storm.trident.trend.function;

import backtype.storm.tuple.Values;
import com.cjie.storm.trident.trend.EWMA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-25
 * Time: 上午9:49
 * To change this template use File | Settings | File Templates.
 */
public class MovingAverageFunction extends
        BaseFunction {
    private static final Logger LOG = LoggerFactory.getLogger(BaseFunction.class);
    private EWMA ewma;
    private EWMA.Time emitRatePer;
    public MovingAverageFunction(EWMA ewma, EWMA.Time emitRatePer){
        this.ewma = ewma;
        this.emitRatePer = emitRatePer;
    }
    public void execute(TridentTuple tuple,
                        TridentCollector collector) {
        this.ewma.mark(tuple.getLong(0));
        LOG.debug("Rate: {}", this.ewma.getAverageRatePer(this.emitRatePer));
        collector.emit(new Values(this.ewma.getAverageRatePer(this.emitRatePer)));
    }
}
