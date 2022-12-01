package advent_of_code

import scala.collection.{AbstractIterator, BufferedIterator}
import scala.io.{BufferedSource, Codec, Source}
import java.io.{Closeable, FileInputStream, FileNotFoundException, InputStream, PrintStream, File => JFile}
import java.net.{URI, URL}

package object data {
  type SessionID = String
  implicit val sessionID: SessionID = "53616c7465645f5f5f30cedcd2ba92cc2acd9566fdc8fd949be577528ed5a9bd52671ccef9487398d093cb74a50ed0ed"

  def createBufferedSource(
    inputStream: InputStream,
    bufferSize: Int = Source.DefaultBufSize,
    reset: () => Source = null,
    close: () => Unit = null
  )(implicit codec: Codec): BufferedSource = {
    // workaround for default arguments being unable to refer to other parameters
    val resetFn = if (reset == null) () => createBufferedSource(inputStream, bufferSize, reset, close)(codec) else reset

    new BufferedSource(inputStream, bufferSize)(codec) withReset resetFn withClose close
  }

  def sourceFromURL(url: String)(implicit codec: Codec, sessionID: SessionID): BufferedSource = {
    var connection = new URL(url).openConnection()
    connection.setRequestProperty("Cookie", s"session=$sessionID")
    sourceFromInputStream(connection.getInputStream())(codec)
  }

  def sourceFromInputStream(is: InputStream)(implicit codec: Codec): BufferedSource =
    createBufferedSource(is, reset = () => sourceFromInputStream(is)(codec), close = () => is.close())(codec)

  val baseURL = "https://adventofcode.com/2020/day"

  def dataForDay(day: Int)(implicit sessionID: SessionID): Iterator[String] = {
    val url = s"$baseURL/$day/input"
    val src = sourceFromURL(url)
    src.getLines()
  }

  def rawDataForDay(day: Int)(implicit sessionID: SessionID): String = {
    val url = s"$baseURL/$day/input"
    sourceFromURL(url).mkString
  }
}
