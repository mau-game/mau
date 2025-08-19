package mau.util

import scala.scalajs.js

final case class Animation(keyframes: Seq[Keyframe], durationMs: Int):
  def jsKeyframes: js.Array[js.Dynamic] = js.Array(keyframes.map(_.toJS)*)
    js.Array(keyframes.map(_.toJS)*)
  
  def jsOptions: js.Any = js.Dynamic.literal(
    "duration" -> durationMs,
    // "easing" -> "ease-in-out",
  )

  def withDurationMs(durationMs: Int): Animation = copy(durationMs = durationMs)

  def delayed(delayMs: Int): Animation =
    val offset = delayMs.toFloat / (delayMs + durationMs)
    val head = keyframes.head
    Animation(
      Seq(head, head.copy(head.properties :+ Keyframe.Property.Offset(offset))) ++ keyframes.tail,
      durationMs = durationMs + delayMs
    )

object Animation:
  def apply(keyframes: Keyframe*): Animation = Animation(keyframes, durationMs = 300)
