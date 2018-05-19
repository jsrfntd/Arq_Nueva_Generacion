package o.a.f.l.schemas;

import com.dataartisans.flinktraining.exercises.datastream_java.datatypes.TaxiRide;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.lumenConcept.model.SensorEvent;

/**
 * Implements a SerializationSchema and DeserializationSchema for TaxiRide for Kafka data sources and sinks.
 */
public class SensorEventSchema implements DeserializationSchema<SensorEvent>, SerializationSchema<SensorEvent> {

    @Override
    public byte[] serialize(SensorEvent element) {
        return element.toString().getBytes();
    }

    @Override
    public SensorEvent deserialize(byte[] message) {
        return SensorEvent.fromString(new String(message));
    }

    @Override
    public boolean isEndOfStream(SensorEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<SensorEvent> getProducedType() {
        return TypeExtractor.getForClass(SensorEvent.class);
    }
}
