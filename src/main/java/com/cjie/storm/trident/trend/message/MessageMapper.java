package com.cjie.storm.trident.trend.message;

import storm.trident.tuple.TridentTuple;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-25
 * Time: 上午10:04
 * To change this template use File | Settings | File Templates.
 */
public interface MessageMapper extends Serializable {
    public String toMessageBody(TridentTuple tuple);
}
