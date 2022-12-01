import advent_of_code.data.{rawDataForDay, sessionID}


object Main extends App {
  def fields(rawPassport: String): Map[String, String] = {
    val fieldRE = """(\w+):([^\s]+)""".r
    fieldRE.findAllMatchIn(rawPassport)
      .map(_.subgroups)
      .map{ case x::y::Nil => (x, y) }
      .toMap
  }
  val data = rawDataForDay(4)
  val passports = data.split("\n\n").map(fields)
  def passportHasRequiredFields(passport: Map[String, String]): Boolean = {
    val requiredFields = Seq("byr", "iyr", "eyr", "hgt", "hcl", "ecl", "pid")
    requiredFields.forall(passport.contains(_))
  }
  println(passports.count(passportHasRequiredFields _))

  def yearValidator(from: Int, to: Int)(yearField: String) =
      """\d{4}""".r.matches(yearField) & (from.to(to).contains(yearField.toInt))

  val fieldValidators: Map[String, (String => Boolean)] = Map(
    "byr" -> yearValidator(1920, 2002),
    "iyr" -> yearValidator(2010, 2020),
    "eyr" -> yearValidator(2020, 2030),
    "hgt" -> ((hgt: String) => {
      val cmR = raw"(\d{3})cm".r
      val inR = raw"(\d{2})in".r
      hgt match {
        case cmR(cm) => 150.to(193).contains(cm.toInt)
        case inR(inches) => 59.to(76).contains(inches.toInt)
        case _ => false
      }
    }),
    "hcl" -> ((hcl: String) => raw"#[\da-f]{6}".r.matches(hcl)),
    "ecl" -> ((ecl: String) =>
        Seq("amb", "blu", "brn", "gry", "grn", "hzl", "oth").contains(ecl)),
    "pid" -> ((pid: String) => raw"\d{9}".r.matches(pid)),
    "cid" -> ((cid: String) => true),
  )
  def passportValid(passport: Map[String, String]): Boolean =
    passportHasRequiredFields(passport) & passport.forall{
      case (field, value) => fieldValidators(field)(value)
    }
  println(passports.count(passportValid _))
}
