import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(18).toSeq

  sealed trait Number {
    def +(other: Number): Pair = Pair(this, other).reduce
    def magnitude: Long
  }
  case class Regular(v: Long) extends Number {
    override def toString: String = v.toString
    def magnitude: Long = v
  }

  case class Pair(left: Number, right: Number) extends Number {
    def reduce: Pair = this match {
      case _ if shouldExplode(0) => explode(0)._1.asInstanceOf[Pair].reduce
      case _ if shouldSplit => split.reduce
      case _ => this
    }

    override def toString: String = s"[$left,$right]"

    def shouldExplode(depth: Int): Boolean = if (depth == 4) true else this match {
      case Pair(left: Pair, _) if left.shouldExplode(depth + 1) => true
      case Pair(_, right: Pair) if right.shouldExplode(depth + 1) => true
      case _ => false
    }

    def explode(depth: Int): (Number, Option[Long], Option[Long]) = (depth, this) match {
      case (4, Pair(Regular(lv), Regular(rv))) => (Regular(0), Some(lv), Some(rv))
      case (_, Pair(Regular(_), Regular(_))) => (this, None, None)
      case (_, Pair(leftPair: Pair, right)) if leftPair.shouldExplode(depth + 1) => {
        val (newLeft, lv, rv) = leftPair.explode(depth + 1)
        (right, rv) match {
          case (rightPair: Pair, _) => (Pair(newLeft, rightPair.applyExplode(rv, false)), lv, None)
          case (Regular(rightValue), Some(rv)) => (Pair(newLeft, Regular(rightValue + rv)), lv, None)
          case _ => (Pair(newLeft, right), lv, rv)
        }
      }
      case (_, Pair(left, rightPair: Pair)) if rightPair.shouldExplode(depth + 1) => {
        val (newRight, lv, rv) = rightPair.explode(depth + 1)
        (left, lv) match {
          case (leftPair: Pair, _) => (Pair(leftPair.applyExplode(lv, true), newRight), None, rv)
          case (Regular(leftValue), Some(lv)) => (Pair(Regular(leftValue + lv), newRight), None, rv)
          case _ => (Pair(left, newRight), lv, rv)
        }
      }
      case _ => throw new RuntimeException(s"Unexpected explode case at depth $depth: $this")
    }

    def applyExplode(v: Option[Long], rightmost: Boolean = true): Number = (v, rightmost, this) match {
      case (None, _, _) => this
      case (v, true, Pair(left, rightPair: Pair)) => Pair(left, rightPair.applyExplode(v, true))
      case (Some(l), true, Pair(left, Regular(rightValue))) => Pair(left, Regular(rightValue + l))
      case (v, false, Pair(leftPair: Pair, right)) => Pair(leftPair.applyExplode(v, false), right)
      case (Some(l), false, Pair(Regular(leftValue), right)) => Pair(Regular(leftValue + l), right)
    }

    def shouldSplit: Boolean = this match {
      case Pair(left: Pair, _) if left.shouldSplit => true
      case Pair(_, right: Pair) if right.shouldSplit => true
      case Pair(Regular(lv), _) if lv > 9 => true
      case Pair(_, Regular(rv)) if rv > 9 => true
      case _ => false
    }

    def makeSplit(v: Long): Pair = Pair(Regular(v / 2), Regular((v / 2) + (if (v % 2 == 0) 0 else 1)))

    def split: Pair = this match {
      case Pair(Regular(lv), _) if lv > 9 => Pair(makeSplit(lv), right)
      case Pair(leftPair: Pair, _) if leftPair.shouldSplit => Pair(leftPair.split, right)
      case Pair(_, Regular(rv)) if rv > 9 => Pair(left, makeSplit(rv))
      case Pair(_, rightPair: Pair) if rightPair.shouldSplit => Pair(left, rightPair.split)
      case _ => throw new RuntimeException(s"Unexpected split case: $this")
    }

    def magnitude: Long = 3 * left.magnitude + 2 * right.magnitude
  }


  def splitPair(s: String, index: Int = 0, bracketDepth: Int = 0): (String, String) = (s(index), bracketDepth) match {
    case (',', 0) => (s.substring(0, index), s.substring(index + 1))
    case ('[', depth) => splitPair(s, index + 1, depth + 1)
    case (']', depth) => splitPair(s, index + 1, depth - 1)
    case (_, depth) => splitPair(s, index + 1, depth)
  }

  def parseNumber(s: String): Number = if (s(0) == '[') {
    val (left, right) = splitPair(s.substring(1, s.size - 1))
    Pair(parseNumber(left), parseNumber(right))
  } else Regular(s.toLong)

  val pairsToAdd = data.map(parseNumber)
  val added = pairsToAdd.reduce(_ + _)
  println(added.magnitude)

  def combinations[T](s: Seq[T], n: Int = 2): Seq[Seq[T]] = (s, n) match {
    case (_, 0) => Seq(Seq[T]())
    case (Seq(), _) => Seq()
    case (s, n) => combinations(s.tail, n-1).map(s.head +: _) ++ combinations(s.tail, n)
  }

  println(
    (combinations(pairsToAdd, 2) ++ combinations(pairsToAdd, 2).map(_.reverse))
      .map{ case Seq(v1, v2) => (v1 + v2).magnitude }.max
  )
}
