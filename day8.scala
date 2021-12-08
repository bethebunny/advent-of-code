import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(8).toSeq
  val allReadouts = data.flatMap(_.split(raw"\|")(1).trim.split(" "))
  println(allReadouts.count(readout => Set(2, 3, 4, 7).contains(readout.size)))

  val lineRE = raw"(.*) \| (.*)".r
  def readDigits(line: String): Int = line match {
    case lineRE(digitsStr, readoutStr) => {
      val digits = digitsStr.split(" ")
      val readouts = readoutStr.split(" ")
      val one = digits.filter(_.size == 2).head
      val seven = digits.filter(_.size == 3).head
      val four = digits.filter(_.size == 4).head
      val eight = digits.filter(_.size == 7).head
      val three = digits.filter(
        digit => digit.size == 5 && one.forall(digit.contains(_))).head
      val two = digits.filter(
        digit => digit.size == 5 && digit != three && (digit.toSet -- four.toSet).size == 3).head
      val five = digits.filter(
        digit => digit.size == 5 && digit != three && (digit.toSet -- four.toSet).size == 2).head

      val c = (one.toSet -- five.toSet).head
      val a = (seven.toSet -- one.toSet).head
      val adg = (two.toSet & five.toSet)
      val bd = four.toSet -- one.toSet
      val b = (bd -- adg).head
      val d = (bd - b).head

      val zero = digits.filter(_.toSet == (eight.toSet - d)).head
      val six = digits.filter(_.toSet == (eight.toSet - c)).head
      val nine = digits.filter(
        digit => digit.size == 6 && digit != zero && digit != six).head

      readouts.map(digit => Map(
        zero.toSet -> 0,
        one.toSet -> 1,
        two.toSet -> 2,
        three.toSet -> 3,
        four.toSet -> 4,
        five.toSet -> 5,
        six.toSet -> 6,
        seven.toSet -> 7,
        eight.toSet -> 8,
        nine.toSet -> 9,
      )(digit.toSet)).map(_.toString).mkString.toInt
    }
  }

  println(data.map(readDigits(_)).sum)
}

