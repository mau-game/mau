package mau.game

import io.circe.Codec

final case class LoginParams(avatar: String) derives Codec
