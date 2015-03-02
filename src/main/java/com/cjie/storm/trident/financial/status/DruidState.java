package com.cjie.storm.trident.financial.status;

import com.cjie.storm.trident.financial.druid.StormFirehose;
import com.cjie.storm.trident.financial.druid.StormFirehoseFactory;
import com.cjie.storm.trident.financial.dto.FixMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import storm.trident.state.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-7-1
 * Time: 下午7:01
 * To change this template use File | Settings | File Templates.
 */
public class DruidState implements State {
    private static final Logger LOG =
            LoggerFactory.getLogger(DruidState.class);
    private List<FixMessageDto> messages =
            new ArrayList<FixMessageDto>();
    private int partitionIndex;

    public DruidState(int partitionIndex) {
        this.partitionIndex = partitionIndex;
    }

    @Override
    public void beginCommit(Long batchId) {
    }

    @Override
    public void commit(Long batchId) {
        String partitionId = batchId.toString() + "-" +
                partitionIndex;
        LOG.info("Committing partition [" +
                partitionIndex + "] of batch [" + batchId + "]");
        try {
            if (StormFirehose.STATUS.isCompleted(partitionId)) {
                LOG.warn("Encountered completed partition ["
                        + partitionIndex + "] of batch [" + batchId
                        + "]");
                return;
            } else if (StormFirehose.STATUS.isInLimbo(partitionId)) {
                LOG.warn("Encountered limbo partition [" +
                        partitionIndex
                        + "] of batch [" + batchId +
                        "] : NOTIFY THE AUTHORITIES!");
                return;
            } else if (StormFirehose.STATUS.isInProgress(partitionId)) {
                LOG.warn("Encountered in-progress partition [" +
                        partitionIndex + "] of batch [" + batchId
                        +
                        "] : NOTIFY THE AUTHORITIES!");
                return;
            }
            StormFirehose.STATUS.putInProgress(partitionId);
            StormFirehoseFactory.getFirehose()
                    .sendMessages(batchId, messages);
        } catch (Exception e) {
            LOG.error("Could not start firehose for ["
                    + partitionIndex + "] of batch [" + batchId + "]", e);
        }
    }

    public void aggregateMessage(FixMessageDto message) {
        messages.add(message);
    }
}
