import advent_of_code.data.{rawDataForDay, sessionID}


object Main extends App {
  val data = rawDataForDay(6).split("\n\n")
  val uniqueAnswers = data.map(_.replaceAll(raw"\s", "")).map(_.toSet)
  println(uniqueAnswers.map(_.size).sum)
  val intersectionAnswers = data.map(_.split("\n").map(_.toSet).reduce(_ intersect _))
  println(intersectionAnswers.map(_.size).sum)
}
