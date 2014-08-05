package com.cjie.storm.trident.trend;

import storm.trident.TridentTopology;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-24
 * Time: 下午7:59
 * To change this template use File | Settings | File Templates.
 */
public class TrendTopology {

    public static void main(String[] args) {
        TridentTopology topology = new
                TridentTopology();
        /*StaticHosts kafkaHosts =
                KafkaConfig.StaticHosts.fromHostString(Arrays.asLis
                        t(new String[] { "localhost" }), 1);
        TridentKafkaConfig spoutConf = new
                TridentKafkaConfig(kafkaHosts, "log-analysis");
        spoutConf.scheme = new StringScheme();
        spoutConf.forceStartOffsetTime(-1);
        OpaqueTridentKafkaSpout spout = new
                OpaqueTridentKafkaSpout(spoutConf);
        Stream spoutStream =
                topology.newStream("kafka-stream", spout);  */
    }
}
