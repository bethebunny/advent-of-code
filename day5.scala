import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(5).toSeq

  def range(a: Int, b: Int) = if (a < b) a to b else b to a

  case class Point(x: Int, y: Int) {
    def -(p: Point): Point = Point(x - p.x, y - p.y)
    def times(v: Int): Point = Point(x * v, y * v)
  }
  case class Line(start: Point, end: Point) {
    def points: Seq[Point] = {
      val xs = start.x to end.x by (if (start.x < end.x) 1 else -1)
      val ys = start.y to end.y by (if (start.y < end.y) 1 else -1)
      xs.zipAll(ys, xs.head, ys.head).map { case (x, y) => Point(x, y) }
    }
    def isDiagonal: Boolean = start.x != end.x && start.y != end.y
  }

  object Line {
    val lineRE = raw"(\d+),(\d+) -> (\d+),(\d+)".r
    def parse(s: String): Line = s match {
      case lineRE(x1, y1, x2, y2) => Line(Point(x1.toInt, y1.toInt), Point(x2.toInt, y2.toInt))
    }
  }

  val lines = data.map(Line.parse)

  println(lines.filter(!_.isDiagonal).flatMap(_.points).groupBy(identity).map(_._2.size).count(_ > 1))
  println(lines.flatMap(_.points).groupBy(identity).map(_._2.size).count(_ > 1))
}
