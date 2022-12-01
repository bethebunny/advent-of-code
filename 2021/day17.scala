import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(17).toSeq

  val targetRE = raw"target area: x=(-?\d+)..(-?\d+), y=(-?\d+)..(-?\d+)".r
  def target = data(0) match {
    case targetRE(xmin, xmax, ymin, ymax) => (xmin.toInt to xmax.toInt, ymin.toInt to ymax.toInt)
  }

  def simulate(start: (Int, Int), v: (Int, Int), target: (Range, Range)): (Boolean, Int) =
    (start, v) match {
      case ((_, y), (_, vy)) if vy <= 0 && y < target._2.min => (false, y)
      case ((x, y), _) if (target._1.contains(x) && target._2.contains(y)) => (true, y)
      case ((x, y), (vx, vy)) => {
        val (hitTarget, ymax) = simulate((x + vx, y + vy), ((vx - 1) max 0, vy - 1), target)
        (hitTarget, if (vy == 0) y else ymax)
      }
    }

  println(
    (1 to 100).flatMap(ivx =>
      (1 to 100).map(ivy => simulate((0, 0), (ivx, ivy), target))
    ).filter(_._1).map(_._2).max
  )

  println(
    (15 to 300).flatMap(ivx =>
      (-100 to 1000).map(ivy => simulate((0, 0), (ivx, ivy), target))
    ).count(_._1)
  )
}
