import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(6).toSeq
  case class Fish(timer: Int) {
    def update: Seq[Fish] = timer match {
      case 0 => Seq(Fish(6), Fish(8))
      case _ => Seq(Fish(timer - 1))
    }
  }

  val fishes = data(0).split(",").map(timer => Fish(timer.toInt))

  println((1 to 80).foldLeft(fishes) {
    case (fishes, day) => fishes.flatMap(_.update)
  }.size)

  case class FishPop(counts: Map[Int, Long]) {
    def update: FishPop = FishPop(
      Map(
        8 -> counts.getOrElse(0, 0L),
        7 -> counts.getOrElse(8, 0L),
        6 -> (counts.getOrElse(7, 0L) + counts.getOrElse(0, 0L)),
      ) ++ (0 to 5).map(timer => (timer -> counts.getOrElse(timer + 1, 0L))).toMap
    )
  }

  val fishPop = FishPop(fishes.map(_.timer).groupBy(identity).mapValues(_.size.toLong).toMap)

  println((1 to 256).foldLeft(fishPop) {
    case (fishPop, day) => fishPop.update
  }.counts.values.sum)
}
