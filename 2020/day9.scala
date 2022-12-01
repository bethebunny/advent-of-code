import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(9).toSeq.map(_.toLong)
  def pairs[A](s: Seq[A]): Seq[(A, A)] = s.tails.map{
    case x::xs => xs.map((x, _))
    case Nil => Seq()
  }.flatten.toSeq
  def isSumOfTwo(s: Seq[Long], v: Long): Boolean = pairs(s).exists{ case (a, b) => a + b == v}
  println(
    data.sliding(26).filter(window => !isSumOfTwo(window.init, window.last)).next().last
  )
  def contiguousSubSeqs[A](s: Seq[A]): Iterator[Seq[A]] = s.inits.map(_.tails).flatten
  println(contiguousSubSeqs(data).filter(l => l.size > 1 & l.sum == 1721308972).map(l => l.min + l.max).next())
}
