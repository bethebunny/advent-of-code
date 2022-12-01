import advent_of_code.data.{dataForDay, sessionID}
import scala.collection.mutable


object Main extends App {
  val data = dataForDay(10).toSeq.map(_.toInt)
  val deltas = (data ++ Seq(0, data.max + 3)).sorted.sliding(2).map{ case x::y::Nil => y - x }.toSeq
  println(deltas.count(_ == 1) * deltas.count(_ == 3))
  def numArrangements(s: Seq[Int], maxDelta: Int = 3)(implicit cache: mutable.Map[Seq[Int], Long] = mutable.Map()): Long = {
    // Number of arrangements which definitely contain the first and last element
    if (!cache.contains(s)) {
      val result = s match {
        case x::Nil => 1
        case Nil => 0
        case _ => s.tail.tails
          .filter(!_.isEmpty)
          .filter(l => l.head - s.head <= maxDelta)
          .map(numArrangements(_))
          .sum
      }
      cache += (s -> result)
    }
    cache(s)
  }
  val data2 = Seq(16, 10, 15, 5, 1, 11, 7, 19, 6, 12, 4)
  val data3 = Seq(28, 33, 18, 42, 31, 14, 46, 20, 48, 47, 24, 23, 49, 45, 19, 38, 39, 11, 1, 32, 25, 35, 8, 17, 7, 9, 4, 2, 34, 10, 3)
  println(numArrangements((data2 ++ Seq(0, data2.max + 3)).sorted))
  println(numArrangements((data3 ++ Seq(0, data3.max + 3)).sorted))
  println(numArrangements((data ++ Seq(0, data.max + 3)).sorted))
}
