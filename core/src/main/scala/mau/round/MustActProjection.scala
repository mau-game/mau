package mau.round

import io.circe.Codec

enum MustActProjection derives Codec:
  case Play(player: Player)
  case Click(player: Player)
