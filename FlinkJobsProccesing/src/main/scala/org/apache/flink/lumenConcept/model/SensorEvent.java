package org.apache.flink.lumenConcept.model;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Objects;

public class SensorEvent implements Comparable<SensorEvent> {

    private static transient DateTimeFormatter timeFormatter =
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.US).withZoneUTC();

    public DateTime eventTime;
    public String sensorId;
    public Float measurementValue;

    public SensorEvent() {

    }

    public SensorEvent( String sensorId,DateTime timeEvent, Float measurementValue) {
        this.eventTime = timeEvent;
        this.sensorId = sensorId;
        this.measurementValue = measurementValue;

    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(eventTime.toString(timeFormatter)).append(",");
        sb.append(sensorId).append(",");
        sb.append(measurementValue);
        return sb.toString();
    }

    public static SensorEvent fromString(String trama) {
        JSONObject obj = new JSONObject(trama);
        String value=null;
        String sensorId=null;
        String dateTime=null;
        SensorEvent sensorEvent = new SensorEvent();
        JSONArray arr = obj.getJSONArray("measurements");
        for (int i = 0; i < arr.length(); i++) {
            String id = arr.getJSONObject(i).get("id").toString();
            if (id.equals("H")) {
                value = arr.getJSONObject(i).get("value").toString();
                sensorId=id;
                dateTime=proccessDate(obj.get("datetime").toString());
                break;
            }
        }
        try {
            sensorEvent.eventTime = DateTime.parse(dateTime, timeFormatter);
            sensorEvent.sensorId = sensorId;
            sensorEvent.measurementValue = Float.parseFloat(value);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException("Invalid record: " + trama, nfe);
        } catch (IllegalArgumentException ie){
            throw new RuntimeException("Invalid record: " + trama, ie);
        }
        return sensorEvent;
    }


    public static SensorEvent fromString2(String trama) {
        String value=null;
        String sensorId=null;
        String dateTime=null;
        String[] tokensTrama=trama.split(",");
        SensorEvent sensorEvent = new SensorEvent();
        sensorEvent.eventTime = DateTime.now();
        sensorEvent.sensorId = tokensTrama[1];
        sensorEvent.measurementValue = Float.parseFloat(tokensTrama[2]);
        return sensorEvent;
    }

    private static String proccessDate(String unformatedDate){
        System.out.println("------------------==================="+unformatedDate);
        String[] tokensGeneralDate = unformatedDate.split("_");
        String[] tokensDate=tokensGeneralDate[0].split("-");
        if(tokensDate[0].length()==1){
            tokensDate[0]="0"+tokensDate[0];
        }
        if(tokensDate[1].length()==1){
            tokensDate[1]="0"+tokensDate[1];
        }
        String[] tokensTime=tokensGeneralDate[1].split(":");
        if(tokensTime[0].length()==1){
            tokensTime[0]="0"+tokensTime[0];
        }
        if(tokensTime[1].length()==1){
            tokensTime[1]="0"+tokensTime[1];
        }
        if(tokensTime[2].length()==1){
            tokensTime[2]="0"+tokensTime[2];
        }
        String formatedDate=tokensDate[2]+"-"+tokensDate[1]+"-"+tokensDate[0]+" "+tokensTime[0]+":"+tokensTime[1]+":"+tokensTime[2];
        return formatedDate;
    }

    // sort by timestamp,
    // putting START events before END events if they have the same timestamp
    public int compareTo(SensorEvent other) {
        if (other == null) {
            return 1;
        }
        int compareTimes = Long.compare(this.getEventTime(), other.getEventTime());
        return compareTimes;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SensorEvent &&
                this.sensorId.equals(((SensorEvent) other).sensorId)  &&
                this.eventTime.equals(((SensorEvent) other).eventTime) &&
                this.measurementValue.equals(((SensorEvent) other).measurementValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventTime, sensorId, measurementValue);
    }

    public long getEventTime() {
        return eventTime.getMillis();
    }
}
