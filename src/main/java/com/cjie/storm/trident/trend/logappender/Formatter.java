package com.cjie.storm.trident.trend.logappender;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-23
 * Time: 下午8:38
 * To change this template use File | Settings | File Templates.
 */
public interface Formatter {
    String format(ILoggingEvent event);

}
