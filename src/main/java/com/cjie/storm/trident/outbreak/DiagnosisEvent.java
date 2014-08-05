package com.cjie.storm.trident.outbreak;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: hucj
 * Date: 14-6-19
 * Time: 上午10:03
 * To change this template use File | Settings | File Templates.
 */
public class DiagnosisEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    public double lat;
    public double lng;
    public long time;
    public String diagnosisCode;

    public DiagnosisEvent(double lat, double lng,
                          long time, String diagnosisCode) {
        super();
        this.time = time;
        this.lat = lat;
        this.lng = lng;
        this.diagnosisCode = diagnosisCode;
    }
}
