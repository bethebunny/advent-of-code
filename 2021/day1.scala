import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(1).map(_.toInt).toSeq
  def countIncreasing(s: Iterable[Int]): Int =
    s.tail.zip(s.init).count{ case (d2, d1) => d2 > d1 }

  println(countIncreasing(data))
  println(countIncreasing(data.sliding(3).map(_.sum).toSeq))
}

