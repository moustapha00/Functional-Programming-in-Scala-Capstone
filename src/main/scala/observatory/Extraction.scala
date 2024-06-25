package observatory

import java.time.LocalDate
import scala.io.Source

/**
  * 1st milestone: data extraction
  */
object Extraction extends ExtractionInterface:

  /**
    * @param year             Year number
    * @param stationsFile     Path of the stations resource file to use (e.g. "/stations.csv")
    * @param temperaturesFile Path of the temperatures resource file to use (e.g. "/1975.csv")
    * @return A sequence containing triplets (date, location, temperature)
    */
  def locateTemperatures(year: Year, stationsFile: String, temperaturesFile: String): Iterable[(LocalDate, Location, Temperature)] = {
    val stationsSource = Source.fromInputStream(getClass.getResourceAsStream(stationsFile))
    val stationsMap = stationsSource.getLines().map(_.split(","))
      .filter(p => p.length == 4 && p(2).nonEmpty && p(3).nonEmpty)
      .map(p => ((p(0), p(1)), Location(p(2).toDouble, p(3).toDouble)))
      .toMap
    stationsSource.close()
    val temperatureSource = Source.fromInputStream(getClass.getResourceAsStream(temperaturesFile))
    val records  = temperatureSource.getLines().map(_.split(","))
      .filter(p => p.length == 5)
      .flatMap { p =>
        val stationKey = (p(0), p(1))
        stationsMap.get(stationKey).map { location =>
          val month = p(2).toInt
          val day = p(3).toInt
          val fahrenheit = p(4).toDouble
          val celsius = (fahrenheit - 32) * 5 / 9
          (LocalDate.of(year, month, day), location, celsius)
        }
      }
      .to(Iterable)
    temperatureSource.close()
    records

  }
  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Temperature)]): Iterable[(Location, Temperature)] = {
    records
      .groupBy(_._2)  // Group by Location, which is the second element of the tuple
      .map { case (location, groupedRecords) =>
        val totalTemperature = groupedRecords.map(_._3).sum  // Sum all temperatures for this location
        val count = groupedRecords.size  // Count the number of records for this location
        (location, totalTemperature / count)  // Calculate the average temperature
      }
  }
    


