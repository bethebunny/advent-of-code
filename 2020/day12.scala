import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  case class Point(x: Int, y: Int) {
    def +(other: Point): Point = Point(x + other.x, y + other.y)
    val manhattan = x + y
  }

  def posMod(x: Int, y: Int): Int = ((x % y) + y) % y

  class Ship(var heading: Int = 0, var x: Int = 0, var y: Int = 0) {
    def north(d: Int): Unit = y += d
    def south(d: Int): Unit = y -= d
    def east(d: Int): Unit = x += d
    def west(d: Int): Unit = x -= d
    def left(degrees: Int): Unit = heading += degrees
    def right(degrees: Int): Unit = heading -= degrees
    def forward(d: Int): Unit = {
      posMod(heading, 360) match {
        case 0 => east(d)
        case 90 => north(d)
        case 180 => west(d)
        case 270 => south(d)
        case _ => throw new RuntimeException("Unsupported heading degrees: " + heading)
      }
    }

    def manhattan: Int = x.abs + y.abs
    override def toString: String = s"Heading: $heading, x: $x, y: $y"

    def execute(s: Seq[String]): Unit = {
      val directionRE = raw"(\w)(\d+)".r
      s.map{directive => {
        //println("Before " + directive + ": " + this)
        directive match {
          case directionRE(i, d) => i match {
            case "N" => north(d.toInt)
            case "S" => south(d.toInt)
            case "E" => east(d.toInt)
            case "W" => west(d.toInt)
            case "L" => left(d.toInt)
            case "R" => right(d.toInt)
            case "F" => forward(d.toInt)
          }
        }
      }}
    }
  }

  // https://www.oreilly.com/library/view/scala-cookbook/9781449340292/ch04s11.html
  // suggests that naming these x, y should be fine, however in practice then this.x,
  // this.y map to the original constructor args and not the set ones.
  class Waypoint(u: Int = 0, v: Int = 0) extends Ship(0, u, v) {
    override def left(degrees: Int): Unit = {
      val x_ = x
      val y_ = y
      //println(s"Rotating $degrees left from $x_($x), $y_($y)")
      posMod(degrees, 360) match {
        case 0 => {}
        case 90 => {
          this.x = -y_
          this.y = x_
        }
        case 180 => {
          this.x = -x_
          this.y = -y_
        }
        case 270 => {
          this.x = y_
          this.y = -x_
        }
        case _ => throw new RuntimeException("Unsupported heading degrees: " + heading)
      }
    }
    override def right(degrees: Int): Unit = left(-degrees)
  }

  class WaypointGuidedShip extends Ship {
    val waypoint = new Waypoint(10, 1)
    override def north(d: Int): Unit = waypoint.north(d)
    override def south(d: Int): Unit = waypoint.south(d)
    override def east(d: Int): Unit = waypoint.east(d)
    override def west(d: Int): Unit = waypoint.west(d)
    override def left(degrees: Int): Unit = waypoint.left(degrees)
    override def right(degrees: Int): Unit = waypoint.right(degrees)
    override def forward(d: Int): Unit = {
      this.x += d * waypoint.x
      this.y += d * waypoint.y
    }
    override def toString: String = s"Ship.x: $x, .y: $y ; Waypoint.x: ${waypoint.x}, .y: ${waypoint.y}"
  }

  val data = dataForDay(12).toSeq
  //val data = Seq("F10", "N3", "F7", "R90", "F11")
  val ship = new Ship()
  ship.execute(data)
  println(ship)
  println(ship.manhattan)
  val waypointGuidedShip = new WaypointGuidedShip
  waypointGuidedShip.execute(data)
  println(waypointGuidedShip)
  println(waypointGuidedShip.manhattan)
}
