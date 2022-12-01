import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(9).toSeq
  def neighbors(m: Map[(Int, Int), Int], p: (Int, Int)): Seq[(Int, Int)] = p match {
    case (x, y) => Seq((1, 0), (-1, 0), (0, 1), (0, -1)).map{
      case (dx, dy) => (x + dx, y + dy)
    }.filter(m.contains _)
  }

  val heightMap: Map[(Int, Int), Int] = data.zipWithIndex.flatMap{
    case (hs, y) => hs.zipWithIndex.map{
      case (h, x) => ((x.toInt, y.toInt), h.toString.toInt)
    }
  }.toMap

  val lowPoints = heightMap.filter{
    case (p, h) => neighbors(heightMap, p).map(heightMap).forall(_ > h)
  }

  println(lowPoints.view.values.map(_ + 1).sum)

  // problem is very poorly specified here
  // instead of deciding whether a point is part of a basin or which
  // assume that 9s partition the space into basins and there will always
  // be _exactly_ 1 low point in a partition, and that will correspond to a basin
  // so really we want to compute connected components of points which are not 9
  
  val nonNine = heightMap.filter { case (p, h) => h != 9 }.keySet

  implicit def connectedNeighbors(p: (Int, Int)): Seq[(Int, Int)] =
    neighbors(heightMap, p).filter(nonNine.contains _)

  def connected[T](start: Set[T], unconnected: Set[T])(implicit neighbors: T => Seq[T]): Set[T] = {
    val expanded = start ++ start.flatMap(neighbors).filter(unconnected.contains _)
    if (start == expanded) start else connected(expanded, unconnected)
  }
  def connectedComponents[T](unconnected: Set[T])(implicit neighbors: T => Seq[T]): Seq[Set[T]] =
    if (unconnected.isEmpty) Seq() else {
      val next = unconnected.head
      val connectedToNext = connected(Set(next), unconnected - next)
      Seq(connectedToNext) ++ connectedComponents(unconnected -- connectedToNext)
    }

  println(connectedComponents(nonNine).map(_.size).sorted.takeRight(3).product)
}
