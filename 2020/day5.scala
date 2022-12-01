import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(5).toSeq
  val seats = data.map((bp: String) => Integer.parseInt(
    bp.replace('F', '0').replace('B', '1').replace('L', '0').replace('R', '1'),
    2,
  ))
  println(seats.max)
  def windows[A](xs: Seq[A], windowSize: Int): Seq[Seq[A]] =
    xs.tails.map(_.take(windowSize)).filter(_.size == windowSize).toSeq
  println(windows(seats.sorted, 2).filter{ case x::y::Nil => x + 1 != y })
}
