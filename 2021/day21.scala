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
  case class DiracDie() extends Die {
    def roll = (this, 0)
  }

  case class Player(position: Int, score: Int = 0) {
    def move(roll: Int) = {
      val newPos = (position + roll - 1) % 10 + 1
      Player(newPos, score + newPos)
    }
  }

  case class Game(p1: Player, p2: Player, p1Turn: Boolean, die: Die) {
    def turn = {
      val (dNext, roll) = die.roll
      nextTurn(roll, dNext)
    }
    def nextTurn(roll: Int, die: Die = die) = p1Turn match {
      case true => Game(p1.move(roll), p2, !p1Turn, die)
      case false => Game(p1, p2.move(roll), !p1Turn, die)
    }
    def over(winningScore: Int): Option[Int] = (p1.score, p2.score) match {
      case (p1Score, p2Score) if p1Score >= winningScore => Some(p2Score)
      case (p1Score, p2Score) if p2Score >= winningScore => Some(p1Score)
      case _ => None
    }
  }

  val positionRE = raw"""Player .* starting position: (\d+)""".r
  val p1 = data(0) match { case positionRE(p1) => Player(p1.toInt) }
  val p2 = data(1) match { case positionRE(p2) => Player(p2.toInt) }

  def playTo1000(game: Game, rolls: Int = 0): Int = game.over(1000) match {
    case Some(losingScore) => losingScore * rolls
    case _ => playTo1000(game.turn, rolls + 3)
  }

  val start = Game(p1, p2, true, DeterministicD100(1))
  println(playTo1000(start))

  val diracRollMap: Map[Int, Int] = Map(3 -> 1, 4 -> 3, 5 -> 6, 6 -> 7, 7 -> 6, 8 -> 3, 9 -> 1)

  def diracWins(game: Game, to: Int, cache: mutable.Map[Game, (Long, Long)] = mutable.Map()): (Long, Long) = {
    if (!cache.contains(game)) cache += game -> {
      if (game.p1.score >= to) (1L, 0L)
      else if (game.p2.score >= to) (0L, 1L)
      else diracRollMap.toSeq.map{ case (roll, nCases) => {
        val (p1Wins, p2Wins) = diracWins(game.nextTurn(roll), to, cache)
        (p1Wins * nCases, p2Wins * nCases)
      }}.reduce((a, b) => (a._1 + b._1, a._2 + b._2))
    }
    cache(game)
  }

  val (p1wins, p2wins) = diracWins(Game(p1, p2, true, DiracDie()), 21)
  println(p1wins max p2wins)
}
