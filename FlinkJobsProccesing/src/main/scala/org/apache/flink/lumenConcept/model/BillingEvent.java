package org.apache.flink.lumenConcept.model;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Objects;

public class BillingEvent implements Comparable<BillingEvent> {

    public Long eventTime;
    public Integer fraud;

    public BillingEvent() {

    }

    public BillingEvent(int fraud, long timeEvent) {
        this.eventTime = timeEvent;
        this.fraud = fraud;

    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"timestamp\":").append(eventTime).append(",\"fraud\":");
        sb.append(fraud).append("}");
        return sb.toString();
    }

    public static BillingEvent fromString(String trama) {
        JSONObject obj = new JSONObject(trama);
        BillingEvent paymentEvent = new BillingEvent();
        paymentEvent.eventTime=new Long(obj.getString("timestamp"));
        paymentEvent.fraud=Integer.parseInt(obj.getString("fraud"));
        try {
        } catch (NumberFormatException nfe) {
            throw new RuntimeException("Invalid record: " + trama, nfe);
        } catch (IllegalArgumentException ie){
            throw new RuntimeException("Invalid record: " + trama, ie);
        }
        return paymentEvent;
    }

    // sort by timestamp,
    // putting START events before END events if they have the same timestamp
    public int compareTo(BillingEvent other) {
        if (other == null) {
            return 1;
        }
        int compareTimes = Long.compare(this.getEventTime(), other.getEventTime());
        return compareTimes;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BillingEvent &&
                this.eventTime.equals(((BillingEvent) other).eventTime) &&
                this.fraud.equals(((BillingEvent) other).fraud);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fraud, eventTime);
    }

    public long getEventTime() {
        return eventTime;
    }
}
