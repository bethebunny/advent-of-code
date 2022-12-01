import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(11).toSeq
  /*
  val data = Seq(
    "L.LL.LL.LL",
    "LLLLLLL.LL",
    "L.L.L..L..",
    "LLLL.LL.LL",
    "L.LL.LL.LL",
    "L.LLLLL.LL",
    "..L.L.....",
    "LLLLLLLLLL",
    "L.LLLLLL.L",
    "L.LLLLL.LL",
  )
  */
  val seatMap = data.zipWithIndex.map{
    case (row, i) => row.zipWithIndex.map{
      case (v, j) => ((i, j), v)
    }
  }.flatten.toMap
  def product[A](s1: Seq[A], s2: Seq[A]): Seq[(A, A)] = s1.map(x => s2.map((x, _))).flatten
  def neighbors(p: (Int, Int)): Seq[(Int, Int)] =
    product(Seq(p._1 - 1, p._1, p._1 + 1), Seq(p._2 - 1, p._2, p._2 + 1)).filter(_ != p)
  def iterate(map: Map[(Int, Int), Char]): Map[(Int, Int), Char] = map.map{
    case (p, '.') => p -> '.'
    case (p, v) => {
      val occupiedNeighbors = neighbors(p).map(map.getOrElse(_, '.')).count(_ == '#')
      if (occupiedNeighbors == 0) {
        p -> '#'
      } else if (occupiedNeighbors >= 4) {
        p -> 'L'
      } else p -> v
    }
  }
  def equilibrium(map: Map[(Int, Int), Char]): Map[(Int, Int), Char] = {
    var next = iterate(map)
    // println()
    // println()
    // data.zipWithIndex.map{ case (row, i) => row.zipWithIndex.map { case (_, j) => map((i, j)) }.mkString("") }.foreach(println)
    if (next == map) map else equilibrium(next)
  }
  println(equilibrium(seatMap).values.count(_ == '#'))

  def nextVisibleNeighbor(p: (Int, Int), direction: (Int, Int), map: Map[(Int, Int), Char]): Char = {
    val next = (p._1 + direction._1, p._2 + direction._2)
    if (!map.contains(next)) {
      'L'
    } else if (map(next) == '.') {
      nextVisibleNeighbor(next, direction, map)
    } else map(next)
  }
  def visibleOccupiedNeighbors(p: (Int, Int), map: Map[(Int, Int), Char]): Int = {
    Seq((-1, -1), (-1, 0), (-1, 1), (0, -1), (0, 1), (1, -1), (1, 0), (1, 1)).map(direction => {
      nextVisibleNeighbor(p, direction, map)
    }).count(_ == '#')
  }
  def equilibrium2(map: Map[(Int, Int), Char]): Map[(Int, Int), Char] = {
    def iterate(map: Map[(Int, Int), Char]): Map[(Int, Int), Char] = map.map{
      case (p, '.') => p -> '.'
      case (p, v) => {
        val occupiedNeighbors = visibleOccupiedNeighbors(p, map)
        if (occupiedNeighbors == 0) {
          p -> '#'
        } else if (occupiedNeighbors >= 5) {
          p -> 'L'
        } else p -> v
      }
    }
    var next = iterate(map)
    if (next == map) map else equilibrium2(next)
  }
  println(equilibrium2(seatMap).values.count(_ == '#'))
}
