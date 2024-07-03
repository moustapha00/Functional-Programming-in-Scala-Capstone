package observatory

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import com.sksamuel.scrimage.metadata.ImageMetadata
import observatory.Interaction.tileLocation
import observatory.Visualization.interpolateColor

import scala.math.*



/**
  * 5th milestone: value-added information visualization
  */
object Visualization2 extends Visualization2Interface:

  /**
    * @param point (x, y) coordinates of a point in the grid cell
    * @param d00 Top-left value
    * @param d01 Bottom-left value
    * @param d10 Top-right value
    * @param d11 Bottom-right value
    * @return A guess of the value at (x, y) based on the four known values, using bilinear interpolation
    *         See https://en.wikipedia.org/wiki/Bilinear_interpolation#Unit_Square
    */
  def bilinearInterpolation(
    point: CellPoint,
    d00: Temperature,
    d01: Temperature,
    d10: Temperature,
    d11: Temperature
  ): Temperature ={
    val CellPoint(x, y) = point
    d00 * (1 - x) * (1 - y) + d10 * x * (1 - y) + d01 * (1 - x) * y + d11 * x * y
  }


  /**
    * @param grid Grid to visualize
    * @param colors Color scale to use
    * @param tile Tile coordinates to visualize
    * @return The image of the tile at (x, y, zoom) showing the grid using the given color scale
    */
  def visualizeGrid(
    grid: GridLocation => Temperature,
    colors: Iterable[(Temperature, Color)],
    tile: Tile
  ): ImmutableImage = {
    val width = 256
    val height = 256
    val alpha = 127
    val pixels =
      for
        py <- 0 until width
        px <- 0 until height
        subTile = Tile(tile.x * 256 + px, tile.y * 256 + py, tile.zoom + 8)
        subTileLocation = tileLocation(subTile)

        // Find the grid points around the location
        latCeil = subTileLocation.lat.ceil.toInt
        lonFloor = subTileLocation.lon.floor.toInt

        latCeilWrapped = if (latCeil + 1 > 90) -89 else latCeil + 1
        lonFloorWrapped = if (lonFloor + 1 > 179) -180 else lonFloor + 1

        d00 = grid(GridLocation(latCeil, lonFloor))
        d01 = grid(GridLocation(latCeil, lonFloorWrapped))
        d10 = grid(GridLocation(latCeilWrapped, lonFloor))
        d11 = grid(GridLocation(latCeilWrapped, lonFloorWrapped))

        // Calculate the relative position within the cell
        cellX = subTileLocation.lon - lonFloor
        cellY = latCeil - subTileLocation.lat

      // Interpolate the temperature at this pixel
        temperature = bilinearInterpolation(CellPoint(cellX, cellY), d00, d01, d10, d11)

        // Determine the color for this temperature
        color = interpolateColor(colors, temperature)

      yield Pixel(px, py, color.red, color.green, color.blue, alpha)
    ImmutableImage.wrapPixels(width, height, pixels.toArray, ImageMetadata.empty)
  }

