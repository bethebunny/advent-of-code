import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val Seq(cardPK, doorPK) = dataForDay(25).toSeq.map(_.toLong)

  case class Modulus(i: Long) extends AnyVal
  case class Subject(i: Long) extends AnyVal
  implicit def modulus2Long(m: Modulus): Long = m.i
  implicit def subject2Long(s: Subject): Long = s.i

  def discreteLog(
    target: Long, v: Long = 1, i: Long = 0,
  )(implicit modulus: Modulus, subject: Subject): Long =
    if (v == target) i else discreteLog(target, v * subject % modulus, i + 1)

  def discreteExp(
    v: Long, exp: Long,
  )(implicit modulus: Modulus, subject: Subject): Long =
    if (exp == 0) v else discreteExp(v * subject % modulus, exp - 1)

  {
    implicit val modulus: Modulus = Modulus(20201227)
    implicit val subject: Subject = Subject(7)
    val cardSK = discreteLog(cardPK)
    val tcSK = discreteExp(1, cardSK) //0L.until(cardSK).foldLeft(1L)((s, _) => s * subject % modulus)
    println(s"Card PK: $cardPK")
    println(s"Card SK: $cardSK")
    println(s"s ^ cardSK % p: $tcSK")

    {
      implicit val subject: Subject = Subject(doorPK);
      println(discreteExp(1, cardSK))
    }
  }
}
