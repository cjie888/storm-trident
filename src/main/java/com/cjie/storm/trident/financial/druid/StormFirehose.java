package com.cjie.storm.trident.financial.druid;

import com.cjie.storm.trident.financial.status.StormCommitRunnable;
import com.cjie.storm.trident.financial.dto.FixMessageDto;
import com.cjie.storm.trident.financial.status.DruidBatchStatus;
import com.cjie.storm.trident.financial.status.DruidStateFactory;
import com.google.common.collect.Maps;
import com.metamx.druid.input.InputRow;
import com.metamx.druid.input.MapBasedInputRow;
import com.metamx.druid.realtime.firehose.Firehose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-7-1
 * Time: 下午7:08
 * To change this template use File | Settings | File Templates.
 */
public class StormFirehose implements Firehose {


    private static final Logger LOG =
            LoggerFactory.getLogger(DruidStateFactory.class);
    private ArrayBlockingQueue<FixMessageDto> BLOCKING_QUEUE;
    private Long TRANSACTION_ID;
    private Object START;
    private Object FINISHED;
    public static final DruidBatchStatus STATUS = new DruidBatchStatus();
    private List<Long> LIMBO_TRANSACTIONS;

    public synchronized void sendMessages(Long partitionId,
                                          List<FixMessageDto> messages) {
        BLOCKING_QUEUE = new ArrayBlockingQueue<FixMessageDto>(messages.size(), false, messages);
        TRANSACTION_ID = partitionId;
        LOG.info("Beginning commit to Druid. [" +  messages.size() + "] messages, unlocking [START]");
        synchronized (START) {
            START.notify();
        }
        try {
            synchronized (FINISHED) {
                FINISHED.wait();
            }
        } catch (InterruptedException e) {
            LOG.error("Commit to Druid interrupted.");
        }
        LOG.info("Returning control to Storm.");
    }

    @Override
    public InputRow nextRow() {
        final Map<String, Object> theMap =
                Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
        try {
            FixMessageDto message = null;
            message = BLOCKING_QUEUE.poll();
            if (message != null) {
                LOG.info("[" + message.symbol + "] @ [" + message.price + "]");
                theMap.put("symbol", message.symbol);
                theMap.put("price", message.price);
            }
            if (BLOCKING_QUEUE.isEmpty()) {
                STATUS.putInLimbo(TRANSACTION_ID);
                LIMBO_TRANSACTIONS.add(TRANSACTION_ID);
                LOG.info("Batch is fully consumed by Druid. "
                        + "Unlocking [FINISH]");
                synchronized (FINISHED) {
                    FINISHED.notify();
                }
            }
        } catch (Exception e) {
            LOG.error("Error occurred in nextRow.", e);
            System.exit(-1);
        }
        final LinkedList<String> dimensions = new LinkedList<String>();
        dimensions.add("symbol");
        dimensions.add("price");
        return new MapBasedInputRow(System.currentTimeMillis(), dimensions, theMap);
    }

    @Override
    public boolean hasMore() {
        if (BLOCKING_QUEUE != null
                && !BLOCKING_QUEUE.isEmpty())
            return true;
        try {
            synchronized (START) {
                START.wait();
            }
        } catch (InterruptedException e) {
            LOG.error("hasMore() blocking interrupted!");
        }
        return true;
    }

    @Override
    public Runnable commit() {
        List<Long> limboTransactions = new ArrayList<Long>();
        //LIMBO_TRANSACTIONS.drainTo(limboTransactions);
        return new StormCommitRunnable(limboTransactions);
    }

    @Override
    public void close() throws IOException {

    }
}
