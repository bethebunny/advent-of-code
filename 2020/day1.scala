import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  def combinations[A](x: Seq[A], y: Seq[A]): Seq[(A, A)] = x.map(xx => y.map((xx, _))).flatten
  def pairs[A](xs: Seq[A]): Iterator[(A, A)] = xs.tails.map{
    case x::xs => xs.map((x, _))
    case Nil => Seq()
  }.flatten

  def triples[A](xs: Seq[A]): Iterator[(A, A, A)] = xs.tails.map{
    case x::xs => pairs(xs).map(p => (x, p._1, p._2))
    case Nil => Seq()
  }.flatten

  val data = dataForDay(1).map(_.toInt).toSeq
  val summingTo2020 = pairs(data).filter(p => p._1 + p._2 == 2020).toSeq
  println(summingTo2020)
  println(((p: (Int, Int)) => p._1 * p._2)(summingTo2020.head))
  val triplesSummingTo2020 = triples(data).filter(p => p._1 + p._2 + p._3 == 2020).toSeq
  println(triplesSummingTo2020)
  println(((p: (Int, Int, Int)) => p._1 * p._2 * p._3)(triplesSummingTo2020.head))
}
