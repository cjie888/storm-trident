package com.cjie.storm.trident.outbreak.state;

import storm.trident.state.map.NonTransactionalMap;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-20
 * Time: 下午8:10
 * To change this template use File | Settings | File Templates.
 */
public class OutbreakTrendState extends NonTransactionalMap<Long> {
    protected OutbreakTrendState(OutbreakTrendBackingMap outbreakBackingMap) {
        super(outbreakBackingMap);
    }
}
