package com.cjie.storm.trident.trend.logappender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.cjie.storm.trident.trend.message.MessageFormatter;
import kafka.javaapi.producer.Producer;
import kafka.javaapi.producer.ProducerData;
import kafka.producer.ProducerConfig;

import java.util.Properties;


/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-23
 * Time: 下午8:27
 * To change this template use File | Settings | File Templates.
 */
public class KafkaAppender extends
        AppenderBase<ILoggingEvent> {
    private String topic;
    private String zookeeperHost;
    private Producer<String, String> producer;
    private Formatter formatter;

    // java bean definitions used to inject
// configuration values from logback.xml
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
    public String getZookeeperHost() {
        return zookeeperHost;
    }
    public void setZookeeperHost(String zookeeperHost)
    {
        this.zookeeperHost = zookeeperHost;
    }
    public Formatter getFormatter() {
        return formatter;
    }
    public void setFormatter(Formatter formatter) {
        this.formatter = formatter;
    }
    // overrides
    @Override
    public void start() {
        if (this.formatter == null) {
            this.formatter = new MessageFormatter();
        }
        super.start();
        Properties props = new Properties();
        props.put("zk.connect", this.zookeeperHost);
        props.put("serializer.class",
                "kafka.serializer.StringEncoder");
        ProducerConfig config = new ProducerConfig(props);
        this.producer = new Producer<String, String>(config);
    }
    @Override
    public void stop() {
        super.stop();
        this.producer.close();
    }
    @Override
    protected void append(ILoggingEvent event) {
       String payload = this.formatter.format(event);
       ProducerData<String, String> data = new
               ProducerData<String, String>(this.topic, payload);
       this.producer.send(data);
    }
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("zk.connect", "testserver:2181");
        props.put("serializer.class",
                "kafka.serializer.StringEncoder");
        ProducerConfig config = new ProducerConfig(props);
        Producer producer = new Producer<String, String>(config);
        String payload = String.format("abc%s","test");
        ProducerData<String, String> data = new
                ProducerData<String, String>("mytopic", payload);
        producer.send(data);
    }
}
