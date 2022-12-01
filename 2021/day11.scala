import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(11).toSeq

  val energyGrid: Map[(Int, Int), Int] = data.zipWithIndex.flatMap{
    case (hs, y) => hs.zipWithIndex.map{
      case (h, x) => ((x.toInt, y.toInt), h.toString.toInt)
    }
  }.toMap

  val neighborDirs = Seq((1, -1), (1, 0), (1, 1), (0, -1), (0, 1), (-1, -1), (-1, 0), (-1, 1))
  def neighbors(p: (Int, Int)): Seq[(Int, Int)] = p match {
    case (x, y) => neighborDirs.map{
      case (dx, dy) => (x + dx, y + dy)
    }
  }

  def isNeighbor(p: (Int, Int), p2: (Int, Int)): Boolean = neighbors(p).contains(p2)

  def flash(grid: Map[(Int, Int), Int], flashed: Set[(Int, Int)] = Set()): Map[(Int, Int), Int] = {
    val flashing = grid.filter { case (k, v) => !flashed.contains(k) && v > 9 }.keys
    if (!flashing.isEmpty) {
      flash(
        grid.map { case (k, v) => (k, v + flashing.count(p => isNeighbor(k, p))) },
        flashed ++ flashing
      )
    } else grid
  }
  def stepEnergyGrid(grid: Map[(Int, Int), Int]): Map[(Int, Int), Int] =
    flash(grid.mapValues(_ + 1).toMap).mapValues(v => if (v > 9) 0 else v).toMap

  println((1 to 100).scanLeft(energyGrid) {
    case (grid, _) => stepEnergyGrid(grid)
  }.map(_.view.values.count(_ == 0)).sum)

  def stepsUntilSynchronized(grid: Map[(Int, Int), Int]): Int =
    if (grid.view.values.forall(_ == 0)) 0 else (1 + stepsUntilSynchronized(stepEnergyGrid(grid)))

  println(stepsUntilSynchronized(energyGrid))
}
