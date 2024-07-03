package observatory

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.implicits.given

import scala.collection.parallel.CollectionConverters.given
import scala.math.*
import scala.util.control.Breaks._

/**
  * 2nd milestone: basic visualization
  */
object Visualization extends VisualizationInterface:

  /**
    * @param temperatures Known temperatures: pairs containing a location and the temperature at this location
    * @param location Location where to predict the temperature
    * @return The predicted temperature at `location`
    */
  def predictTemperature(temperatures: Iterable[(Location, Temperature)], location: Location): Temperature = {
    val p = 6
    // Local function to compute the great-circle distance
    def greatCircleDistance(loc1: Location, loc2: Location): Double = {
      val R = 6371 // Earth's radius in kilometers
      val (lat1, lon1) = (toRadians(loc1._1), toRadians(loc1._2))
      val (lat2, lon2) = (toRadians(loc2._1), toRadians(loc2._2))
      val deltaLat = lat2 - lat1
      val deltaLon = lon2 - lon1
      val dDelta = if (deltaLat == 0 && deltaLon == 0) {
        0 // equal points
      } else if (lat1 == -lat2 && abs(lon1 - lon2) == Pi) {
        Pi // antipodes
      } else {
        acos(sin(lat1)*sin(lat2) + cos(lat1)*cos(lat2)*cos(deltaLon))
      }
      R*dDelta
    }

    // Use the local function within the main function
    val (closeLocation, closeTemp) = temperatures.find { case (loc, _) =>
      greatCircleDistance(loc, location) < 1
    }.getOrElse((null, Double.NaN))

    if (closeLocation != null) {
      closeTemp
    } else {
      val weightedSum = temperatures.foldLeft(0.0) {
        case (acc, (loc, temp)) =>
          val dist = greatCircleDistance(loc, location)
          if (dist != 0) {
            val weight = 1 / pow(dist, p)
            acc + (weight * temp)
          } else acc
      }
      val totalWeight = temperatures.foldLeft(0.0) {
        case (acc, (loc, _)) =>
          val dist = greatCircleDistance(loc, location)
          if (dist != 0) acc + (1 / pow(dist, p)) else acc
      }
      weightedSum / totalWeight
    }
  }

  /**
    * @param points Pairs containing a value and its associated color
    * @param value The value to interpolate
    * @return The color that corresponds to `value`, according to the color scale defined by `points`
    */
  def interpolateColor(points: Iterable[(Temperature, Color)], value: Temperature): Color = {
    
    def interpolate(start: Int, end: Int, proportion: Double): Int = {
      (start + (end - start) * proportion).round.toInt
    }
    def interpolateColorValues(lower: (Temperature, Color), upper: (Temperature, Color), value: Temperature): Color = {
      val range = upper._1 - lower._1
      val proportion = (value - lower._1) / range

      val red = interpolate(lower._2.red, upper._2.red, proportion)
      val green = interpolate(lower._2.green, upper._2.green, proportion)
      val blue = interpolate(lower._2.blue, upper._2.blue, proportion)

      Color(red, green, blue)
    }
    
    val sortedPoints = points.toSeq.sortBy(_._1) // Sort points by temperature

    // Initialize variables to hold the bounding points
    var lowerPoint: (Double, Color) = sortedPoints.head // Default to the first point
    var upperPoint: (Double, Color) = sortedPoints.last // Default to the last point

    breakable {
      for (i <- 0 until sortedPoints.length - 1) {
        if (sortedPoints(i)._1 <= value && sortedPoints(i + 1)._1 >= value) {
          lowerPoint = sortedPoints(i)
          upperPoint = sortedPoints(i + 1)
          break // Exit the loop once the correct bounds are found
        }
      }
    }

    // Check if the value is out of the bounds of provided points
    if (value <= lowerPoint._1) {
      lowerPoint._2
    } else if (value >= upperPoint._1) {
      upperPoint._2
    } else {
      // Perform linear interpolation on color
      interpolateColorValues(lowerPoint, upperPoint, value)
    }
  }

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @return A 360×180 image where each pixel shows the predicted temperature at its location
    */
  def visualize(temperatures: Iterable[(Location, Temperature)], colors: Iterable[(Temperature, Color)]): ImmutableImage = {
    // Create an image of 360 by 180 pixels
    val width = 360
    val height = 180
    val alpha = 127
    val pixels =
      for
        y <- 0 until width
        x <- 0 until height
        lon = x - 180 // Convert pixel x to longitude
        lat = 90 - y  // Convert pixel y to latitude

        // Predict the temperature at this location
        temp = predictTemperature(temperatures, Location(lat, lon))
        // Determine the color for this temperature
        color = interpolateColor(colors, temp)
      yield Pixel(x, y, color.red, color.green, color.blue, alpha)
    ImmutableImage.wrapPixels(width, height, pixels.toArray, ImageMetadata.empty)
  }


