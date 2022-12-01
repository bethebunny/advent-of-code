import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(17).toSeq
  /* part1, 3d conway
  val initActive = data.zipWithIndex.flatMap{
    case (row, i) => row.zipWithIndex.filter{case (c, _) => (c == '#')}.map{case (_, j) => (i, j, 0)}
  }.toSet
  val neighborDeltas =
    -1.to(1).flatMap(x => -1.to(1).flatMap(y => -1.to(1).map(z => (x, y, z)))).filter(_ != (0, 0, 0))
  def neighbors(p: (Int, Int, Int)): Seq[(Int, Int, Int)] =
    neighborDeltas.map{case (x, y, z) => (p._1 + x, p._2 + y, p._3 + z)}
  def iterateConwayCube(active: Set[(Int, Int, Int)]): Set[(Int, Int, Int)] = {
    val inactive = active.flatMap(neighbors).filter(!active.contains(_))
    (
      active.filter(p => 2.to(3).contains(neighbors(p).count{active.contains(_)}))
      | inactive.filter(p => neighbors(p).count(active.contains(_)) == 3)
    )
  }
  def iterateConwayCube(iterations: Int, active: Set[(Int, Int, Int)]): Set[(Int, Int, Int)] =
    iterations match {
      case 0 => active
      case _ => iterateConwayCube(iterations - 1, iterateConwayCube(active))
    }
  */

 // part 2, 4d conway
 /*
  val initActive = data.zipWithIndex.flatMap{
    case (row, i) => row.zipWithIndex.filter{case (c, _) => (c == '#')}.map{case (_, j) => (i, j, 0, 0)}
  }.toSet
  val neighborDeltas =
    -1.to(1).flatMap(x => -1.to(1).flatMap(y => -1.to(1).flatMap(z => -1.to(1).map(s => (x, y, z, s)))).filter(_ != (0, 0, 0, 0)))
  def neighbors(p: (Int, Int, Int, Int)): Seq[(Int, Int, Int, Int)] =
    neighborDeltas.map{case (x, y, z, s) => (p._1 + x, p._2 + y, p._3 + z, p._4 + s)}
  def iterateConwayCube(active: Set[(Int, Int, Int, Int)]): Set[(Int, Int, Int, Int)] = {
    val inactive = active.flatMap(neighbors).filter(!active.contains(_))
    (
      active.filter(p => 2.to(3).contains(neighbors(p).count{active.contains(_)}))
      | inactive.filter(p => neighbors(p).count(active.contains(_)) == 3)
    )
  }
  def iterateConwayCube(iterations: Int, active: Set[(Int, Int, Int, Int)]): Set[(Int, Int, Int, Int)] =
    iterations match {
      case 0 => active
      case _ => iterateConwayCube(iterations - 1, iterateConwayCube(active))
    }
  println(iterateConwayCube(6, initActive).size)
  */

  def initActive(implicit nDims: Int) = data.zipWithIndex.flatMap{
    case (row, i) => row.zipWithIndex
      .filter{case (c, _) => (c == '#')}
      .map{case (_, j) => Seq(i, j) ++ Array.fill(nDims-2)(0)}
  }.toSet

  def cartesianPower[A](v: Seq[A], n: Int): Seq[Seq[A]] =
    0.until(n).map(_ => v).foldLeft(Seq(Seq[A]()))((s, a) => s.flatMap(x => a.map(x:+_)))

  def neighborDeltas(nDims: Int): Seq[Seq[Int]] =
    cartesianPower(-1.to(1), nDims).filter(!_.forall(_ == 0))

  def neighbors(p: Seq[Int])(implicit nDims: Int): Seq[Seq[Int]] =
    neighborDeltas(nDims).map(_.zip(p).map{case (a, b) => a + b})

  def iterateConwayCube(active: Set[Seq[Int]])(implicit nDims: Int): Set[Seq[Int]] = {
    val inactive = active.flatMap(neighbors).filter(!active.contains(_))
    def numActiveNeighbors(p: Seq[Int]) = neighbors(p).count(active.contains(_))
    active.filter(p => 2.to(3).contains(numActiveNeighbors(p))) | inactive.filter(numActiveNeighbors(_) == 3)
  }

  def applyN[A](n: Int, f: A => A, i: A): A = 0.until(n).foldLeft(i)((i, _) => f(i))

  { implicit val nDims = 3; println(applyN(6, iterateConwayCube, initActive).size) }
  { implicit val nDims = 4; println(applyN(6, iterateConwayCube, initActive).size) }
}
