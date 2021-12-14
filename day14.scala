import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(14).toSeq

  val template = data(0)
  val ruleRE = raw"(.*) -> (.*)".r
  val rules = data.drop(2).map{ case ruleRE(left, right) => (left, right) }.toMap

  def iteratePolymerCounts(polymerCounts: Map[String, Long]): Map[String, Long] =
    polymerCounts.toSeq.flatMap{
      case (pair, count) if rules.contains(pair) => Seq(
        Seq(pair(0), rules(pair)).mkString -> count,
        Seq(rules(pair), pair(1)).mkString -> count
      )
      case pc => Seq(pc)
    }.groupBy(_._1).mapValues(_.map(_._2).sum).toMap

  def scorePolymerCounts(template: String, polymerCounts: Map[String, Long]): Long = {
    val elementCounts = (polymerCounts.toSeq :+ (template.last.toString -> 1L)).map{
      case (pair, count) => (pair(0), count)
    }.groupBy(_._1).mapValues(_.map(_._2).sum).toMap
    elementCounts.view.values.max - elementCounts.view.values.min
  }

  val templatePairCounts = template.sliding(2).toSeq.groupBy(identity).mapValues(_.size.toLong).toMap
  val countsAfter10Iterations =
    (1 to 10).foldLeft(templatePairCounts){ case(counts, _) => iteratePolymerCounts(counts) }
  println(scorePolymerCounts(template, countsAfter10Iterations))
  val countsAfter40Iterations =
    (1 to 40).foldLeft(templatePairCounts){ case(counts, _) => iteratePolymerCounts(counts) }
  println(scorePolymerCounts(template, countsAfter40Iterations))
}
