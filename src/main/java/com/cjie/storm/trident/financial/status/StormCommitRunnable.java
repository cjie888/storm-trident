package com.cjie.storm.trident.financial.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-7-1
 * Time: 下午7:12
 * To change this template use File | Settings | File Templates.
 */
public class StormCommitRunnable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(StormCommitRunnable.class);
    private List<Long> partitionIds = null;

    public StormCommitRunnable(List<Long> partitionIds) {
        this.partitionIds = partitionIds;
    }

    @Override
    public void run() {
        try {
            //StormFirehose.STATUS.complete(partitionIds);
        } catch (Exception e) {
            LOG.error("Could not complete transactions.", e);
        }
    }
}
