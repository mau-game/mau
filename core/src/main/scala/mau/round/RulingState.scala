package mau.round

import io.circe.Codec
import mau.utils.given_Codec_Map

final case class RulingState(
    discard: Card,
    yourHandSize: Int,
    expectedPlayerHandSize: Int,
    isExpectedPlayer: Boolean,
    direction: Direction
) derives Codec
