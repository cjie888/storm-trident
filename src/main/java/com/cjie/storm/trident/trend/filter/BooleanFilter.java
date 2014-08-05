package com.cjie.storm.trident.trend.filter;

import storm.trident.operation.BaseFilter;
import storm.trident.tuple.TridentTuple;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-25
 * Time: 上午10:00
 * To change this template use File | Settings | File Templates.
 */
public class BooleanFilter extends BaseFilter {
    public boolean isKeep(TridentTuple tuple) {
        return tuple.getBoolean(0);
    }
}