import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(20).toSeq

  def kernelIndices(p: (Int, Int)) =
    Seq((-1, -1), (0, -1), (1, -1), (-1, 0), (0, 0), (1, 0), (-1, 1), (0, 1), (1, 1)).map{
      case (dx, dy) => (dx + p._1, dy + p._2)
    }

  def applyKernel(s: Seq[Char]): Char =
    lookup(Integer.parseInt(s.map(Map('.' -> '0', '#' -> '1')(_)).mkString, 2))

  case class Image(pixels: Map[(Int, Int), Char], default: Char = '.') {
    def xmin = pixels.keys.map(_._1).min
    def xmax = pixels.keys.map(_._1).max
    def ymin = pixels.keys.map(_._2).min
    def ymax = pixels.keys.map(_._2).max
    def enhance: Image = Image(
      ((ymin - 1) to (ymax + 1)).flatMap(y =>
        ((xmin - 1) to (xmax + 1)).map(x =>
          (x, y) -> applyKernel(kernelIndices((x, y)).map(p => pixels.getOrElse(p, default)))
        )
      ).toMap,
      default = applyKernel(default.toString * 9)
    )

    def print(): Unit =
      (ymin - 1 to ymax + 1).map(y => (xmin - 1 to xmax + 1).map(x =>
          pixels.getOrElse((x, y), default)).mkString).foreach(println)
  }

  val lookup = data.head
  val image = Image(data.drop(2).zipWithIndex.flatMap{
    case (line, y) => line.zipWithIndex.map{ case (c, x) => ((x, y), c) }
  }.toMap, default='.')

  println(image.enhance.enhance.pixels.values.count(_ == '#'))
  val enhanced = (1 to 50).foldLeft(image){ case (image, _) => image.enhance }
  enhanced.print
  println(enhanced.pixels.values.count(_ == '#'))
}
