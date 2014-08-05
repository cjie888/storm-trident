package com.cjie.storm.trident.trend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-23
 * Time: 下午8:00
 * To change this template use File | Settings | File Templates.
 */
public class RogueApplication {
//    private static final Logger LOG =
//            LoggerFactory.getLogger(RogueApplication.class);
    private static final Logger LOG =
        LoggerFactory.getLogger("com.cjie.storm.trident.trend.RogueApplication");
    public static void main(String[] args) throws Exception {
        int slowCount = 6;
        int fastCount = 15;
        while (true)        {
            // slow state
            for(int i = 0; i < slowCount; i++){
                LOG.warn("This is a warning (slow state).");
                Thread.sleep(5000);
            }
            // enter rapid state
            for(int i = 0; i < fastCount; i++){
                LOG.warn("This is a warning (rapid state).");
                Thread.sleep(1000);
            }
            // return to slow state
            for(int i = 0; i < slowCount; i++){
                LOG.warn("This is a warning (slow state).");
                Thread.sleep(5000);
            }
        }

    }
}
