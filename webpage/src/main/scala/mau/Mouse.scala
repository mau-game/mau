package mau

import com.raquo.laminar.api.L._
import org.scalajs.dom.MouseEvent

object Mouse:
  case class Position(x: Double, y: Double)

  val $down: EventStream[MouseEvent] = documentEvents(_.onMouseDown)
  val $up: EventStream[MouseEvent] = documentEvents(_.onMouseUp)
  val $move: EventStream[MouseEvent] = documentEvents(_.onMouseMove)

  val $position = $move
    .map(e => Position(e.pageX, e.pageY))
    .toSignal(Position(0, 0))
