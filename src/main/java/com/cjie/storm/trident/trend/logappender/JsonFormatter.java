package com.cjie.storm.trident.trend.logappender;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-24
 * Time: 下午7:10
 * To change this template use File | Settings | File Templates.
 */
public class JsonFormatter implements Formatter {
    private static final String QUOTE = "\"";
    private static final String COLON = ":";
    private static final String COMMA = ",";
    private boolean expectJson = false;

    public String format(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        fieldName("level", sb);
        quote(event.getLevel().levelStr, sb);
        sb.append(COMMA);
        fieldName("logger", sb);
        quote(event.getLoggerName(), sb);
        sb.append(COMMA);
        fieldName("timestamp", sb);
        sb.append(event.getTimeStamp());
        sb.append(COMMA);
        fieldName("message", sb);
        if (this.expectJson) {
            sb.append(event.getFormattedMessage());
        } else {
            quote(event.getFormattedMessage(), sb);
        }
        sb.append("}");
        return sb.toString();
    }
    private static void fieldName(String name, StringBuilder sb) {
        quote(name, sb);
        sb.append(COLON);
    }
    private static void quote(String value, StringBuilder sb) {
        sb.append(QUOTE);
        sb.append(value);
        sb.append(QUOTE);
    }
    public boolean isExpectJson() {
        return expectJson;
    }
    public void setExpectJson(boolean expectJson) {
        this.expectJson = expectJson;
    }
}
