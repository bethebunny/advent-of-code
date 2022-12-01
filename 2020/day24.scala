import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(24).toSeq

  def addCoords(p1: (Int, Int), p2: (Int, Int)): (Int, Int) = (p1._1 + p2._1, p1._2 + p2._2)
  def coord(hexPath: List[Char]): (Int, Int) = hexPath match {
    case Nil => (0, 0)
    case 'e'::xs => addCoords(coord(xs), (1, 0))
    case 'w'::xs => addCoords(coord(xs), (-1, 0))
    case 'n'::'e'::xs => addCoords(coord(xs), (1, 1))
    case 'n'::'w'::xs => addCoords(coord(xs), (0, 1))
    case 's'::'e'::xs => addCoords(coord(xs), (0, -1))
    case 's'::'w'::xs => addCoords(coord(xs), (-1, -1))
    case _ => throw new RuntimeException(s"Invalid coord string: ${hexPath}")
  }

  def counts[A](l: Seq[A]): Map[A, Int] =
    l.foldLeft(Map[A, Int]())((z, a) => z + (a -> (z.getOrElse(a, 0) + 1)))

  println(counts(data.map(d => coord(d.toList))).values.count(_ % 2 == 1))

  def neighbors(p: (Int, Int)): Seq[(Int, Int)] =
    Seq((1, 0), (-1, 0), (1, 1), (0, 1), (0, -1), (-1, -1)).map(addCoords(_, p))

  def iterate(blackTiles: Set[(Int, Int)]): Set[(Int, Int)] = {
    def numBlackNeighbors(p: (Int, Int)): Int = neighbors(p).count(blackTiles.contains(_))
    val allTiles = blackTiles.flatMap(neighbors(_))
    val whiteTiles = allTiles -- blackTiles;
    (
      blackTiles.filter(t => 1.to(2).contains(numBlackNeighbors(t)))
      ++ whiteTiles.filter(numBlackNeighbors(_) == 2)
    )
  }

  def nTimes[A](f: A => A)(a: A, n: Int): A = 0.until(n).foldLeft(a)((a, _) => f(a))

  val initial =
    counts(data.map(d => coord(d.toList))).filter{ case (_, v) => v % 2 == 1 }.keys.toSet
  val after100Days = nTimes(iterate)(initial, 100)
  println(after100Days.size)
}
