import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(4).toSeq

  val numbers = data.head.split(",").map(_.toInt)

  case class Board(vals: Seq[Seq[Int]], _marked: Seq[Int] = Seq()) {
    val marked = _marked.toSet
    def hasWon: Boolean = (
      vals.exists(row => row.forall(marked.contains(_)))
      || vals.transpose.exists(col => col.forall(marked.contains(_)))
    )
    def score: Int = vals.flatten.filter(!marked.contains(_)).sum * _marked.last
    def mark(v: Int): Board = Board(vals, _marked :+ v)
  }
  
  object Board {
    def parse(s: Seq[String]): Board = Board(s.map(_.trim.split(raw"\s+").map(_.toInt)))
  }

  def winningBoard(numbers: Seq[Int], boards: Seq[Board]): Board = {
    val winningBoards = boards.filter(_.hasWon)
    if (winningBoards.size > 0) {
      winningBoards.head
    } else {
      winningBoard(numbers.tail, boards.map(_.mark(numbers.head)))
    }
  }

  val boards = data.tail.grouped(6).map(ss => Board.parse(ss.tail)).toSeq

  println(winningBoard(numbers, boards).score)

  def leastWinningBoard(numbers: Seq[Int], boards: Seq[Board]): Board = {
    val nonWinningBoards = boards.filter(!_.hasWon)
    nonWinningBoards match {
      case Seq() => boards.head
      case _ => leastWinningBoard(numbers.tail, nonWinningBoards.map(_.mark(numbers.head)))
    }
  }

  println(leastWinningBoard(numbers, boards).score)
}

