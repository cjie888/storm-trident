package com.cjie.storm.trident.druid.spout;

import com.cjie.storm.trident.druid.dto.FixMessageDto;
import net.java.fixparser.SimpleFixMessage;
import net.java.fixparser.SimpleFixParser;
import net.java.util.IoUtils;
import net.java.util.TagValue;
import storm.trident.operation.TridentCollector;
import storm.trident.spout.ITridentSpout;
import storm.trident.topology.TransactionAttempt;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-7-1
 * Time: 下午6:41
 * To change this template use File | Settings | File Templates.
 */
public class FixEventEmitter implements ITridentSpout.Emitter<Long>,
        Serializable {
    private static final long serialVersionUID = 1L;
    public static AtomicInteger successfulTransactions = new AtomicInteger(0);
    public static AtomicInteger uids = new AtomicInteger(0);

    @SuppressWarnings("rawtypes")
    @Override
    public void emitBatch(TransactionAttempt tx, Long coordinatorMeta, TridentCollector collector) {
        InputStream inputStream = null;
        File file = new File("fix_data.txt");
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            SimpleFixParser parser = new SimpleFixParser(inputStream);
            SimpleFixMessage msg = null;
            do {
                msg = parser.readFixMessage();
                if (null != msg) {
                    FixMessageDto dto = new FixMessageDto();
                    for (TagValue tagValue : msg.fields()) {
                        if (tagValue.tag().equals("6")) { //AvgPx
                            // dto.price = Double.valueOf((String) tagValue.value());
                            dto.price = new Double((int)(Math.random() * 100));
                        } else if (tagValue.tag().equals("35")) {
                            dto.msgType = (String) tagValue.value();
                        } else if (tagValue.tag().equals("55")) {
                            dto.symbol = (String) tagValue.value();
                        } else if (tagValue.tag().equals("11")) {
                            // dto.uid = (String) tagValue.value();
                            dto.uid = Integer.toString(uids.incrementAndGet());
                        }
                    }
                    new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(dto);
                    List<Object> message = new ArrayList<Object>();
                    message.add(dto);
                    collector.emit(message);
                }
            } while (msg != null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IoUtils.closeSilently(inputStream);
        }
    }

    @Override
    public void success(TransactionAttempt tx) {
        successfulTransactions.incrementAndGet();
    }

    @Override
    public void close() {
    }
}
