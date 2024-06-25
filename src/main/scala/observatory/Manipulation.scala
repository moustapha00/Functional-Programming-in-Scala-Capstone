package observatory

import observatory.Visualization.predictTemperature

import scala.collection.parallel.CollectionConverters.given

import scala.collection.mutable


/**
  * 4th milestone: value-added information
  */
object Manipulation extends ManipulationInterface:

  /**
    * @param temperatures Known temperatures
    * @return A function that, given a latitude in [-89, 90] and a longitude in [-180, 179],
    *         returns the predicted temperature at this location
    */
  def makeGrid(temperatures: Iterable[(Location, Temperature)]): GridLocation => Temperature = {
    val grid = mutable.Map[GridLocation, Temperature]()

    for (lat <- -89 to 90; lon <- -180 to 179){
      val gridLocation = GridLocation(lat, lon)
      val location = Location(lat, lon)
      val temperature =  predictTemperature(temperatures, location)
      grid(gridLocation) = temperature
    }
    gridLocation => grid(gridLocation)
  }

  /**
    * @param temperaturess Sequence of known temperatures over the years (each element of the collection
    *                      is a collection of pairs of location and temperature)
    * @return A function that, given a latitude and a longitude, returns the average temperature at this location
    */
  def average(temperaturess: Iterable[Iterable[(Location, Temperature)]]): GridLocation => Temperature = {
    // Step 1: Flatten and aggregate the data by location
    val aggregatedData = temperaturess.flatten
      .groupBy(_._1) // Group by Location
      .view.mapValues { temps =>
        val totalTemp = temps.map(_._2).sum
        val count = temps.size
        totalTemp / count // Calculate average temperature
      }
    makeGrid(aggregatedData)
  }

  /**
    * @param temperatures Known temperatures
    * @param normals A grid containing the “normal” temperatures
    * @return A grid containing the deviations compared to the normal temperatures
    */
  def deviation(temperatures: Iterable[(Location, Temperature)], normals: GridLocation => Temperature): GridLocation => Temperature = {
    val gridFunction = makeGrid(temperatures)
    GridLocation => gridFunction(GridLocation) - normals(GridLocation)
  }



