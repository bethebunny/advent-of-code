import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  class Counter[A](xs: Iterable[A]) {
    def apply(c: A) = xs.count(_ == c)
  }

  trait Password {
    def isValid: Boolean
  }

  case class OldPassword(policyMin: Int, policyMax: Int, policyChar: Char, password: String) extends Password {
    def isValid = {
      val counts = new Counter(password)(policyChar)
      counts >= policyMin && counts <= policyMax
    }
  }

  case class NewPassword(policyMin: Int, policyMax: Int, policyChar: Char, password: String) extends Password {
    def charAt(i: Int): Char = if (password.isDefinedAt(i - 1)) password(i - 1) else '\u0000'
    def isValid = (charAt(policyMin) == policyChar) ^ (charAt(policyMax) == policyChar)
  }

  object Password {
    val regex = """(\d+)-(\d+) (\w): (\w+)""".r
    val parse: (String => Password) = {
      case regex(min, max, c, password) => OldPassword(min.toInt, max.toInt, c(0), password)
    }
    val parseNew: (String => Password) = {
      case regex(min, max, c, password) => NewPassword(min.toInt, max.toInt, c(0), password)
    }
  }

  val data = dataForDay(2).toSeq
  println(data.map(Password.parse).count(_.isValid))
  println(data.map(Password.parseNew).count(_.isValid))

  // Much shorter solution
  val passwordRE = raw"(\d+)-(\d+) (\w): (\w+)".r
  println(data.count{case passwordRE(min, max, c, pw) => min.toInt.to(max.toInt).contains(pw.count(_ == c(0)))})
  println(data.count{case passwordRE(i1, i2, c, pw) => (pw(i1.toInt-1) == c(0)) ^ (pw(i2.toInt-1) == c(0))})
}
