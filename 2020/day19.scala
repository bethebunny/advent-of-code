import advent_of_code.data.{rawDataForDay, sessionID}
import scala.collection.mutable
import scala.collection.StringOps


object Main extends App {
  case class Rule(options: Seq[Either[Seq[Int], Char]]) {}
  object Rule {
    def parse(s: String): Rule = {
      val characterRE = raw"""^"(\w)"$$""".r
      Rule(s match {
        case characterRE(c) => Seq(Right(c(0)))
        case _ => s.split(raw"\|").map(_.trim.split(" ").toSeq.map(_.toInt)).map(Left(_)).toSeq
      })
    }
  }


  case class Grammar(rules: Map[Int, Rule]) {
    type Cache = mutable.Map[String, Seq[Int]]
    def rulesMatching(s: String)(implicit cache: Cache = mutable.Map()): Seq[Int] = {
      if (!cache.contains(s)) {
        cache += (s -> rules.filter{ case (_, r) => matches(r, s) }.keys.toSeq)
      }
      cache(s)
    }

    def matches(rule: Rule, s: String)(implicit cache: Cache): Boolean = rule.options.exists(matches(_, s))
    def matches(r: Either[Seq[Int], Char], s: String)(implicit cache: Cache): Boolean = r match {
      case Left(subrules) => matches(subrules, s)
      case Right(c) => matches(c, s)
    }
    def matches(c: Char, s: String)(implicit cache: Cache) = {
      var ss = new StringOps(s)
      (ss.size == 1) & (ss(0) == c)
    }
    def matches(subrules: Seq[Int], s: String)(implicit cache: Cache): Boolean = {
      subrules match {
        case Seq(x, y) => {
          val ss = new StringOps(s)
          1.until(ss.size).map(ss.splitAt(_)).exists{
            case (left: String, right: String) => rulesMatching(left).contains(x) & rulesMatching(right).contains(y)
          }
        }
        case Seq(x) => matches(rules(x), s)
        case _ => throw new RuntimeException(s"Unexpected rule sequence: $subrules")
      }
    }
  }
  val data = rawDataForDay(19)
  /*
  val data = """0: 4 6
               |1: 2 3 | 3 2
               |2: 4 4 | 5 5
               |3: 4 5 | 5 4
               |4: "a"
               |5: "b"
               |6: 1 5
               |
               |ababbb
               |bababa
               |abbbab
               |aaabbb
               |aaaabbb""".stripMargin
               */
  val Array(rawRules, texts) = data.split("\n\n").map(_.split("\n"))
  val ruleRE = raw"(\d+): (.*)".r
  val grammar = Grammar(rawRules.map{ case ruleRE(ruleID, rule) => (ruleID.toInt, Rule.parse(rule)) }.toMap)

  // part 1 (slow)
  // println(texts.map(text => grammar.rulesMatching(text).contains(0)).count(_ == true))

  val grammar2 = Grammar(grammar.rules ++ Map(
    (8 -> Rule.parse("42 | 42 8")),
    // The problem states "11: 42 31 | 42 11 31" but it would take tweaking to make this work for
    // not chomsky-normal-form, so adding as 2 rules
    (11 -> Rule.parse("42 31 | 42 999")),
    (999 -> Rule.parse("11 31")),
  ))
  println(texts.map(text => grammar2.rulesMatching(text).contains(0)).count(_ == true))
}
