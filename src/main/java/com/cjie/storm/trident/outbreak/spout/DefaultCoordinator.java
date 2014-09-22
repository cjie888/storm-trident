package com.cjie.storm.trident.outbreak.spout;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import storm.trident.spout.ITridentSpout;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-19
 * Time: 上午9:54
 * To change this template use File | Settings | File Templates.
 */
public class DefaultCoordinator implements
        ITridentSpout.BatchCoordinator<Long>, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG =
            LoggerFactory.getLogger(DefaultCoordinator.class);

    @Override
    public boolean isReady(long txid) {
        return true;
    }

    @Override
    public void close() {
    }


    @Override
    public Long initializeTransaction(long txid, Long prevMetadata, Long currMetadata) {
        return null;
    }

    @Override
    public void success(long txid) {
        LOG.info("Successful Transaction [" + txid + "]");
    }
}
