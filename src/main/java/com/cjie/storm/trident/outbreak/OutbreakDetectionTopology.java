package com.cjie.storm.trident.outbreak;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.generated.StormTopology;
import backtype.storm.tuple.Fields;
import com.cjie.storm.trident.outbreak.aggregator.Count;
import com.cjie.storm.trident.outbreak.filter.DiseaseFilter;
import com.cjie.storm.trident.outbreak.function.CityAssignment;
import com.cjie.storm.trident.outbreak.function.DispatchAlert;
import com.cjie.storm.trident.outbreak.function.HourAssignment;
import com.cjie.storm.trident.outbreak.function.OutbreakDetector;
import com.cjie.storm.trident.outbreak.spout.DiagnosisEventSpout;
import com.cjie.storm.trident.outbreak.state.OutbreakTrendFactory;
import storm.trident.Stream;
import storm.trident.TridentTopology;
/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-19
 * Time: 上午9:37
 * To change this template use File | Settings | File Templates.
 */
public class OutbreakDetectionTopology {
    public static StormTopology buildTopology() {
        TridentTopology topology = new TridentTopology();
        DiagnosisEventSpout spout = new DiagnosisEventSpout();
        Stream inputStream = topology.newStream("event", spout);
        inputStream
                // Filter for critical events.
                .each(new Fields("event"), new DiseaseFilter())
                        // Locate the closest city
                .each(new Fields("event"), new CityAssignment(), new Fields("city"))
                        // Derive the hour segment
                .each(new Fields("event", "city"), new HourAssignment(), new Fields("hour", "cityDiseaseHour"))
                        // Group occurrences in same city and hour
                .groupBy(new Fields("cityDiseaseHour"))
                        // Count occurrences and persist the results.
                .persistentAggregate(new
                        OutbreakTrendFactory(), new Count(), new Fields("count"))
                .newValuesStream()
                // Detect an outbreak
                .each(new Fields("cityDiseaseHour", "count"),
                        new OutbreakDetector(), new Fields("alert"))// Dispatch the alert
                .each(new Fields("alert"), new DispatchAlert(), new Fields());
        return topology.build();
    }
    public static void main(String[] args) throws
            Exception {
        Config conf = new Config();
        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("cdc", conf, buildTopology());
        Thread.sleep(200000);
        cluster.shutdown();
    }
}