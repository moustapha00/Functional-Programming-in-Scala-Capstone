import observatory.Extraction.*
import observatory.Visualization.*
import observatory.Interaction.*
import observatory.{Color, Temperature}

import java.io.File
import observatory.Visualization._
import com.sksamuel.scrimage.implicits.given


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

// Visualization
val image = visualize(averageRecords, colorScale)

// Save the image
image.output(new java.io.File("some-image.png"))