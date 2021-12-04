import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(3).toSeq
  def mode[T](s: Seq[T]): T = s.groupBy(identity).maxBy(_._2.size)._1
  val mostCommonBits = data.transpose.map(mode)
  val gamma = Integer.parseInt(mostCommonBits.mkString, 2)
  def invertBit(bit: Char): Char = bit match {
    case '1' => '0'
    case '0' => '1'
  }
  val epsilon = Integer.parseInt(mostCommonBits.map(invertBit).mkString, 2)
  println(gamma * epsilon)

  def filterDiagnostics(
    s: Seq[String],
    criterion: Seq[Char] => Char,
    bit: Int = 0
  ): String = s match {
    case Seq(result) => result
    case _ => {
      val filterBit = criterion(s.map(_(bit)))
      filterDiagnostics(s.filter(_(bit) == filterBit), criterion, bit+1)
    }
  }

  // Very specialized to this problem
  def modeOrOne(s: Seq[Char]): Char = {
    val counts = s.groupBy(identity).map{ case (k, v) => (k, v.size) }
    val ones = counts.get('1').getOrElse(0)
    val zeros = counts.get('0').getOrElse(0)
    if (ones >= zeros) '1' else '0'
  }

  val oxygenGenerator = Integer.parseInt(filterDiagnostics(data, bits => modeOrOne(bits)), 2)
  val co2Scrubber = Integer.parseInt(filterDiagnostics(data, bits => invertBit(modeOrOne(bits))), 2)
  println(oxygenGenerator * co2Scrubber)
}

