import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(16).toSeq

  def extractLiteral(bits: String): (String, Int) = bits(0) match {
    case '0' => (bits.substring(1, 5), 5)
    case '1' => {
      val (restBits, restLength) = extractLiteral(bits.substring(5))
      (bits.substring(1, 5) ++ restBits, 5 + restLength)
    }
  }

  sealed trait Packet {
    def versionSum: Int
    def value: Long
  }
  case class LiteralPacket(version: Int, value: Long) extends Packet {
    def versionSum: Int = version
  }
  case class OperatorPacket(version: Int, typeID: Int, packets: Seq[Packet]) extends Packet {
    def versionSum = version + packets.map(_.versionSum).sum
    def value: Long = typeID match {
      case 0 => packets.map(_.value).sum
      case 1 => packets.map(_.value).product
      case 2 => packets.map(_.value).min
      case 3 => packets.map(_.value).max
      case 5 => if (packets(0).value > packets(1).value) 1 else 0
      case 6 => if (packets(0).value < packets(1).value) 1 else 0
      case 7 => if (packets(0).value == packets(1).value) 1 else 0
    }
  }

  def parseNPackets(bits: String, n: Int): (Seq[Packet], Int) =
    if (n == 0) (Seq(), 0) else {
      val (packet, length) = parsePacket(bits)
      val (packets, restLength) = parseNPackets(bits.substring(length), n - 1)
      (packet +: packets, length + restLength)
    }

  def parsePacketsTilNBits(bits: String, n: Int): Seq[Packet] =
    if (bits.substring(0, n).forall(_ == '0')) Seq() else {
      val (packet, length) = parsePacket(bits)
      packet +: parsePacketsTilNBits(bits.substring(length), n - length)
    }

  def roundToPacketLength(n: Int): Int = n + (4 - (n % 4))

  def parsePacket(bits: String): (Packet, Int) = {
    val version = Integer.parseInt(bits.substring(0, 3), 2)
    val typeID = Integer.parseInt(bits.substring(3, 6), 2)
    typeID match {
      case 4 => {
        val (extractedLiteral, length) = extractLiteral(bits.substring(6))
        (LiteralPacket(version, BigInt(extractedLiteral, 2).toLong), length + 6)
      }
      case _ if bits(6) == '0' => {
        val packetsBitLength = Integer.parseInt(bits.substring(7, 22), 2)
        val packets = parsePacketsTilNBits(bits.substring(22), packetsBitLength)
        (OperatorPacket(version, typeID, packets), 22 + packetsBitLength)
      }
      case _ if bits(6) == '1' => {
        val nPackets = Integer.parseInt(bits.substring(7, 18), 2)
        val (packets, length) = parseNPackets(bits.substring(18), nPackets)
        (OperatorPacket(version, typeID, packets), 18 + length)
      }
    }
  }

  def leadingZeros(hex: Char): String =
    "0" * (4 - Integer.toString(Integer.parseInt(hex.toString, 16), 2).size)

  // This lacks some leading 0s by default
  val bits = leadingZeros(data(0)(0)) ++ BigInt(data(0), 16).toString(2)
  val (packet, _) = parsePacket(bits)

  println(packet.versionSum)
  println(packet.value)
}
