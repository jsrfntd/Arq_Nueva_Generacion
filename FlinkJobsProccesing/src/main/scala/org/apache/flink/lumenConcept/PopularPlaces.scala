package org.apache.flink.lumenConcept

import com.dataartisans.flinktraining.exercises.datastream_java.datatypes.TaxiRide
import com.dataartisans.flinktraining.exercises.datastream_java.sources.TaxiRideSource
import com.dataartisans.flinktraining.exercises.datastream_java.utils.GeoUtils
import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector

/**
  * Scala reference implementation for the "Popular Places" exercise of the Flink training
  * (http://training.data-artisans.com).
  *
  * The task of the exercise is to identify every five minutes popular areas where many taxi rides
  * arrived or departed in the last 15 minutes.
  *
  * Parameters:
  * -input path-to-input-file
  *
  */
object PopularPlaces {

  def main(args: Array[String]) {

    // read parameters
//    val params = ParameterTool.fromArgs(args)
//    val input = params.getRequired("input")

    val popThreshold = 20 // threshold for popular places
    val maxDelay = 60     // events are out of order by max 60 seconds
    val speed = 600       // events of 10 minutes are served in 1 second

    // set up streaming execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    // start the data generator
    val rides = env.addSource(new TaxiRideSource("/Users/Usuario/Downloads/nycTaxiRides.gz", maxDelay, speed))

    // find n most popular spots
    val popularPlaces = rides
      // remove all rides which are not within NYC
      .filter { r => GeoUtils.isInNYC(r.startLon, r.startLat) && GeoUtils.isInNYC(r.endLon, r.endLat) }
      // match ride to grid cell and event type (start or end)
      .map(new GridCellMatcher)
      // partition by cell id and event type
      .keyBy( k => k )
      // build sliding window
      .timeWindow(Time.minutes(15), Time.minutes(5))
      // count events in window
      .apply{ (key: (Int, Boolean), window, values, out: Collector[(Int, Long, Boolean, Int)]) =>
      out.collect( (key._1, window.getEnd, key._2, values.size) )
    }
      // filter by popularity threshold
      .filter( c => { c._4 >= popThreshold } )
      // map grid cell to coordinates
      .map(new GridToCoordinates)

    // print result on stdout
    popularPlaces.print()

    // execute the transformation pipeline
    env.execute("Popular Places")
  }

  /**
    * Map taxi ride to grid cell and event type.
    * Start records use departure location, end record use arrival location.
    */
  class GridCellMatcher extends MapFunction[TaxiRide, (Int, Boolean)] {

    def map(taxiRide: TaxiRide): (Int, Boolean) = {
      if (taxiRide.isStart) {
        // get grid cell id for start location
        val gridId: Int = GeoUtils.mapToGridCell(taxiRide.startLon, taxiRide.startLat)
        (gridId, true)
      } else {
        // get grid cell id for end location
        val gridId: Int = GeoUtils.mapToGridCell(taxiRide.endLon, taxiRide.endLat)
        (gridId, false)
      }
    }
  }

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