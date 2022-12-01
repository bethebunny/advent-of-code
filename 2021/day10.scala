import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(10).toSeq

  sealed trait Token
  case class Open(c: Char) extends Token {
    def completionValue: Long = Map('(' -> 1, '[' -> 2, '{' -> 3, '<' -> 4)(c).toLong
  }
  case class Close(c: Char) extends Token {
    def closes(o: Open): Boolean = (o.c, c) match {
      case ('(', ')') | ('<', '>') | ('[', ']') | ('{', '}') => true
      case _ => false
    }
    def value: Long = Map(')' -> 3, ']' -> 57, '}' -> 1197, '>' -> 25137)(c).toLong
  }

  def firstIllegalToken(tokens: List[Token], stack: List[Open] = Nil): Option[Close] = (tokens, stack) match {
    case (Nil, _) => None  // ending with a non-empty staci is fine
    case ((t: Close) :: ts, s :: ss) if t.closes(s) => firstIllegalToken(ts, ss)
    case ((t: Close) :: _, _) => Some(t)
    case ((t: Open) :: ts, stack) => firstIllegalToken(ts, t :: stack)
  }

  val lines: Seq[List[Token]] = data.map(_.map(c => c match {
    case '[' | '(' | '<' | '{' => Open(c)
    case ']' | ')' | '>' | '}' => Close(c)
  }).toList)

  println(lines.flatMap(firstIllegalToken(_)).map(_.value).sum)

  def unclosedTokens(tokens: List[Token], stack: List[Open] = Nil): Option[List[Open]] = (tokens, stack) match {
    case (Nil, stack) => Some(stack.reverse)  // reverse stack to get the original token order
    case ((t: Close) :: ts, s :: ss) if t.closes(s) => unclosedTokens(ts, ss)
    case ((t: Close) :: _, _) => None
    case ((t: Open) :: ts, stack) => unclosedTokens(ts, t :: stack)
  }

  def scoreUnclosedTokens(tokens: List[Open]): Long =
    tokens.zipWithIndex.map{
      case (t: Open, i: Int) => scala.math.pow(5L, i).toLong * t.completionValue
    }.sum

  def median(s: Seq[Long]): Long = s.sorted.apply(s.size / 2)

  println(median(lines.flatMap(unclosedTokens(_)).map(scoreUnclosedTokens)))
}
