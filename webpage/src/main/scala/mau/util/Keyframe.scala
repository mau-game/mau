package mau.util

import scala.annotation.targetName
import scala.scalajs.js

case class Keyframe(properties: Seq[Keyframe.Property]):
  def toJS: js.Dynamic =
    js.Dynamic.literal(properties.map(_.toJS)*)

object Keyframe:
  export Property.*, Transformation.*
  def empty: Keyframe = Keyframe()

  @targetName("applySeq")
  def apply(properties: Property*): Keyframe = Keyframe(properties)

  enum Property:
    case Transform(transformations: Seq[Transformation])
    case ZIndex(value: Int)
    case Offset(value: Float)
    case Easing(value: String)

    def toJS: (String, js.Any) = this match
      case Transform(transformations) => "transform" ->  transformations.mkString(" ")
      case ZIndex(value) => "zIndex" -> value
      case Offset(value) => "offset" -> value
      case Easing(value) => "easing" -> value
  end Property

  enum Transformation:
    case Translate(x: Int, y: Int)
    case Scale(value: Double)
    case Rotate(angle: Int)
    case RotateY(angle: Int)

    override def toString: String = this match
      case Translate(x, y) => s"translate(${x}px, ${y}px)"
      case Scale(value) => s"scale($value)"
      case Rotate(angle) => s"rotate(${angle}deg)"
      case RotateY(angle) => s"rotateY(${angle}deg)"
  end Transformation
end Keyframe
