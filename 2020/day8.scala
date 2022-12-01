import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  abstract class Instruction {
    def exec(gb: GameBoy, i: Int): Int
  }

  case class NOP(arg: Int) extends Instruction {
    def exec(gb: GameBoy, i: Int): Int = i + 1
  }
  case class ACC(arg: Int) extends Instruction {
    def exec(gb: GameBoy, i: Int): Int = {
      gb.accumulator += arg
      i + 1
    }
  }
  case class JMP(arg: Int) extends Instruction {
    def exec(gb: GameBoy, i: Int): Int = i + arg
  }

  object Instruction {
    def parse(s: String): Instruction = {
      val instructionRE = raw"(\w{3}) ([+-]?\d+)".r
      s match {
        case instructionRE(ins, arg) => ins match {
          case "nop" => NOP(arg.toInt)
          case "acc" => ACC(arg.toInt)
          case "jmp" => JMP(arg.toInt)
          case _ => throw new RuntimeException("Unsupported instruction: " ++ s ++ ", " ++ ins)
        }
      }
    }
  }


  class GameBoy(var accumulator: Int = 0) {
    def run(program: Seq[Instruction]): Unit = {
      var i = 0
      var seen: Set[Int] = Set()
      while (!seen.contains(i) & 0 <= i & i < program.size) {
        seen = seen + i
        i = program(i).exec(this, i)
      }
      if (seen.contains(i) | i > program.size | i < 0) {
        throw new RuntimeException("Program looped or jumped outside program range")
      }
    }
  }

  def readProgram(data: Seq[String]): Seq[Instruction] = data.map(Instruction.parse _)

  val data = dataForDay(8).toSeq
  var gb = new GameBoy
  try {
    gb.run(readProgram(data))
  } catch {
    case e: RuntimeException => println(gb.accumulator)
  }

  val program = readProgram(data)
  program.zipWithIndex.filter{
    case (ins: ACC, i) => false
    case _ => true
  }.map{
    case (ins: NOP, i) => (program.slice(0, i) :+ JMP(ins.arg)) ++ program.slice(i + 1, program.size)
    case (ins: JMP, i) => (program.slice(0, i) :+ NOP(ins.arg)) ++ program.slice(i + 1, program.size)
  }.foreach(program => {
    try {
      var gb = new GameBoy
      gb.run(program)
      println(gb.accumulator)
    } catch {
      case e: RuntimeException => {}
    }
  })
}
