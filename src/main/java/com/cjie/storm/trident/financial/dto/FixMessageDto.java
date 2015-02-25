package com.cjie.storm.trident.financial.dto;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-7-1
 * Time: 下午6:45
 * To change this template use File | Settings | File Templates.
 */
public class FixMessageDto {
    public String msgType;
    public Object symbol;
    public Object price;
    public String uid;
}
