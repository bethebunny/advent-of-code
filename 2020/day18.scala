import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  //val data = dataForDay(18).toSeq
  val data = Seq(
    "((2 + 4 * 9) * (6 + 9 * 8 + 6) + 6) + 2 + 4 * 2",
    "5 * 9 * (7 * 3 * 3 + 9 * 3 + (8 + 6 * 4))",
    "2 * 3 + (4 * 5)",
  )
  abstract class Value {
    def value(implicit orderOfOps: Seq[Seq[String]]): Long
  }
  case class Operation(
    val values: Seq[Value],
    val operators: Seq[String],
    val closed: Boolean = false,
  ) extends Value {
    def simpleEval(op: String, left: Value, right: Value)(implicit orderOfOps: Seq[Seq[String]]): Long = op match {
      case "+" => left.value + right.value
      case "*" => left.value * right.value
    }
    def value(implicit orderOfOps: Seq[Seq[String]]): Long = values match {
      case Seq(v) => v.value
      case Seq(v1, v2) => simpleEval(operators.head, v1, v2)
      case _ => {
        val opsClass = orderOfOps.filter(opClass => !(opClass.toSet & operators.toSet).isEmpty).head
        val i = operators.indexWhere(opsClass.contains(_))
        val v = Operation(values.slice(i, i+2), Seq(operators(i)))
        val nestedOp = Operation(
          (values.take(i) :+ v) ++ values.drop(i+2),
          operators.take(i) ++ operators.drop(i+1),
        )
        nestedOp.value
      }
    }
    override def toString =
      s"(${values(0)} ${values.tail.zip(operators).map(p => s"${p._2} ${p._1}").mkString(" ")})"
  }
  case class LongValue(val v: Long) extends Value {
    def value(implicit orderOfOps: Seq[Seq[String]]): Long = v
    override def toString = s"$v"
  }

  def prependOp(v: Value, op: String, ops: Value): Operation = ops match {
    case Operation(values, ops, false) => Operation(v+:values, op+:ops)
    case rv => Operation(Seq(v, rv), Seq(op))
  }

  def splitOnOpenParen(s: String, b: String = "", nClose: Int = 1): (String, String) = (s.last, nClose) match {
    case ('(', 1) => (s.init, b)
    case ('(', nClose) => splitOnOpenParen(s.init, '(' +: b, nClose - 1)
    case (')', nClose) => splitOnOpenParen(s.init, ')' +: b, nClose + 1)
    case (c, nClose) => splitOnOpenParen(s.init, c +: b, nClose)
  }

  def splitOnCloseParen(s: String, b: String = "", nOpen: Int = 1): (String, String) = (s.head, nOpen) match {
    case (')', 1) => (b, s.tail)
    case (')', nOpen) => splitOnCloseParen(s.tail, b :+ ')', nOpen - 1)
    case ('(', nOpen) => splitOnCloseParen(s.tail, b :+ '(', nOpen + 1)
    case (c, nOpen) => splitOnCloseParen(s.tail, b :+ c, nOpen)
  }

  def parseOperation(s: String, closed: Boolean = false): Value = {
    val onlyNumRE = raw"^(\d+)$$".r
    val firstNumRE = raw"^(\d+) ([+*]) (.*)$$".r
    val startsWithParenRE = raw"^\(.*".r
    val clippedRE = raw"^ ([+*]) (.*)$$".r
    s match {
      case onlyNumRE(num) => LongValue(num.toLong)

      case firstNumRE(num, op, rest) => prependOp(LongValue(num.toLong), op, parseOperation(rest))

      case startsWithParenRE() => {
        val (left, rest) = splitOnCloseParen(s.tail)
        rest match {
          case clippedRE(op, right) => prependOp(parseOperation(left), op, parseOperation(right))
          case "" => parseOperation(left, true)
        }
      }
    }
  }

  {
    implicit val orderOfOps = Seq(Seq("+", "*"))
    data.foreach(exp => println(s"$exp ==> ${parseOperation(exp)} ==> ${parseOperation(exp).value}"))
    println(data.map(parseOperation(_).value).sum)
  }

  {
    implicit val orderOfOps = Seq(Seq("*"), Seq("+"))
    data.foreach(exp => println(s"$exp ==> ${parseOperation(exp)} ==> ${parseOperation(exp).value}"))
    println(data.map(parseOperation(_).value).sum)
  }
}




/*  Part 1 alone:
  val data = dataForDay(18).toSeq
  //val data = Seq("((2 + 4 * 9) * (6 + 9 * 8 + 6) + 6) + 2 + 4 * 2")
  abstract class Value {
    def value: Long
  }
  case class Operation(val operator: String, val left: Value, val right: Value) extends Value {
    def value: Long = operator match {
      case "+" => left.value + right.value
      case "*" => left.value * right.value
    }
    override def toString = s"($left $operator $right)"
  }
  case class LongValue(val v: Long) extends Value {
    def value: Long = v
    override def toString = s"$v"
  }
  def splitOnOpenParen(s: String, b: String = "", nClose: Int = 1): (String, String) = (s.last, nClose) match {
    case ('(', 1) => (s.init, b)
    case ('(', nClose) => splitOnOpenParen(s.init, '(' +: b, nClose - 1)
    case (')', nClose) => splitOnOpenParen(s.init, ')' +: b, nClose + 1)
    case (c, nClose) => splitOnOpenParen(s.init, c +: b, nClose)
  }
  def parseOperation(s: String): Value = {
    val onlyNumRE = raw"^(\d+)$$".r
    val lastNumRE = raw"^(.*) ([+*]) (\d+)$$".r
    val endsWithParenRE = raw".*\)$$".r
    val clippedRE = raw"^(.*) ([+*]) $$".r
    s match {
      case onlyNumRE(num) => LongValue(num.toLong)
      case lastNumRE(init, op, num) => Operation(op, parseOperation(init), LongValue(num.toLong))
      case endsWithParenRE() => {
        val (init, right) = splitOnOpenParen(s.init)
        init match {
          case clippedRE(left, op) => Operation(op, parseOperation(left), parseOperation(right))
          case "" => parseOperation(right)
        }
      }
    }
  }

  data.map(parseOperation).foreach(exp => println(s"$exp ==> ${exp.value}"))
  println(data.map(parseOperation(_).value).sum)
}
*/
