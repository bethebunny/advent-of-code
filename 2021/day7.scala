import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(7).toSeq
  val poss = data(0).split(",").map(_.toLong)
  def median(s: Seq[Long]): Long = {
    val sorted = s.sorted
    if (s.size % 2 == 1) {
      sorted(s.size / 2)
    } else {
      val x1 = sorted(s.size / 2)
      val x2 = sorted(s.size / 2 + 1)
      (x1 + x2) / 2L
    }
  }
  val pMedian = median(poss)
  println(poss.map(p => (p - pMedian).abs).sum)

  def triangle(n: Long): Long = (n * (n + 1L)) / 2L

  println(
    (poss.min to poss.max).map(center =>
        poss.map(p => triangle((p - center).abs)).sum
    ).min
  )
}
