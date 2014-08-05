package com.cjie.storm.trident.outbreak.function;

import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-20
 * Time: 上午9:53
 * To change this template use File | Settings | File Templates.
 */
public class OutbreakDetector extends BaseFunction {
    private static final long serialVersionUID = 1L;
    public static final int THRESHOLD = 10000;

    @Override
    public void execute(TridentTuple tuple,
                        TridentCollector collector) {
        String key = (String) tuple.getValue(0);
        Long count = (Long) tuple.getValue(1);
        if (count > THRESHOLD) {
            List<Object> values = new ArrayList<Object>();
            values.add("Outbreak detected for [" + key + "]!");
            collector.emit(values);
        }
    }
}
