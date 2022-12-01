import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(12).toSeq
  val edgeRE = raw"(.*)-(.*)".r
  val edges = data.flatMap{
    case edgeRE(left, right) => Seq((left, right), (right, left))
  }.groupBy(_._1).mapValues(_.map(_._2)).toMap
  def isBigRoom(room: String): Boolean = room == room.toUpperCase

  def numPaths(
    start: String,
    end: String,
    smallRoomsVisited: Set[String] = Set()
  ): Int = if (start == end) 1 else {
    val nowVisited = smallRoomsVisited ++ (if (isBigRoom(start)) Seq() else Seq(start))
    edges(start).filter(!smallRoomsVisited.contains(_)).map(
      room => numPaths(room, end, nowVisited)
    ).sum
  }

  println(numPaths("start", "end"))

  def numPathsVisitingOneSmallRoomTwice(
    start: String,
    end: String,
    visitedASmallRoomTwice: Boolean = false,
    smallRoomsVisited: Set[String] = Set()
  ): Int = if (start == end) 1 else {
    if (visitedASmallRoomTwice) numPaths(start, end, smallRoomsVisited) else {
      val nowVisited = smallRoomsVisited ++ (if (isBigRoom(start)) Seq() else Seq(start))
      edges(start).filter(_ != "start").map(room => numPathsVisitingOneSmallRoomTwice(
        room, end, smallRoomsVisited.contains(room), nowVisited
      )).sum
    }
  }

  println(numPathsVisitingOneSmallRoomTwice("start", "end"))
}
