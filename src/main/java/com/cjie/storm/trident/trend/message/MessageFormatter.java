package com.cjie.storm.trident.trend.message;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.cjie.storm.trident.trend.logappender.Formatter;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-23
 * Time: 下午8:39
 * To change this template use File | Settings | File Templates.
 */
public class MessageFormatter implements Formatter {
    public String format(ILoggingEvent event) {
        return event.getFormattedMessage();
    }
}
