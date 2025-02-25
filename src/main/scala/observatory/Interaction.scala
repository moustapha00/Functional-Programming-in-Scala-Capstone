package observatory

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import com.sksamuel.scrimage.metadata.ImageMetadata

import scala.collection.parallel.CollectionConverters.given
import scala.math.*
import observatory.Visualization._
/**
  * 3rd milestone: interactive visualization
  */
object Interaction extends InteractionInterface:

  /**
    * @param tile Tile coordinates
    * @return The latitude and longitude of the top-left corner of the tile, as per http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    */
  def tileLocation(tile: Tile): Location = {
    val n = pow(2, tile.zoom)
    val lonDeg = tile.x / n*360.0 - 180.0
    val latRad = atan(sinh(Pi * (1 - 2 * tile.y/n)))
    val latDeg = latRad * 180.0/Pi
    Location(latDeg, lonDeg)
  }

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @param tile Tile coordinates
    * @return A 256×256 image showing the contents of the given tile
    */
  def tile(temperatures: Iterable[(Location, Temperature)], colors: Iterable[(Temperature, Color)], tile: Tile): ImmutableImage = {
    val width = 256
    val height = 256
    val alpha = 127
    val pixels =
      for
        py <- 0 until width
        px <- 0 until height
        subTile = Tile(tile.x * 256 + px, tile.y * 256 + py, tile.zoom + 8)
        subTileLocation = tileLocation(subTile)
        // Predict the temperature at this location
        temp = predictTemperature(temperatures, subTileLocation)
        // Determine the color for this temperature
        color = interpolateColor(colors, temp)
      yield Pixel(px, py, color.red, color.green, color.blue, alpha)
    ImmutableImage.wrapPixels(width, height, pixels.toArray, ImageMetadata.empty)
    
  }

  /**
    * Generates all the tiles for zoom levels 0 to 3 (included), for all the given years.
    * @param yearlyData Sequence of (year, data), where `data` is some data associated with
    *                   `year`. The type of `data` can be anything.
    * @param generateImage Function that generates an image given a year, a zoom level, the x and
    *                      y coordinates of the tile and the data to build the image from
    */
  def generateTiles[Data](
    yearlyData: Iterable[(Year, Data)],
    generateImage: (Year, Tile, Data) => Unit
  ): Unit = {
    val zoomLevels = 0 to 1 // 0 to 3 is default
    yearlyData.foreach { case (year, data) =>
      for (zoom <- zoomLevels; x <- 0 until (1 << zoom); y <- 0 until (1 << zoom)) {
        val tile = Tile(x, y, zoom)
        generateImage(year, tile, data)
      }
    }
  }

