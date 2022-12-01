import advent_of_code.data.{dataForDay, sessionID}
import scala.collection.mutable


object Main extends App {
  def numBits(x: math.BigInt): Int = (math.log(x.toDouble) / math.log(2)).toInt
  def bits(x: math.BigInt, nbits: Int): Seq[Int] = {
    val n = if (nbits <= 0) numBits(x) else nbits
    0.to(n).map(b => ((x >> b) & 1).toInt)
  }
  def fromBits(s: Seq[Int]): Long = s.zipWithIndex.map{
    case (bit, place) => bit.toLong * math.pow(2, place).toLong
  }.sum

  class ShipsComputer {
    var mask = ""
    var memory: mutable.Map[String, Long] = mutable.Map()

    def setMemory(address: String, value: Long): Unit = memory += (address -> applyMask(value))
    def applyMask(value: Long): Long = {
      fromBits(
        bits(value, mask.size).zip(mask.reverse).map{
          case (bit, 'X') => bit
          case (bit, '0') => 0
          case (bit, '1') => 1
        }
      )
    }

    def execute(instruction: String): Unit = {
      val maskRE = "mask = (.*)".r
      val memRE = raw"(mem.*) = (\d+)".r
      instruction match {
        case maskRE(mask) => this.mask = mask
        case memRE(address, value) => setMemory(address, value.toLong)
      }
      /*
      println()
      println(s"Executed $instruction")
      println(s"Mask: $mask")
      println(s"Memory: $memory")
      */
    }
  }

  class ShipsComputerMk2 extends ShipsComputer {
    override def setMemory(address: String, value: Long): Unit = {
      maskedAddresses(address).foreach(address => memory += (address -> value))
    }

    def maskedAddresses(address: String): Seq[String] = {
      val addrRE = raw"mem\[(\d+)\]".r 
      val addr = address match { case addrRE(addr) => addr }
      def recurse(bits: List[Char], addr: List[Char]): Seq[List[Int]] = bits match {
        case Nil => Seq(Nil)
        case _ => recurse(bits.tail, addr.tail).flatMap(xs => bits.head match {
          case 'X' => Seq(0::xs, 1::xs)
          case '1' => Seq(1::xs)
          case '0' => Seq(addr.head.toString.toInt::xs)
        })
      }
      val addresses = recurse(mask.toList.reverse, bits(addr.toLong, mask.size).toList.map(_.toString.apply(0)))
      addresses.map(fromBits).map(_.toString)
    }
  }

  val data = dataForDay(14).toSeq
  /*
  val data = Seq(
    "mask = XXXXXXXXXXXXXXXXXXXXXXXXXXXXX1XXXX0X",
    "mem[8] = 11",
    "mem[7] = 101",
    "mem[8] = 0",
  )
  val data = Seq(
    "mask = 000000000000000000000000000000X1001X",
    "mem[42] = 100",
    "mask = 00000000000000000000000000000000X0XX",
    "mem[26] = 1",
  )
  */
  /*
  val computer = new ShipsComputer
  data.foreach(computer.execute)
  println(computer.memory.values.sum)
  */
  val computer2 = new ShipsComputerMk2
  data.foreach(computer2.execute)
  println(computer2.memory.values.sum)
}
