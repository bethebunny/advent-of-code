import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  case class Rule(color: String, contains: Map[String, Int]) {
  }
  object Rule {
    def parse(s: String): Rule = {
      val noBagsRE = "(.*) bags contain no other bags.".r
      val bagsRE = "(.*) bags contain (.*).".r
      val subBagsRE = raw"(\d+) (.*) bags?".r
      s match {
        case noBagsRE(color) => Rule(color, Map())
        case bagsRE(color, subBags) => Rule(color, subBags.split(", ").map{
            case subBagsRE(n, subColor) => (subColor, n.toInt)
          }.toMap
        )
      }
    }
  }
  val data = dataForDay(7).toSeq
  val rules = data.map(Rule.parse _).map(rule => (rule.color, rule.contains)).toMap
  def hasPathTo(to: String, rules: Map[String, Map[String, Int]], from: Set[String] = Set()): Set[String] = {
    val next  = from | rules.filter{ case (k, v) => !(v.keySet & (if (from.isEmpty) Set(to) else from)).isEmpty}.keySet
    if (next == from) from else hasPathTo(to, rules, next)
  }
  println(hasPathTo("shiny gold", rules).size)
  def bagsRequired(from: String, rules: Map[String, Map[String, Int]], cache: Map[String, Int] = Map()): Int = {
    rules(from).toSeq match {
      case Seq() => 0
      case mustContain => mustContain.map{ case (color, number) => (1 + bagsRequired(color, rules)) * number }.sum
    }
  }
  println(bagsRequired("shiny gold", rules))
}
