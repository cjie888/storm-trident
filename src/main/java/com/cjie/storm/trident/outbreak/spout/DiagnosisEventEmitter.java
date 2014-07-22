package com.cjie.storm.trident.outbreak.spout;

import com.cjie.storm.trident.outbreak.DiagnosisEvent;
import storm.trident.operation.TridentCollector;
import storm.trident.spout.ITridentSpout;
import storm.trident.topology.TransactionAttempt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-19
 * Time: 上午9:59
 * To change this template use File | Settings | File Templates.
 */
public class DiagnosisEventEmitter implements
        ITridentSpout.Emitter<Long>, Serializable {
    private static final long serialVersionUID = 1L;
    AtomicInteger successfulTransactions = new
            AtomicInteger(0);

    @Override
    public void emitBatch(TransactionAttempt tx, Long
            coordinatorMeta, TridentCollector
                                  collector) {
        for (int i = 0; i < 10000; i++) {
            List<Object> events = new ArrayList<Object>();
            double lat =  new Double(-30 + (int) (Math.random() * 75));
            double lng =  new Double(-120 + (int) (Math.random() * 70));
            long time = System.currentTimeMillis();
            String diag = new Integer(320 + (int) (Math.random() * 7)).toString();
            DiagnosisEvent event = new DiagnosisEvent(lat, lng, time, diag);
            events.add(event);
            collector.emit(events);
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
