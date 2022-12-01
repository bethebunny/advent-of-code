import advent_of_code.data.{rawDataForDay, sessionID}
import scala.collection.mutable


object Main extends App {
  val data = rawDataForDay(22)
  val Array(rawp1, rawp2) = data.split("\n\n")
  val deck1 = rawp1.split("\n").tail.map(_.toInt).toSeq
  val deck2 = rawp2.split("\n").tail.map(_.toInt).toSeq
  def combat(deck1: List[Int], deck2: List[Int]): List[Int] = (deck1, deck2) match {
    case (List(), deck) => deck
    case (deck, List()) => deck
    case (d1::r1, d2::r2) => {
      val p1wins = d1 > d2
      val r1p = if (d1 > d2) r1 ++ Seq(d1, d2) else r1
      val r2p = if (d2 > d1) r2 ++ Seq(d2, d1) else r2
      combat(r1p, r2p)
    }
  }

  // Wait, does seenStates invalidate the cache?
  // Why is this all so slow? I suspect memoizing will not solve my problem.
  type Cache = mutable.Map[(Vector[Int], Vector[Int]), (Boolean, Vector[Int])]

  def recursiveCombat(
    deck1: Vector[Int],
    deck2: Vector[Int],
    seenStates: Set[(Vector[Int], Vector[Int])] = Set(),
  )(implicit cache: Cache): (Boolean, Vector[Int]) = {
    if (seenStates.contains((deck1, deck2))) {
      (true, deck1)
    } else (deck1, deck2) match {
      case (Vector(), deck) => (false, deck)
      case (deck, Vector()) => (true, deck)
      case _ => {
        val (d1, r1) = (deck1.head, deck1.tail)
        val (d2, r2) = (deck2.head, deck2.tail)
        val shouldRecurse = r1.size >= d1 & r2.size >= d2
        val p1wins = if (shouldRecurse) recursiveCombat(r1, r2)._1 else d1 > d2
        val r1p = if (p1wins) r1 ++ Seq(d1, d2) else r1
        val r2p = if (!p1wins) r2 ++ Seq(d2, d1) else r2
        recursiveCombat(r1p, r2p, seenStates ++ Set((deck1, deck2)))
      }
    }
  }

  def valueDeck(deck: Seq[Int]): Long =
    deck.reverse.zipWithIndex.map{ case (v, i) => v.toLong * (i + 1) }.sum

  println(valueDeck(combat(deck1.toList, deck2.toList)))
  println(valueDeck(recursiveCombat(deck1.toVector, deck2.toVector)._2))
}
