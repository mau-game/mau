package mau.round

import io.circe.Codec

case class User(name: String, avatar: String) derives Codec
