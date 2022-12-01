import advent_of_code.data.{rawDataForDay, sessionID}


object Main extends App {

  implicit class ExtraSeqOps[A](s: Seq[A]) {
    def &&[B >: A](that: Iterable[B]): Seq[A] = s.intersect(that.toSeq)
    def &?[B >: A](that: Iterable[B]): Boolean = !(s && that).isEmpty
    def contains[B >: A](that: Iterable[B]): Boolean = (this && that).size == that.size
    /*
    def <:[B >: A](that: Iterable[B]): Boolean = {
      if (that.size > 3 & s.size > 8) {
        val ss = s.toSet
        that.forall(ss.contains(_))
      } else that.forall(s.contains(_))
    }
    def <:[B >: A](b: B): Boolean = s.contains(b)
    */
  }

  case class Tile(val id: Int, val data: Seq[String]) {

    def reflect: Tile = Tile(id, data.reverse)
    def rotateLeft: Tile = Tile(id, 0.until(data.size).reverse.map(i => data.map(_(i)).mkString("")))
    def rotate180: Tile = Tile(id, data.map(_.reverse).reverse)
    def rotateRight: Tile = rotateLeft.rotate180

    def rotations: Seq[Tile] = Seq(this, rotateLeft, rotate180, rotateRight)
    def orientations: Seq[Tile] = rotations ++ reflect.rotations

    def topBorder = data.head
    def bottomBorder = data.last
    def leftBorder = data.map(_.head).mkString("")
    def rightBorder = data.map(_.last).mkString("")

    def borders: Seq[String] = Seq(topBorder, bottomBorder, leftBorder, rightBorder)
    def allPossibleBorders: Seq[String] = borders ++ borders.map(_.reverse)

    def display = data.mkString("\n")

    def inner = data.init.tail.map(_.init.tail)

    val seaMonster = Seq(
      "                  # ",
      "#    ##    ##    ###",
      " #  #  #  #  #  #   ",
    )

    val seaMonsterHighlight = seaMonster.map(_.replace("#", "O"))

    def seaMonsterAt(i: Int, j: Int): Boolean = {
      val rows = data.drop(i).take(seaMonster.size).map(_.drop(j).take(seaMonster.head.size))
      seaMonster.zip(rows).forall{
        case (smr, row) => Seq(smr, row).transpose.forall{
          case Seq(' ', _) => true
          case Seq('#', '#') => true
          case Seq('#', _) => false
        }
      }
    }

    def seaMonsters: Seq[(Int, Int)] =
      0.until(data.size - seaMonster.size).flatMap(i =>
        0.until(data.head.size - seaMonster.head.size).filter(seaMonsterAt(i, _)).map((i, _))
      )

    def dataWithSeaMonstersHighlighted: Seq[String] = seaMonsters.foldLeft(data)(highlightSeaMonster)
    def highlightSeaMonster(data: Seq[String], at: (Int, Int)): Seq[String] = {
      val (x, y) = at
      val ss = seaMonster.size
      data.take(x) ++ data.drop(x).take(ss).zip(seaMonster).map{
        case (row, smr) => highlight(row, smr, y)
      } ++ data.drop(x + ss)
    }
    def highlight(s: String, h: String, offset: Int): String = {
      s.take(offset) ++ mask(s.slice(offset, offset + h.size), h) ++ s.drop(offset + h.size)
    }
    def mask(s: String, h: String): String = Seq(s, h).transpose.map{
      case Seq(c, ' ') => c
      case Seq(_, c) => 'O'
    }.mkString("")
    def waterRoughness: Int = dataWithSeaMonstersHighlighted.map(_.count(_ == '#')).sum
  }
  object Tile {
    def parse(s: String): Tile = {
      val (header, lines) = s.split("\n").splitAt(1)
      val headerRE = raw"Tile (\d+):".r
      val tileID = header(0) match { case headerRE(id) => id.toInt }
      Tile(tileID, lines.toSeq)
    }
  }

  case class Image(val tiles: Seq[Seq[Tile]]) {
    def data = Tile(0, tiles.map(row => row.map(_.inner).transpose.map(_.mkString(""))).flatten)
    override def toString = data.toString
  }
  object Image {
    def stitch(tiles: Seq[Tile]): Image = {
      val (cornerTiles, nonCornerTiles) =
        tiles.partition(t => tiles.count(_.borders &? t.allPossibleBorders) == 3)
      val nonCornerEdges = nonCornerTiles.flatMap(_.allPossibleBorders).toSet
      val firstCornerOrientations = cornerTiles.head.rotations.filter(
        t => nonCornerEdges.contains(t.bottomBorder) & nonCornerEdges.contains(t.rightBorder))
      val solutions = firstCornerOrientations.flatMap(firstCorner =>
        tileSolutions(
          Seq(Seq(firstCorner)),
          tiles.map(t => (t.id, t)).toMap - firstCorner.id,
          cornerTiles.map(_.id).toSet
        )
      )
      println(s"Found ${solutions.size} solutions")
      println(s"Solution shape: ${solutions.head.map(_.size)}")
      Image(solutions.head)
    }

    def tileSolutions(
      placed: Seq[Seq[Tile]],
      unplaced: Map[Int, Tile],
      corners: Set[Int],
    ): Iterable[Seq[Seq[Tile]]] = {
      //  1) build up the first row until we hit another corner piece
      //  2) build up each row until it has the same length as the previous row
      //  3) end when we're out of pieces
      //  anywhere there are multiple options for the pieces or orientations, branch
      if (unplaced.isEmpty) {
        Seq(placed)
      } else {
        val top = placed.head
        if (
          (placed.size == 1 & top.size > 1 & corners.contains(top.last.id))
          | (placed.size > 1 & placed.last.size == placed.head.size)
        ) {
          // we just finished a row, add first column of a new row
          val topBorder = placed.last.head.bottomBorder
          val matchingPieces = unplaced.values.filter(_.allPossibleBorders.contains(topBorder))
          val matchingOrientations = matchingPieces.flatMap(_.orientations.filter(_.topBorder == topBorder))
          matchingOrientations.flatMap(t => tileSolutions(placed :+ Seq(t), unplaced - t.id, corners))
        } else if (placed.size == 1) {
          // we're working on the first row
          val leftBorder = top.last.rightBorder
          val matchingPieces = unplaced.values.filter(_.allPossibleBorders.contains(leftBorder))
          val matchingOrientations = matchingPieces.flatMap(_.orientations.filter(_.leftBorder == leftBorder))
          matchingOrientations.flatMap(t => tileSolutions(Seq(top :+ t), unplaced - t.id, corners))
        } else {
          // not first row or first column
          val bottom = placed.last
          val nextLast = placed.init.last
          val leftBorder = bottom.last.rightBorder
          val topBorder = nextLast(bottom.size).bottomBorder
          val matchingPieces = unplaced.values.filter(t => {
              val borders = t.allPossibleBorders
              borders.contains(leftBorder) & borders.contains(topBorder)
          })
          val matchingOrientations = matchingPieces.flatMap(_.orientations.filter(
            t => t.topBorder == topBorder & t.leftBorder == leftBorder))
          matchingOrientations.flatMap(
            t => tileSolutions(placed.init :+ (bottom :+ t), unplaced - t.id, corners))
        }
      }
    }
  }

  val data = rawDataForDay(20)
  val rawTiles = data.split("\n\n").toSeq
  val tiles = rawTiles.map(Tile.parse)
  /*
  tiles.head.orientations.foreach(tile => println("\n" + tile.display))
  println("\n")
  tiles.head.allPossibleBorders.foreach(println(_))
  println(rawTiles.size)
  tiles.foreach(tile => {
    println(tiles.count(t2 => t2.allPossibleBorders &? tile.allPossibleBorders))
  })
  TODO: put part 1 solution back in
  */
  val image = Image.stitch(tiles)
  println(image.data.orientations.size)
  println(image.data.orientations.map(_.seaMonsters.size).max)
  println(image.data.orientations.maxBy(_.seaMonsters.size).waterRoughness)
}
