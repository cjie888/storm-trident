package com.cjie.storm.trident.financial.status;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-7-1
 * Time: 下午7:17
 * To change this template use File | Settings | File Templates.
 */
public class DruidBatchStatus {
    private static final Logger LOG = LoggerFactory.getLogger(DruidBatchStatus.class);
    final String COMPLETED_PATH = "completed";
    final String LIMBO_PATH = "limbo";

    final String CURRENT_PATH = "current";
    private CuratorFramework curatorFramework;

    public DruidBatchStatus() {
        try {
            curatorFramework =
                    CuratorFrameworkFactory.builder()
                            .namespace("stormdruid")
                            .connectString("localhost:2181")
                            .retryPolicy(new RetryNTimes(1, 1000))
                            .connectionTimeoutMs(5000)
                            .build();
            curatorFramework.start();
            if (curatorFramework.checkExists().forPath(COMPLETED_PATH) == null) {
                curatorFramework.create().forPath(COMPLETED_PATH);
            }
        } catch (Exception e) {
            LOG.error("Could not establish connection to Zookeeper", e);
        }
    }

    public boolean isCompleted(String paritionId) throws Exception {
        return (curatorFramework.checkExists().forPath(COMPLETED_PATH + "/" + paritionId) != null);
    }
    public boolean isInLimbo(String paritionId) throws Exception {
        return (curatorFramework.checkExists().forPath(LIMBO_PATH + "/" + paritionId) != null);
    }
    public boolean isInProgress(String paritionId) throws Exception {
        return (curatorFramework.checkExists().forPath(CURRENT_PATH + "/" + paritionId) != null);
    }
    public void putInLimbo(Long paritionId) throws Exception {
        curatorFramework.inTransaction().
                delete().forPath(CURRENT_PATH + "/" + paritionId)
                .and().create().forPath(LIMBO_PATH + "/" + paritionId).and().commit();
    }
    public void putInProgress(String paritionId) throws Exception {
        curatorFramework.create().forPath(CURRENT_PATH + "/" + paritionId);
    }
}