import advent_of_code.data.{rawDataForDay, sessionID}


object Main extends App {
  val rawData = rawDataForDay(16)
  /*
  val rawData = """class: 1-3 or 5-7
row: 6-11 or 33-44
seat: 13-40 or 45-50

your ticket:
7,1,14

nearby tickets:
7,3,47
40,4,50
55,2,20
38,6,12"""
*/
  val Array(section1, section2, section3) = rawData.split("\n\n")
  val rules = section1.split("\n")
  val myTicket = section2.split("\n")(1).split(",").map(_.toInt).toSeq
  val tickets = section3.split("\n").tail.map(_.split(",").map(_.toInt)).toSeq
  val ruleRE = raw"(.*): (\d+)-(\d+) or (\d+)-(\d+)".r
  val validRanges = rules.flatMap{
    case ruleRE(name, t1, f1, t2, f2) => Seq(t1.toInt.to(f1.toInt), t2.toInt.to(f2.toInt))
    case v => throw new RuntimeException(s"'$v' didn't match RE")
  }.toSeq
  println(tickets.flatten.filter(v => !validRanges.exists(_.contains(v))).sum)

  def permutationConstraintSatisfy(possibilities: Seq[(Seq[Int], Int)]): Seq[Seq[Int]] = {
    if (possibilities.exists(_._1 == Seq())) Seq() else possibilities match {
      case Nil => Seq(Seq())
      case _ => {
        val n = possibilities.size - 1
        val canBeN = possibilities.map(_._1).zipWithIndex.filter(_._1.contains(n)).map(_._2)
        val withoutN = possibilities.map(p => (p._1.filter(_ != n), p._2))

        canBeN.flatMap(i => permutationConstraintSatisfy(
          withoutN.take(i) ++ withoutN.drop(i+1)
        ).map(_:+possibilities(i)._2))
      }
    }
  }

  val ruleRanges = rules.map{
    case ruleRE(name, t1, f1, t2, f2) => Seq(t1.toInt.to(f1.toInt), t2.toInt.to(f2.toInt))
  }.toSeq

  val validTickets = tickets.filter(_.forall(v => validRanges.exists(_.contains(v))))

  val fieldPermutations = {
    val possibleRuleMappings = ruleRanges.map(rule =>
      0.until(ruleRanges.size).filter(i => {
        validTickets.map(_(i)).forall(v => rule.exists(_.contains(v)))
      })
    ).toSeq
    permutationConstraintSatisfy(possibleRuleMappings.zipWithIndex)
  }

  def inversePermutation(s: Seq[Int]): Seq[Int] = 0.until(s.size).sortBy(s(_))
  println(fieldPermutations)
  // This is a Sequence where a value i at index j means that
  // the rule at index i in rules maps to ticket column j
  val permutation = fieldPermutations.head
  println(permutation.head)
  println(ruleRanges(permutation.head))
  println(validTickets.map(_.head).filter(v => ruleRanges(permutation.head).forall(!_.contains(v))))
  println(ruleRanges.zipWithIndex.map{
    case (rule, i) => validTickets.map(_(i)).forall(v => rule.exists(_.contains(v)))
  })
  val departureFields = rules
    .map{ case ruleRE(name, _*) => name }
    .zipWithIndex
    .filter(_._1.contains("departure"))
    .map(_._2)
    .map(permutation(_))
    .toSeq
  println(departureFields)
  println(departureFields.map(myTicket(_)).map(_.toLong).product)
}
