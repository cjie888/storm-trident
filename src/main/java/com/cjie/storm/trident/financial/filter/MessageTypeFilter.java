package com.cjie.storm.trident.financial.filter;

import com.cjie.storm.trident.financial.dto.FixMessageDto;
import storm.trident.operation.BaseFilter;
import storm.trident.tuple.TridentTuple;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-7-1
 * Time: 下午6:39
 * To change this template use File | Settings | File Templates.
 */
public class MessageTypeFilter extends BaseFilter {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean isKeep(TridentTuple tuple) {
        FixMessageDto message = (FixMessageDto) tuple.getValue(0);
        if (message.msgType.equals("8")) {
            return true;
        }
        return false;
    }
}
