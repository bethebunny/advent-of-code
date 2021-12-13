import advent_of_code.data.{dataForDay, sessionID}
import scala.collection.mutable


object Main extends App {
  val data = dataForDay(13).toSeq
  val dotRE = raw"(\d+),(\d+)".r
  val foldRE = raw"fold along (x|y)=(\d+)".r
  val dots = data.takeWhile(_ != "").map{ case dotRE(x, y) => (x.toInt, y.toInt) }.toSet
  val folds = data.drop(dots.size + 1).map{ case foldRE(direction, value) => (direction, value.toInt) }

  def foldDots(dots: Set[(Int, Int)], foldDir: String, foldValue: Int): Set[(Int, Int)] =
    dots.map(p => (foldDir, p) match {
      case ("x", (x, y)) if x > foldValue => (2*foldValue - x, y)
      case ("y", (x, y)) if y > foldValue => (x, 2*foldValue - y)
      case (_, p) => p
    })

  val firstFold = folds.head
  println(foldDots(dots, firstFold._1, firstFold._2).size)

  val folded = folds.foldLeft(dots){
    case (dots, (foldDir, foldValue)) => foldDots(dots, foldDir, foldValue)
  }

  def drawDots(dots: Set[(Int, Int)]): Unit = {
    val width = dots.map(_._1).max + 1
    val height = dots.map(_._2).max + 1
    (0 until height).foreach(y => {
      val chars = (" " * width).to(mutable.Seq)
      dots.filter(_._2 == y).foreach(p => chars.update(p._1, '#'))
      println(chars.mkString)
    })
  }

  drawDots(folded)
}
