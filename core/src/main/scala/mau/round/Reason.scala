package mau.round

import io.circe.Codec

enum Reason derives Codec:
  case InvalidCard
  case InvalidButton
  case NotClickButton(button: Button)
  case OutOfTurn
  case TooLongToPlay
