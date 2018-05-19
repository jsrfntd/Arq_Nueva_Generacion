package org.apache.flink.lumenConcept

import com.dataartisans.flinktraining.exercises.datastream_java.datatypes.{TaxiFare, TaxiRide}
import com.dataartisans.flinktraining.exercises.datastream_java.sources.{CheckpointedTaxiFareSource, CheckpointedTaxiRideSource}
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector


object JoinRidesWithFaresExpiring {
  val unmatchedRides = new OutputTag[TaxiRide]("unmatchedRides") {}
  val unmatchedFares = new OutputTag[TaxiFare]("unmatchedFares") {}

  def main(args: Array[String]) {

    // parse parameters
    val params = ParameterTool.fromArgs(args)
//    val ridesFile = params.getRequired("rides")
//    val faresFile = params.getRequired("fares")

    val servingSpeedFactor = 1800 // 30 minutes worth of events are served every second

    // set up streaming execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val rides = env
      .addSource(new CheckpointedTaxiRideSource("/Users/Usuario/Downloads/nycTaxiRides.gz", servingSpeedFactor))
      .filter { ride => !ride.isStart && (ride.rideId % 1000 != 0) }
      .keyBy("rideId")

    val fares = env
      .addSource(new CheckpointedTaxiFareSource("/Users/Usuario/Downloads/nycTaxiFares.gz", servingSpeedFactor))
      .keyBy("rideId")

    val processed = rides.connect(fares).process(new EnrichmentFunction)

    processed.getSideOutput[TaxiFare](unmatchedFares).print
    processed.getSideOutput[TaxiRide](unmatchedRides).print

    env.execute("Join Rides with Fares (scala ProcessFunction)")
  }

  class EnrichmentFunction extends CoProcessFunction[TaxiRide, TaxiFare, (TaxiRide, TaxiFare)] {
    // keyed, managed state
    lazy val rideState: ValueState[TaxiRide] = getRuntimeContext.getState(
      new ValueStateDescriptor[TaxiRide]("saved ride", classOf[TaxiRide]))
    lazy val fareState: ValueState[TaxiFare] = getRuntimeContext.getState(
      new ValueStateDescriptor[TaxiFare]("saved fare", classOf[TaxiFare]))

    override def processElement1(ride: TaxiRide,
                                 context: CoProcessFunction[TaxiRide, TaxiFare, (TaxiRide, TaxiFare)]#Context,
                                 out: Collector[(TaxiRide, TaxiFare)]): Unit = {
      val fare = fareState.value
      if (fare != null) {
        fareState.clear()
        out.collect((ride, fare))
      }
      else {
        rideState.update(ride)
        // as soon as the watermark arrives, we can stop waiting for the corresponding fare
        context.timerService.registerEventTimeTimer(ride.getEventTime)
      }
    }

    override def processElement2(fare: TaxiFare,
                                 context: CoProcessFunction[TaxiRide, TaxiFare, (TaxiRide, TaxiFare)]#Context,
                                 out: Collector[(TaxiRide, TaxiFare)]): Unit = {
      val ride = rideState.value
      if (ride != null) {
        rideState.clear()
        out.collect((ride, fare))
      }
      else {
        fareState.update(fare)
        // wait up to 6 hours for the corresponding ride END event, then clear the state
        context.timerService.registerEventTimeTimer(fare.getEventTime + 6 * 60 * 60 * 1000)
      }
    }

    override def onTimer(timestamp: Long,
                         ctx: CoProcessFunction[TaxiRide, TaxiFare, (TaxiRide, TaxiFare)]#OnTimerContext,
                         out: Collector[(TaxiRide, TaxiFare)]): Unit = {
      if (fareState.value != null) {
        ctx.output(unmatchedFares, fareState.value)
        fareState.clear()
      }
      if (rideState.value != null) {
        ctx.output(unmatchedRides, rideState.value)
        rideState.clear()
      }
    }
  }
}
