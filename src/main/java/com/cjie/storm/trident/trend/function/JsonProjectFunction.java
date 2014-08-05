package com.cjie.storm.trident.trend.function;

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import org.json.simple.JSONValue;
import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-24
 * Time: 下午8:02
 * To change this template use File | Settings | File Templates.
 */
public class JsonProjectFunction extends BaseFunction {

    private Fields fields;
    public JsonProjectFunction(Fields fields) {
        this.fields = fields;
    }
    public void execute(TridentTuple tuple,
                        TridentCollector collector) {
        String json = tuple.getString(0);
        Map<String, Object> map = (Map<String, Object>) JSONValue.parse(json);
        Values values = new Values();
        for (int i = 0; i < this.fields.size(); i++) {
            values.add(map.get(this.fields.get(i)));
        }
        collector.emit(values);
    }
}
