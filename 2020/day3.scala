import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  def isTree(row: String, i: Int) = row(i) == '#'
  def isTree3by1(row: String, rowIndex: Int) = isTree(row, (3 * rowIndex) % row.size)
  def countTrees(data: Seq[String], over: Int, down: Int): Int =
    data
      .zipWithIndex
      .filter{ case(row, i) => i % down == 0 }
      // reset the index
      .map{ case(row, i) => row }
      .zipWithIndex
      .count{ case(row, i) => row((i * over) % row.size) == '#' }
  val data = dataForDay(3).toSeq
  /*
  val data = Seq(
    "..##.......",
    "#...#...#..",
    ".#....#..#.",
    "..#.#...#.#",
    ".#...##..#.",
    "..#.##.....",
    ".#.#.#....#",
    ".#........#",
    "#.##...#...",
    "#...##....#",
    ".#..#...#.#",
  )
  */
  println(data.zipWithIndex.count((isTree3by1 _).tupled))
  println(
    Seq((1, 1), (3, 1), (5, 1), (7, 1), (1, 2))
      .map{ case(over, down) => countTrees(data, over, down) }
      .map(BigInt(_))
      .product
  )
}
