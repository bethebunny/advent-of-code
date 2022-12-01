import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(2).toSeq
  val line = raw"(.+) (\d+)".r
  val directives = data.map{
    case line(directive, distance) => (directive, distance.toLong)
  }
  val distance = directives.map {
    case ("forward", distance) => distance
    case _ => 0
  }.sum
  val depth = directives.map {
    case ("down", distance) => distance
    case ("up", distance) => -distance
    case _ => 0
  }.sum
  println(distance * depth)

  case class Position(aim: Long = 0, distance: Long = 0, depth: Long = 0) {
    def applyDirective(directive: String, amount: Long): Position = directive match {
      case "forward" => Position(aim, distance+amount, depth+aim*amount)
      case "down" => Position(aim+amount, distance, depth)
      case "up" => Position(aim-amount, distance, depth)
    }
  }

  val finalPosition = directives.foldLeft(Position()){
    case (pos, (directive, amount)) => pos.applyDirective(directive, amount)
  }
  println(finalPosition.distance * finalPosition.depth)
}

