object Main extends App {
  //val data = List(4, 6, 9, 2, 1, 7, 5, 3, 8)
  val data = List(3, 8, 9, 1, 2, 5, 4, 6, 7)
  def rotateToStart[A](a: A, l: List[A]): List[A] = {
    val i = l.indexOf(a)
    l.drop(i) ++ l.take(i)
  }
  def mix(l: List[Int], n: Int): List[Int] = {
    var c = l(0)
    var i = n
    var ll = l
    while (i > 0) {
      println(c, ll)
      i -= 1
      val r = rotateToStart(c, ll)
      val rr = c::r.drop(4)
      val taken = r.drop(1).take(3)
      val destination = {
        // Some crappy off-by-one errors here
        val possible = (-4).to(-1).map(v => (v + c + l.size) % (l.size + 1)).reverse
        println(possible)
        possible.filter(!taken.contains(_)).head
      }
      val di = rr.indexOf(destination) + 1
      ll = rr.take(di) ++ taken ++ rr.drop(di)
      c = ll(1)
    }
    ll
  }
  println(rotateToStart(1, mix(data, 10)).drop(1).mkString(""))
  /*
  println(rotateToStart(1, mix(data, 100)).drop(1).mkString(""))
  val longData = data ++ 10.to(1000000).toList
  val mixed = rotateToStart(1, mix(longData, 10000000))
  println(mixed(1).toLong * mixed(2).toLong)
  */
}
