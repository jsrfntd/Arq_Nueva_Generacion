package org.apache.flink.lumenConcept.jobs

import java.util.{Date, Properties}
import com.dataartisans.flinktraining.exercises.datastream_java.utils.GeoUtils
import o.a.f.l.schemas.SensorEventSchema
import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.lumenConcept.RideCleansingToKafka
import org.apache.flink.lumenConcept.model.SensorEvent
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer011, FlinkKafkaProducer011}
import org.apache.flink.util.Collector

object SensorEventFromKafka {

  private val ZOOKEEPER_HOST = "18.204.96.185:8088"
  private val KAFKA_BROKER = "18.204.96.185:8089"
  private val SENSOR_EVENTS_TOPIC= "lumenconcept.telemetry"
  private val RIDE_SPEED_GROUP = "flink.consumer3"

  private val LOCAL_ZOOKEEPER_HOST = "localhost:2181"
  private val LOCAL_KAFKA_BROKER = "localhost:9092"
  private val EVENTS_TOPIC_RESPONSE = "cleansedRidesScalaResponse"
  private val MAX_EVENT_DELAY = 0 // events are out of order by max 60 seconds

  def main(args: Array[String]) {

    //val popThreshold = 20 // threshold for popular places

    // set up streaming execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    // configure Kafka consumer
    val kafkaProps = new Properties
    kafkaProps.setProperty("zookeeper.connect", ZOOKEEPER_HOST)
    kafkaProps.setProperty("bootstrap.servers", KAFKA_BROKER)
    kafkaProps.setProperty("group.id", RIDE_SPEED_GROUP)
    // always read the Kafka topic from the start
    kafkaProps.setProperty("auto.offset.reset", "earliest")

    // create a Kafka consumer
    val consumer = new FlinkKafkaConsumer011[SensorEvent](
      SENSOR_EVENTS_TOPIC,
      new SensorEventSchema,
      kafkaProps)
    // configure timestamp and watermark assigner
    consumer.assignTimestampsAndWatermarks(new SensorEventTSAssigner)

    // create a Kafka source
    val sensorEvents = env.addSource(consumer)

    // find popular places
//    val metricEvents = sensorEvents
//      // match ride to grid cell and event type (start or end)
//      .map { x => (x.id, x.eventTime, x.sensorId,x.measurementValue)}
//      // partition by cell id and event type
//      .keyBy(2)
//      // build sliding window
//      .windowAll(TumblingEventTimeWindows.of(Time.seconds(5)))
//      .max(3)


    val metricEvents = sensorEvents.keyBy(event=>event.sensorId)
    .timeWindow(Time.seconds(5))
    .apply { (key: String, window, events, out: Collector[(String, String, Float)]) =>
      out.collect((window.getEnd).toString, key, events.map(fare => fare.measurementValue.floatValue()).sum/events.size)
    }.filter( c => { c._3>= 45.0 } )

    // print result on stdout
    metricEvents.map(new TransformSensorEvent).addSink(
        new FlinkKafkaProducer011[SensorEvent](
          LOCAL_KAFKA_BROKER,
          EVENTS_TOPIC_RESPONSE,
          new SensorEventSchema()))

    // execute the transformation pipeline
    env.execute("Popular Places from Kafka")
  }

  /**
    * Assigns timestamps to TaxiRide records.
    * Watermarks are periodically assigned, a fixed time interval behind the max timestamp.
    */
  class SensorEventTSAssigner
    extends BoundedOutOfOrdernessTimestampExtractor[SensorEvent](Time.seconds(MAX_EVENT_DELAY)) {

    override def extractTimestamp(event: SensorEvent): Long = {
      event.eventTime.getMillis
    }
  }


  class TransformSensorEvent extends MapFunction[(String, String, Float), SensorEvent] {
    def map(event: (String, String, Float)): SensorEvent = {
      val sensorEvent=SensorEvent.fromString2(event._1+","+event._2+","+event._3.toString)
      return sensorEvent
    }
  }

  /**
    * Map taxi ride to grid cell and event type.
    * Start records use departure location, end record use arrival location.
    */
//  class GridCellMatcher extends MapFunction[SensorEvent, (Int, Boolean)] {
//
//    def map(taxiRide: SensorEvent): (Int, Boolean) = {
//      if (taxiRide.isStart) {
//        // get grid cell id for start location
//        val gridId: Int = GeoUtils.mapToGridCell(taxiRide.startLon, taxiRide.startLat)
//        (gridId, true)
//      } else {
//        // get grid cell id for end location
//        val gridId: Int = GeoUtils.mapToGridCell(taxiRide.endLon, taxiRide.endLat)
//        (gridId, false)
//      }
//    }
//  }

  /**
    * Maps the grid cell id back to longitude and latitude coordinates.
    */
  class GridToCoordinates extends MapFunction[
    (Int, Long, Boolean, Int),
    (Float, Float, Long, Boolean, Int)] {

    def map(cellCount: (Int, Long, Boolean, Int)): (Float, Float, Long, Boolean, Int) = {
      val longitude = GeoUtils.getGridCellCenterLon(cellCount._1)
      val latitude = GeoUtils.getGridCellCenterLat(cellCount._1)
      (longitude, latitude, cellCount._2, cellCount._3, cellCount._4)
    }
  }
}
