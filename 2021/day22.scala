import advent_of_code.data.{dataForDay, sessionID}
import scala.collection.mutable


object Main extends App {
  val data = dataForDay(22).toSeq

  case class Point(x: Int, y: Int, z: Int)
  case class Cuboid(xr: Range, yr: Range, zr: Range) {
    def points = xr.flatMap(x => yr.flatMap(y => zr.map(z => Point(x, y, z)).toSeq))
    def pointsIntersecting(smallRegion: Cuboid) =
      smallRegion.points.filter(p => xr.contains(p.x) && yr.contains(p.y) && zr.contains(p.z))
    def size: Long = xr.size.toLong * yr.size.toLong * zr.size.toLong
    def intersection(other: Cuboid): Cuboid = Cuboid(
      (xr.min max other.xr.min) to (xr.max min other.xr.max),
      (yr.min max other.yr.min) to (yr.max min other.yr.max),
      (zr.min max other.zr.min) to (zr.max min other.zr.max),
    )
    def intersects(other: Cuboid): Boolean = intersection(other).size > 0
    def -(other: Cuboid): Set[Cuboid] = {
      val i = intersection(other)
      if (i.size == 0) return Set(this)
      val left = Cuboid(xr.min until i.xr.min, yr, zr)
      val right = Cuboid(i.xr.max + 1 to xr.max, yr, zr)
      val bottom = Cuboid(i.xr, yr.min until i.yr.min, zr)
      val top = Cuboid(i.xr, i.yr.max + 1 to yr.max, zr)
      val front = Cuboid(i.xr, i.yr, zr.min until i.zr.min)
      val back = Cuboid(i.xr, i.yr, i.zr.max + 1 to zr.max)
      Seq(left, right, bottom, top, front, back).filter(_.size > 0).toSet
    }
    def union(other: Cuboid): Set[Cuboid] = (this - other) + this
  }

  val lineRE = raw"""(on|off) x=(-?\d+)..(-?\d+),y=(-?\d+)..(-?\d+),z=(-?\d+)..(-?\d+)""".r
  val procedure = data.map{
    case lineRE(onoff, xmin, xmax, ymin, ymax, zmin, zmax) =>
      (onoff, Cuboid(xmin.toInt to xmax.toInt, ymin.toInt to ymax.toInt, zmin.toInt to zmax.toInt))
  }

  def executeProcedure(procedure: Seq[(String, Cuboid)], area: Cuboid, on: Set[Point] = Set()): Set[Point] =
    if (procedure.isEmpty) on else procedure.head match {
      case ("on", cuboid) => executeProcedure(procedure.tail, area, on ++ cuboid.pointsIntersecting(area))
      case ("off", cuboid) => executeProcedure(procedure.tail, area, on -- cuboid.pointsIntersecting(area))
    }

  val initializationArea = Cuboid(-50 to 50, -50 to 50, -50 to 50)
  println(executeProcedure(procedure, initializationArea).size)

  def turnOn(on: Cuboid, cover: Set[Cuboid]): Set[Cuboid] = {
    val intersecting = cover.filter(_.intersects(on))
    if (intersecting.isEmpty) cover + on
    else if (cover.contains(on)) cover
    else (on - intersecting.head).foldLeft(cover){ case (cover, segment) => turnOn(segment, cover) }
  }
  def turnOff(off: Cuboid, cover: Set[Cuboid]): Set[Cuboid] =
    cover.flatMap(area => if (!off.intersects(area)) Seq(area) else (area - off))

  println(
    procedure.foldLeft(Set[Cuboid]()){
      case (cover, ("on", area)) => turnOn(area, cover)
      case (cover, ("off", area)) => turnOff(area, cover)
    }.toSeq.map(_.size).sum
  )
}
