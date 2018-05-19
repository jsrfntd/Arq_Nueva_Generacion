package org.apache.flink.lumenConcept.jobs

import java.util.{Date, Properties}
import java.util.concurrent.TimeUnit
import o.a.f.l.schemas.KafkaStringSchema
import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer011, FlinkKafkaProducer011}

object RequestEventsFromKafka {

  import IpCount._
  private val ZOOKEEPER_HOST = "18.204.96.185:8088"
  private val KAFKA_BROKER = "18.204.96.185:8089"
  private val EVENTS_TOPIC= "lumenconcept.request"
  private val EVENTS_TOPIC_RESPONSE = "lumenconcept.dos"
  private val MAX_EVENT_DELAY = 0
  private val GROUP="flink.dosConsumer";

  val stopWords = Set("a", "an", "the")
  val window = Time.of(10, TimeUnit.SECONDS)

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val kafkaProps = new Properties
    kafkaProps.setProperty("zookeeper.connect", ZOOKEEPER_HOST)
    kafkaProps.setProperty("bootstrap.servers", KAFKA_BROKER)
    kafkaProps.setProperty("group.id", GROUP)
    kafkaProps.setProperty("auto.offset.reset", "earliest")
    val kafkaConsumer = new FlinkKafkaConsumer011[String](
      EVENTS_TOPIC,
      KafkaStringSchema ,
      kafkaProps)
    kafkaConsumer.assignTimestampsAndWatermarks(new EventTSAssigner)
    val lines = env.addSource(kafkaConsumer)
    val wordCounts = countIps(lines, stopWords, window)
    wordCounts.map(new JsonParser)
      .addSink(new FlinkKafkaProducer011[String](
        KAFKA_BROKER,
        EVENTS_TOPIC_RESPONSE,
        KafkaStringSchema))
    env.execute()
  }

  class JsonParser extends MapFunction[(String, Int), String] {
    def map(event: (String,Int)): String = "{\"ip\":\"" +event._1 + "\",\"frequency\":" + event._2.toString + ",\"timestamp\":\""+
      new Date().getTime+"\"}"
  }

  class EventTSAssigner
    extends BoundedOutOfOrdernessTimestampExtractor[String](Time.seconds(MAX_EVENT_DELAY)) {
    def extractTimestamp(event: String): Long = {
      new Date().getTime
    }
  }
}

