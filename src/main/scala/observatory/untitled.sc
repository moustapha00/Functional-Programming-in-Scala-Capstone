import observatory.Extraction.*
import observatory.Visualization.*
import observatory.Interaction.*
import observatory.Tile
import observatory.{Color, Location, Temperature, Tile, Year}

import java.io.File
import observatory.Visualization.*
import com.sksamuel.scrimage.implicits.given
import com.sksamuel.scrimage.nio.JpegWriter

import java.nio.file.{Files, Paths}



val year = 1975
val temperaturesFile = "/1975.csv"
val stationsFile = "/stations.csv"


val colorScale: Iterable[(Temperature, Color)] = Iterable(
  (60, Color(255, 255, 255)),
  (32, Color(255, 0, 0)),
  (12, Color(255, 255, 0)),
  (0, Color(0, 255, 255)),
  (-15, Color(0, 0, 255)),
  (-27, Color(255, 0, 255)),
  (-50, Color(33, 0, 107)),
  (-60, Color(0, 0, 0))
)


// Extraction
val records = locateTemperatures(year, stationsFile, temperaturesFile)
val averageRecords  = locationYearlyAverageRecords(records)

// Yearly average records Iterable
val yearlyData: Iterable[(Year, Iterable[(Location, Temperature)])] = Iterable((year, averageRecords))

// Visualization
// val image = visualize(averageRecords, colorScale)

// Save the image
// image.output(new java.io.File("someImage.png"))
// image.output(JpegWriter.Default, new File("C:/Users/Moustapha/Downloads/someImage.png"))


def generateImage(year: Year, tile: Tile, averageRecords: Iterable[(Location, Temperature)]): Unit = {
  val image = observatory.Interaction.tile(averageRecords, colorScale, tile)
  // Construct the directory path string
  val directoryPath = f"C:/Users/Moustapha/Neurostack/Repositories/observatory/target/temperatures/${year}/${tile.zoom}"

  // Create a Path object
  val path = Paths.get(directoryPath)

  Files.createDirectories(path)

  image.output(new java.io.File(f"C:/Users/Moustapha/Neurostack/Repositories/observatory/target/temperatures/${year}/${tile.zoom}/${tile.x}-${tile.y}.png"))
}

generateTiles(yearlyData, generateImage)