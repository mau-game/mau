package mau

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement

private def commandButton(modifiers: Modifier[Button]*): Div = div(
  className := "command-button",
  button(modifiers*)
)

extension [T] (stream: EventStream[T])
  def withTiming(timeMs: Int): EventStream[Option[T]] =
    stream.map(Some(_)).mergeWith(stream.debounce(timeMs).mapTo(None))
