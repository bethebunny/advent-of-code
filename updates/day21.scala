import advent_of_code.data.{dataForDay, sessionID}
import scala.collection.mutable


object Main extends App {
  val data = dataForDay(21).toSeq

  trait Die {
    def roll: (Die, Int)
  }
  case class DeterministicD100(v: Int) extends Die {
    def roll = (DeterministicD100((v + 2) % 100 + 1), ((v + 1) * 3) % 10)
  }

  case class Player(position: Int, score: Int = 0) {
    def move(roll: Int) = {
      val newPos = (position + roll - 1) % 10 + 1
      Player(newPos, score + newPos)
    }
  }

  case class Game(ap: Player, nap: Player) {
    def nextTurn(roll: Int) = Game(nap, ap.move(roll))
  }

  def playTo(game: Game, to: Int, die: Die, rolls: Int = 0): (Game, Int) =
    if (game.nap.score >= to) (game, rolls) else {
      val (dNext, roll) = die.roll
      playTo(game.nextTurn(roll), to, dNext, rolls + 3)
    }

  val positionRE = raw"""Player .* starting position: (\d+)""".r
  val p1 = data(0) match { case positionRE(p1) => Player(p1.toInt) }
  val p2 = data(1) match { case positionRE(p2) => Player(p2.toInt) }

  val start = Game(p1, p2)
  val (end, nRolls) = playTo(start, 1000, DeterministicD100(1))
  println(end.ap.score * nRolls)

  val diracRollMap: Map[Int, Int] = Map(3 -> 1, 4 -> 3, 5 -> 6, 6 -> 7, 7 -> 6, 8 -> 3, 9 -> 1)

  def diracWins(game: Game, to: Int, cache: mutable.Map[Game, (Long, Long)] = mutable.Map()): (Long, Long) = {
    if (!cache.contains(game)) cache += game -> {
      if (game.nap.score >= to) (0L, 1L)
      else diracRollMap.toSeq.map{ case (roll, nCases) => {
        val (napWins, apWins) = diracWins(game.nextTurn(roll), to, cache)
        (apWins * nCases, napWins * nCases)
      }}.reduce((a, b) => (a._1 + b._1, a._2 + b._2))
    }
    cache(game)
  }

  val (p1wins, p2wins) = diracWins(start, 21)
  println(p1wins max p2wins)
}
