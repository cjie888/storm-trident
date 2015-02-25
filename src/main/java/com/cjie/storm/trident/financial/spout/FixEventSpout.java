package com.cjie.storm.trident.financial.spout;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Fields;
import com.cjie.storm.trident.outbreak.spout.DefaultCoordinator;
import storm.trident.spout.ITridentSpout;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-7-1
 * Time: 下午6:39
 * To change this template use File | Settings | File Templates.
 */
public class FixEventSpout implements ITridentSpout<Long> {
    private static final long serialVersionUID = 1L;
    SpoutOutputCollector collector;
    BatchCoordinator<Long> coordinator = new DefaultCoordinator();
    Emitter<Long> emitter = new FixEventEmitter();


    @Override
    public BatchCoordinator<Long> getCoordinator(String txStateId, Map conf, TopologyContext
            context) {
        return coordinator;
    }
    @Override
    public Emitter<Long> getEmitter(String txStateId, Map conf, TopologyContext context) {
        return emitter;
    }
    @Override
    public Map getComponentConfiguration() {
        return null;
    }
    @Override
    public Fields getOutputFields() {
        return new Fields("message");
    }
}
