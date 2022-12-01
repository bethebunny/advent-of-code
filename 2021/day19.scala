import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(19).toSeq

  def splitSeq[T](s: Seq[T], t: T): Seq[Seq[T]] =
    if (!s.contains(t)) Seq(s) else s.takeWhile(_ != t) +: splitSeq(s.dropWhile(_ != t).tail, t)

  case class Point(x: Int, y: Int, z: Int) {
    def +(other: Point) = Point(x + other.x, y + other.y, z + other.z)
    def -(other: Point) = Point(x - other.x, y - other.y, z - other.z)
    def *(v: Int) = Point(x * v, y * v, z * v)
    def dot(other: Point) = x * other.x + y * other.y + z * other.z
    def manhattan(other: Point) = {
      val d = other - this
      d.x.abs + d.y.abs + d.z.abs
    }
  }

  case class Rotation(a: Point, b: Point, c: Point) {
    def apply(p: Point) = Point(a.dot(p), b.dot(p), c.dot(p))
    def determinant: Int =
      a.x * (b.y * c.z - b.z * c.y) - a.y * (b.x * c.z - b.z * c.x) + a.z * (b.x * c.y - b.y * c.x)
  }
  object Rotation {
    val all: Seq[Rotation] =
      Seq(Point(1, 0, 0), Point(0, 1, 0), Point(0, 0, 1)).permutations.flatMap{
        case Seq(i, j, k) => Seq(
          Seq(1, 1, 1), Seq(1, 1, -1), Seq(1, -1, 1), Seq(1, -1, -1),
          Seq(-1, 1, 1), Seq(-1, 1, -1), Seq(-1, -1, 1), Seq(-1, -1, -1)
        ).map{ case Seq(iv, jv, kv) => Rotation(i * iv, j * jv, k * kv) }
      }.filter(_.determinant == 1).toSeq
  }

  // A scanner is oriented (ie. guaranteed to be rotated the same direction as scanner 0)
  // while a report is not.
  case class Scanner(scannerID: Int, offset: Point, beaconsRelative: Seq[Point]) {
    val beacons: Seq[Point] = beaconsRelative.map(_ + offset)
  }

  def bestOffset(s1: Seq[Point], s2: Seq[Point]): Point = {
    val allOffsets = s1.flatMap(p1 => s2.map((p1 - _))).toSet
    allOffsets.maxBy(offset => (s1.toSet & (s2.map(_ + offset).toSet)).size)
  }

  case class Report(scannerID: Int, beacons: Seq[Point]) {
    def alignTo(scanner: Scanner): Option[Scanner] = Rotation.all.flatMap(r => {
      val rotated = beacons.map(r(_))
      val offset = bestOffset(scanner.beacons, rotated)
      if ((rotated.map(_ + offset).toSet & scanner.beacons.toSet).size < 12)
        None else Some(Scanner(scannerID, offset, rotated))
    }).headOption
  }
  object Report {
    val headerRE = raw"--- scanner (\d+) ---".r
    def parse(raw: Seq[String]): Report = Report(
      raw.head match { case headerRE(id) => id.toInt },
      raw.tail.map(_.split(",").map(_.toInt).toSeq).map{ case Seq(x, y, z) => Point(x, y, z) },
    )
  }

  def alignScanners(reports: Seq[Report], scanners: Seq[Scanner] = Seq()): Seq[Scanner] =
    if (reports.isEmpty) scanners else if (scanners.isEmpty) {
      alignScanners(reports.tail, Seq(Scanner(reports.head.scannerID, Point(0, 0, 0), reports.head.beacons)))
    } else {
      val newlyAligned = reports.flatMap(r => scanners.flatMap(r.alignTo(_)).headOption)
      val newlyAlignedIDs = newlyAligned.map(_.scannerID).toSet
      println(s"Aligned: $newlyAlignedIDs")
      alignScanners(
        reports.filter(r => !newlyAlignedIDs.contains(r.scannerID)),
        newlyAligned ++ scanners
      )
    }

  val reports = splitSeq(data, "").map(Report.parse(_))
  val scanners = alignScanners(reports)
  println(scanners.flatMap(_.beacons).toSet.size)
  println(scanners.combinations(2).map{ case Seq(s1, s2) => s1.offset.manhattan(s2.offset) }.max)
}
