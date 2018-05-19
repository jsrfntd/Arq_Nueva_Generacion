package org.apache.flink.lumenConcept

import com.dataartisans.flinktraining.exercises.datastream_java.datatypes.TaxiFare
import com.dataartisans.flinktraining.exercises.datastream_java.sources.CheckpointedTaxiFareSource
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector

object HourlyTips {
  def main(args: Array[String]) {

    // read parameters
    val params = ParameterTool.fromArgs(args)
    //val input = params.getRequired("input")

    val speed = 600 // events of 10 minutes are served in 1 second

    // set up streaming execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    // start the data generator
    val fares = env.addSource(new CheckpointedTaxiFareSource("/Users/Usuario/Downloads/nycTaxiFares.gz", speed))

    // total tips per hour by driver
    val hourlyTips = fares
      .keyBy(fare => fare.driverId)
      .timeWindow(Time.hours(1))
      .apply { (key: Long, window, fares, out: Collector[(Long, Long, Float)]) =>
        out.collect((window.getEnd, key, fares.map(fare => fare.tip).sum/fares.size))
      }

    // max tip total in each hour
    val hourlyMax = hourlyTips
      .timeWindowAll(Time.hours(1))
      .maxBy(2)

    // print result on stdout
    hourlyMax.print()

    // execute the transformation pipeline
    env.execute("Hourly Tips (scala)")
  }
}
