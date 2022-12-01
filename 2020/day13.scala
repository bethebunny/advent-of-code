import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(13).toSeq
  val eta = data(0).toInt
  val busses = data(1).split(",").filter(_ != "x").map(_.toInt).toSeq
  val nextBus = busses.minBy(bus => bus - (eta % bus))
  println(s"ETA: $eta")
  println(s"Busses: $busses")
  println(s"Next bus: $nextBus")
  println(s"Wait time: ${nextBus - (eta % nextBus)}")
  println(nextBus * (nextBus - (eta % nextBus)))

  val bussesWithIndex = data(1).split(",").zipWithIndex.filter(_._1 != "x").map{case (x, y) => (x.toLong, y.toLong)}.toSeq
  //val bussesWithIndex = "17,x,13,19".split(",").zipWithIndex.filter(_._1 != "x").map{case (x, y) => (x.toLong, y.toLong)}.toSeq
  val bussesWithWaitTimes = bussesWithIndex.map{ case (busID, idx) => (busID, posMod(busID - idx, busID)) }
  def posMod(x: math.BigInt, y: math.BigInt): Long = (((x % y) + y) % y).toLong
  def numBits(x: math.BigInt): Int = (math.log(x.toDouble) / math.log(2)).toInt
  def bits(x: math.BigInt): Seq[Int] = 0.to(numBits(x)).map(b => ((x >> b) & 1).toInt)
  def filterLike[A](x: Seq[A], p: Seq[Boolean]): Seq[A] = x.zip(p).filter(_._2).map(_._1)
  def modularExp(a: math.BigInt, x: math.BigInt, p: math.BigInt): Long = {
    val powers = 1.to(numBits(x)).scanLeft(a)((i, _) => i * i % p)
    filterLike(powers, bits(x).map(_ > 0)).foldLeft(math.BigInt(1))(_ * _ % p).toLong
  }
  def solveModularSystem(mods: Seq[(Long, Long)]): (Long, Long) = {
    // Assume inputs are primes
    val result = _solveModularSystem(mods.map{ case (p, a) => (p, a, p-1) })
    (result._1, result._2)
  }
  def _solveModularSystem(mods: Seq[(Long, Long, Long)]): (Long, Long, Long) = {
    // Each input is (modulus, value, totient(modulus))
    mods.toList match {
      case x::y::xs => {
        val p = _solveModularSystem(y::xs)
        val p1 = p._1
        val p2 = x._1
        val a = p._2
        val b = x._2
        val totientP1 = p._3
        val totientP2 = x._3
        val k = posMod((b - a) * modularExp(p1, totientP2-1, p2), p2)
        (p1 * p2, a + p1 * k, totientP1 * totientP2)
      }
      case x::Nil => x
      case Nil => (1, 0, 0)
    }
  }
  println(solveModularSystem(bussesWithWaitTimes)._2)
  //val x = solveModularSystem(bussesWithWaitTimes)._2
  //println(bussesWithWaitTimes.map(_._1).map((v: Long) => (v, x % v)))
}
