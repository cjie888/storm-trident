package com.cjie.storm.trident.financial.druid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.metamx.druid.realtime.firehose.Firehose;
import com.metamx.druid.realtime.firehose.FirehoseFactory;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-7-1
 * Time: 下午7:06
 * To change this template use File | Settings | File Templates.
 */
@JsonTypeName("storm")
public class StormFirehoseFactory implements
        FirehoseFactory {
    private static final StormFirehose FIREHOSE = new StormFirehose();

    @JsonCreator
    public StormFirehoseFactory() {
    }

    public static StormFirehose getFirehose() {
        return FIREHOSE;
    }

    @Override
    public Firehose connect() throws IOException {
        return FIREHOSE;
    }
}
