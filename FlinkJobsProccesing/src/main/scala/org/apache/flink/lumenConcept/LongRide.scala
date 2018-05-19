package org.apache.flink.lumenConcept

import com.dataartisans.flinktraining.exercises.datastream_java.datatypes.TaxiRide
import com.dataartisans.flinktraining.exercises.datastream_java.sources.TaxiRideSource
import com.dataartisans.flinktraining.exercises.datastream_java.utils.GeoUtils
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.{TimeCharacteristic, TimerService}
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object LongRide {
  def main(args: Array[String]) {

    // parse parameters
//    val params = ParameterTool.fromArgs(args)
//    val input = params.getRequired("input")

    val maxDelay = 60     // events are out of order by max 60 seconds
    val speed = 1800      // events of 30 minutes are served every second

    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    // operate in Event-time
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val rides = env.addSource(new TaxiRideSource("/Users/Usuario/Downloads/nycTaxiRides.gz", maxDelay, speed))

    val longRides = rides
      // remove all rides which are not within NYC
      .filter { r => GeoUtils.isInNYC(r.startLon, r.startLat) && GeoUtils.isInNYC(r.endLon, r.endLat) }
      .keyBy(_.rideId)
      .process(new MatchFunction())

    longRides.print()

    env.execute("Long Taxi Rides")
  }

  class MatchFunction extends ProcessFunction[TaxiRide, TaxiRide] {
    // keyed, managed state
    // holds an END event if the ride has ended, otherwise a START event
    lazy val rideState: ValueState[TaxiRide] = getRuntimeContext.getState(
      new ValueStateDescriptor[TaxiRide]("saved ride", classOf[TaxiRide]))

    override def processElement(ride: TaxiRide,
                                context: ProcessFunction[TaxiRide, TaxiRide]#Context,
                                out: Collector[TaxiRide]): Unit = {
      val timerService = context.timerService

      if (ride.isStart) {
        // the matching END might have arrived first; don't overwrite it
        if (rideState.value() == null) {
          rideState.update(ride)
        }
      }
      else {
        rideState.update(ride)
      }

      timerService.registerEventTimeTimer(ride.getEventTime + 120 * 60 * 1000)
    }

    override def onTimer(timestamp: Long,
                         ctx: ProcessFunction[TaxiRide, TaxiRide]#OnTimerContext,
                         out: Collector[TaxiRide]): Unit = {
      val savedRide = rideState.value

      if (savedRide != null && savedRide.isStart) {
        out.collect(savedRide)
      }

      rideState.clear()
    }
  }

}
