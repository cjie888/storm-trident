package com.cjie.storm.trident.financial;

import backtype.storm.generated.StormTopology;
import backtype.storm.tuple.Fields;
import com.cjie.storm.trident.financial.filter.MessageTypeFilter;
import com.cjie.storm.trident.financial.spout.FixEventSpout;
import com.cjie.storm.trident.financial.status.DruidStateFactory;
import com.cjie.storm.trident.financial.status.DruidStateUpdater;
import storm.trident.Stream;
import storm.trident.TridentTopology;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-7-1
 * Time: 下午6:39
 * To change this template use File | Settings | File Templates.
 */
public class FinancialAnalyticsTopology {
    public static StormTopology buildTopology() {
        TridentTopology topology = new TridentTopology();
        FixEventSpout spout = new FixEventSpout();
        Stream inputStream = topology.newStream("message", spout);
        inputStream.each(new Fields("message"), new MessageTypeFilter())
                .partitionPersist(new DruidStateFactory(), new Fields("message"), new DruidStateUpdater());
        return topology.build();
    }
}
