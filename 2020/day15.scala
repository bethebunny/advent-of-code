import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  def speak(n: Int, lastSpoken: Int, turnLastSpoken: Map[Int, Int] = Map()): Int = {
    val spoken = turnLastSpoken.getOrElse(lastSpoken, n) - n
    n match {
      case 1 => lastSpoken
      case _ => speak(n-1, spoken, turnLastSpoken ++ Map((lastSpoken -> n)))
    }
  }
  def speak(n: Int, starting: Seq[Int]): Int =
    speak(n-starting.size+1, starting.last, starting.init.zipWithIndex.map{case (v, i) => (v, n - i)}.toMap)

  println(speak(2020, Seq(0, 3, 6)))
  println(speak(2020, Seq(0, 14, 1, 3, 7, 9)))
  println(speak(30000000, Seq(0, 14, 1, 3, 7, 9)))
}
