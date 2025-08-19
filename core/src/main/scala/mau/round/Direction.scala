package mau.round

import io.circe.Codec

enum Direction derives Codec:
  case ClockWise, CounterClockWise

  def reverse: Direction = this match
    case ClockWise        => CounterClockWise
    case CounterClockWise => ClockWise
