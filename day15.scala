import advent_of_code.data.{dataForDay, sessionID}
import scala.collection.mutable


object Main extends App {
  val data = dataForDay(15).toSeq

  val riskMap = data.zipWithIndex.flatMap{
    case (line, y) => line.zipWithIndex.map{
      case (v, x) => ((x, y), v.toString.toInt)
    }
  }.toMap

  def neighbors(m: Map[(Int, Int), Int], p: (Int, Int)): Seq[(Int, Int)] = p match {
    case (x, y) => Seq((1, 0), (-1, 0), (0, 1), (0, -1)).map{
      case (dx, dy) => (x + dx, y + dy)
    }.filter(m.contains _)
  }

  def djikstras(
    end: (Int, Int),
    costs: Map[(Int, Int), Int],
    cheapestPaths: Map[(Int, Int), Int] = Map(),
    lastUpdated: Set[(Int, Int)] = Set()
  ): Map[(Int, Int), Int] = if (cheapestPaths.isEmpty) {
    val maxCost = costs.values.sum + 1
    djikstras(end, costs, (costs.mapValues(_ => maxCost).toMap + (end -> costs(end))), Set(end))
  } else if (lastUpdated.isEmpty) cheapestPaths else {
    val toUpdate = lastUpdated.flatMap(p => neighbors(costs, p))
    val updated = toUpdate.flatMap(p => {
      val minPath = neighbors(costs, p).map(n => cheapestPaths(n) + costs(p)).min
      if (minPath < cheapestPaths(p)) Some(p -> minPath) else None
    }).toMap
    djikstras(end, costs, cheapestPaths ++ updated, updated.keySet)
  }

  val width = riskMap.keys.map(_._1).max + 1
  val height = riskMap.keys.map(_._2).max + 1
  val cheapestPaths = djikstras((width - 1, height - 1), riskMap)
  // starting position "not entered" so its risk is not counted"
  println(cheapestPaths((0, 0)) - riskMap((0, 0)))

  val expandedRiskMap = (0 until 5).flatMap(tileX => (0 until 5).flatMap(tileY => {
    riskMap.map{
      case ((x, y), risk) =>
        (x + width * tileX, y + height * tileY) -> ((risk + tileX + tileY - 1) % 9 + 1)
    }
  })).toMap

  val expandedCheapestPaths = djikstras((width * 5 - 1, height * 5 - 1), expandedRiskMap)
  println(expandedCheapestPaths((0, 0)) - riskMap((0, 0)))
}
