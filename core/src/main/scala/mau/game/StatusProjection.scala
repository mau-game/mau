package mau.game

import io.circe.Codec

enum StatusProjection derives Codec:
  case Deliberating, InLobby, Playing
